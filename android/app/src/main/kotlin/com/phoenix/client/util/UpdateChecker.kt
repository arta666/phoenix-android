package com.phoenix.client.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val downloadUrl: String)

object UpdateChecker {

    const val RELEASES_URL = "https://github.com/dondiego2020/phoenix-android/releases/latest"
    const val TELEGRAM_URL  = "https://t.me/FoxFig"

    private const val REPO = "dondiego2020/phoenix-android"

    private fun apkUrl(version: String) =
        "https://github.com/$REPO/releases/download/$version/phoenix-android-$version.apk"

    // Fallback chain — tried in order until one succeeds
    private val CHECK_CHAIN: List<() -> String?> = listOf(
        // 1. GitHub API — also provides browser_download_url but we derive it from tag
        {
            val json = fetch("https://api.github.com/repos/$REPO/releases/latest")
            Regex(""""tag_name"\s*:\s*"([^"]+)"""").find(json ?: "")?.groupValues?.get(1)
        },
        // 2. jsDelivr CDN (mirrors GitHub metadata, different domain — harder to block)
        {
            val json = fetch("https://data.jsdelivr.com/v1/packages/gh/$REPO")
            Regex(""""version"\s*:\s*"([^"]+)"""").find(json ?: "")?.groupValues?.get(1)
        },
        // 3. Follow GitHub redirect — extract version from Location header (no body needed)
        {
            val conn = URL(RELEASES_URL).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 6_000
            conn.readTimeout = 6_000
            conn.connect()
            val location = conn.getHeaderField("Location")
            conn.disconnect()
            Regex("""/tag/([^/\s]+)$""").find(location ?: "")?.groupValues?.get(1)
        },
    )

    suspend fun getLatestUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        for (check in CHECK_CHAIN) {
            try {
                val version = check()
                if (!version.isNullOrBlank()) {
                    return@withContext UpdateInfo(version, apkUrl(version))
                }
            } catch (_: Exception) { }
        }
        null // all sources failed — silently give up
    }

    fun isNewer(latest: String, current: String): Boolean {
        val l = latest.trimStart('v').split(".").mapNotNull { it.toIntOrNull() }
        val c = current.trimStart('v').split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }

    private fun fetch(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 6_000
        conn.readTimeout = 6_000
        return try {
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().readText()
            else null
        } finally {
            conn.disconnect()
        }
    }
}
