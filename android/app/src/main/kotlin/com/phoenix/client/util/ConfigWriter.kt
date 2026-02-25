package com.phoenix.client.util

import android.content.Context
import com.phoenix.client.domain.model.ClientConfig
import java.io.File
import java.net.InetAddress

/**
 * Writes a TOML config file compatible with the Phoenix Go client binary.
 * Field names MUST match the Go struct tags in pkg/config/client_config.go:
 *   remote_addr, private_key, server_public_key, [[inbounds]], protocol, local_addr, enable_udp
 */
object ConfigWriter {

    private const val CONFIG_FILE = "client.toml"

    data class Result(val file: File, val tomlContent: String)

    /**
     * Resolves the hostname in a "host:port" address to an IP using Android's
     * system resolver. The Go binary (CGO_ENABLED=0) cannot do DNS on Android
     * because there is no /etc/resolv.conf — pre-resolving here fixes domain support.
     * Returns the original address unchanged if it's already an IP or resolution fails.
     */
    private fun resolveAddr(addr: String): String {
        val lastColon = addr.lastIndexOf(':')
        if (lastColon < 0) return addr
        val host = addr.substring(0, lastColon)
        val port = addr.substring(lastColon + 1)
        // Already an IPv4 or IPv6 literal — no lookup needed
        if (host.all { it.isDigit() || it == '.' || it == ':' || it == '[' || it == ']' }) return addr
        return try {
            val ip = InetAddress.getByName(host).hostAddress ?: return addr
            "$ip:$port"
        } catch (_: Exception) {
            addr // fall back to original; Go will also fail, but we tried
        }
    }

    fun write(context: Context, config: ClientConfig): Result {
        val file = File(context.filesDir, CONFIG_FILE)
        val resolvedAddr = resolveAddr(config.remoteAddr)

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
        return Result(file, toml)
    }
}
