package com.phoenix.client.util

import android.content.Context
import com.phoenix.client.domain.model.ClientConfig
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URI

/**
 * Writes a TOML config file compatible with the Phoenix Go client binary.
 * Field names MUST match the Go struct tags in pkg/config/client_config.go:
 *   remote_addr, private_key, server_public_key, [[inbounds]], protocol, local_addr, enable_udp
 */
object ConfigWriter {

    private const val CONFIG_FILE = "client.toml"

    data class Result(val file: File, val tomlContent: String, val resolveLog: String)

    /**
     * Parses and resolves a user-supplied server address.
     *
     * Returns a [ResolveResult] with:
     * - [ResolveResult.originalAddr] — "domain:port" (used for remote_addr, HTTP Host, TLS SNI)
     * - [ResolveResult.dialAddr] — "ip:port" if the domain was resolved to a different IP,
     *   null if the address was already an IP (no separate dial override needed)
     * - [ResolveResult.logLine] — human-readable resolution log for the UI
     *
     * Why two fields: The Go binary (CGO_ENABLED=0) uses a pure-Go DNS resolver that
     * cannot find nameservers on Android (/etc/resolv.conf is absent). Pre-resolving
     * via Android's Java InetAddress is needed for DNS to work, but the resolved IP must
     * NOT replace the domain in remote_addr — Cloudflare and other CDNs require the
     * domain in the HTTP Host header and TLS SNI to route the connection correctly.
     */
    private data class ResolveResult(
        val originalAddr: String,
        val dialAddr: String?,
        val logLine: String,
    )

    private fun resolveAddr(addr: String): ResolveResult {
        val trimmed = addr.trim()

        // Prepend a dummy scheme so java.net.URI can parse bare "host:port" strings.
        val prefixed = if (trimmed.contains("://")) trimmed else "x://$trimmed"

        val uri = try { URI(prefixed) } catch (e: Exception) {
            return ResolveResult(trimmed, null, "DNS: URI parse failed for '$trimmed': $e")
        }

        // URI.host strips brackets from IPv6 literals like [::1] → "::1"
        val host = uri.host?.takeIf { it.isNotBlank() }
            ?: return ResolveResult(trimmed, null, "DNS: could not extract host from '$trimmed'")

        // Determine port: explicit > inferred from scheme > 443 (Phoenix default)
        val port: Int = when {
            uri.port > 0 -> uri.port
            trimmed.startsWith("https://") -> 443
            trimmed.startsWith("http://") -> 80
            else -> 443  // bare host with no port → default to 443
        }

        val isIpv6 = host.contains(':')
        val originalAddr = formatAddr(host, port, isIpv6)

        // IP literals need no resolution — no dial_addr needed
        val isIpv4 = host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))
        if (isIpv4 || isIpv6) {
            return ResolveResult(originalAddr, null, "DNS: '$trimmed' is already an IP → $originalAddr")
        }

        // Resolve via Android DNS (called on Dispatchers.IO — blocking is fine)
        return try {
            val ip = (InetAddress.getAllByName(host).firstOrNull { it is Inet4Address }
                ?: InetAddress.getByName(host)).hostAddress
                ?: return ResolveResult(
                    originalAddr, null,
                    "DNS: getHostAddress() returned null for '$host'",
                )

            val dialAddr = formatAddr(ip, port, ip.contains(':'))
            ResolveResult(originalAddr, dialAddr, "DNS: resolved '$host' → $ip")
        } catch (e: Exception) {
            ResolveResult(originalAddr, null, "DNS: resolution FAILED for '$host': $e")
        }
    }

    private fun hostPort(host: String, port: Int, fallback: String) =
        if (port > 0) "$host:$port" else fallback

    private fun formatAddr(host: String, port: Int, isIpv6: Boolean) =
        if (isIpv6) "[$host]:$port" else "$host:$port"

    fun write(context: Context, config: ClientConfig): Result {
        val file = File(context.filesDir, CONFIG_FILE)
        val resolved = resolveAddr(config.remoteAddr)

        val toml = buildString {
            // Keep the original domain for Host header and TLS SNI (required by Cloudflare/CDNs).
            appendLine("remote_addr = \"${resolved.originalAddr}\"")
            // Write the pre-resolved IP as dial_addr so the Go binary can bypass Android's
            // missing /etc/resolv.conf without losing the domain for SNI.
            if (resolved.dialAddr != null) {
                appendLine("dial_addr = \"${resolved.dialAddr}\"")
            }

            if (config.privateKeyFile.isNotBlank()) {
                val absPath = File(context.filesDir, config.privateKeyFile).absolutePath
                // Key: "private_key" — matches toml:"private_key" in ClientConfig Go struct
                appendLine("private_key = \"$absPath\"")
            }

            if (config.serverPubKey.isNotBlank()) {
                appendLine("server_public_key = \"${config.serverPubKey}\"")
            }

            if (config.authToken.isNotBlank()) {
                appendLine("auth_token = \"${config.authToken}\"")
            }

            if (config.tlsMode.isNotBlank()) {
                appendLine("tls_mode = \"${config.tlsMode}\"")
            }

            if (config.fingerprint.isNotBlank()) {
                appendLine("fingerprint = \"${config.fingerprint}\"")
            }

            appendLine()
            appendLine("[[inbounds]]")
            appendLine("protocol = \"socks5\"")
            appendLine("local_addr = \"${config.localSocksAddr}\"")
            appendLine("enable_udp = ${config.enableUdp}")
        }

        file.writeText(toml)
        return Result(file, toml, resolved.logLine)
    }
}
