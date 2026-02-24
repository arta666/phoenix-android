package com.phoenix.client.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object UpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/dondiego2020/phoenix-android/releases/latest"
    const val RELEASES_URL =
        "https://github.com/dondiego2020/phoenix-android/releases/latest"

    suspend fun getLatestVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val json = URL(API_URL).readText()
            Regex(""""tag_name"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
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
}
