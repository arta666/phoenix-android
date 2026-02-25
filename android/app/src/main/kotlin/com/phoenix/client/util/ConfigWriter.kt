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
     * Normalises a user-supplied server address and pre-resolves the hostname to an IP.
     *
     * Accepted formats:
     *   https://example.com:443   http://sub.example.com
     *   example.com:443           192.168.1.1:443         [::1]:443
     *
     * The Go binary (CGO_ENABLED=0) uses a pure-Go DNS resolver that cannot find
     * nameservers on Android (no /etc/resolv.conf). Pre-resolving here via Android's
     * Java InetAddress ensures domain names always work.
     *
     * Always returns "ip:port" (no scheme) because the Go transport prepends its own scheme.
     */
    /** Returns Pair(resolvedAddr, logLine) */
    private fun resolveAddr(addr: String): Pair<String, String> {
        val trimmed = addr.trim()

        // Prepend a dummy scheme so java.net.URI can parse bare "host:port" strings.
        val prefixed = if (trimmed.contains("://")) trimmed else "x://$trimmed"

        val uri = try { URI(prefixed) } catch (e: Exception) {
            return trimmed to "DNS: URI parse failed for '$trimmed': $e"
        }

        // URI.host strips brackets from IPv6 literals like [::1] → "::1"
        val host = uri.host?.takeIf { it.isNotBlank() }
            ?: return trimmed to "DNS: could not extract host from '$trimmed'"

        // Determine port: explicit > inferred from scheme > 443 (Phoenix default)
        val port: Int = when {
            uri.port > 0 -> uri.port
            trimmed.startsWith("https://") -> 443
            trimmed.startsWith("http://") -> 80
            else -> 443  // bare host with no port → default to 443
        }

        // IP literals need no resolution — just normalise to "ip:port"
        val isIpv4 = host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))
        val isIpv6 = host.contains(':')
        if (isIpv4 || isIpv6) {
            val result = if (port > 0) formatAddr(host, port, isIpv6) else trimmed
            return result to "DNS: '$trimmed' is already an IP → $result"
        }

        // Resolve via Android DNS (called on Dispatchers.IO — blocking is fine)
        return try {
            val ip = (InetAddress.getAllByName(host).firstOrNull { it is Inet4Address }
                ?: InetAddress.getByName(host)).hostAddress
                ?: return hostPort(host, port, trimmed) to "DNS: getHostAddress() returned null for '$host'"

            val result = if (port > 0) formatAddr(ip, port, ip.contains(':')) else ip
            result to "DNS: resolved '$host' → $ip → remote_addr=$result"
        } catch (e: Exception) {
            val fallback = hostPort(host, port, trimmed)
            fallback to "DNS: resolution FAILED for '$host': $e → fallback=$fallback"
        }
    }

    private fun hostPort(host: String, port: Int, fallback: String) =
        if (port > 0) "$host:$port" else fallback

    private fun formatAddr(host: String, port: Int, isIpv6: Boolean) =
        if (isIpv6) "[$host]:$port" else "$host:$port"

    fun write(context: Context, config: ClientConfig): Result {
        val file = File(context.filesDir, CONFIG_FILE)
        val (resolvedAddr, resolveLog) = resolveAddr(config.remoteAddr)

        val toml = buildString {
            appendLine("remote_addr = \"$resolvedAddr\"")

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
        return Result(file, toml, resolveLog)
    }
}
