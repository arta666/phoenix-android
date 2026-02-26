package com.phoenix.client.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.phoenix.client.domain.model.ClientConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "phoenix_config")

/**
 * Persists a list of [ClientConfig] profiles plus the ID of the currently selected one.
 *
 * Storage layout (DataStore keys):
 *   configs_json       – JSON array of all config profiles
 *   selected_config_id – UUID string of the active profile
 *
 * Legacy single-config keys (remote_addr, server_pub_key, …) are read once for migration
 * and thereafter ignored once configs_json is written for the first time.
 */
@Singleton
class ConfigDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        // Multi-config storage
        val CONFIGS_JSON = stringPreferencesKey("configs_json")
        val SELECTED_ID  = stringPreferencesKey("selected_config_id")

        // Legacy single-config keys — used only for one-time migration
        val REMOTE_ADDR       = stringPreferencesKey("remote_addr")
        val SERVER_PUB_KEY    = stringPreferencesKey("server_pub_key")
        val PRIVATE_KEY_FILE  = stringPreferencesKey("private_key_file")
        val CLIENT_PUBLIC_KEY = stringPreferencesKey("client_public_key")
        val USE_VPN_MODE      = booleanPreferencesKey("use_vpn_mode")
        val LOCAL_SOCKS_ADDR  = stringPreferencesKey("local_socks_addr")
        val ENABLE_UDP        = booleanPreferencesKey("enable_udp")
        val AUTH_TOKEN        = stringPreferencesKey("auth_token")
        val TLS_MODE          = stringPreferencesKey("tls_mode")
        val FINGERPRINT       = stringPreferencesKey("fingerprint")
    }

    // ── Public flows ───────────────────────────────────────────────────────────

    /** All stored config profiles. */
    val configsFlow: Flow<List<ClientConfig>> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.CONFIGS_JSON]
        when {
            json != null -> parseConfigs(json)
            prefs[Keys.REMOTE_ADDR] != null -> listOf(legacyConfig(prefs)) // migrate old install
            else -> emptyList() // fresh install — start empty
        }
    }

    /** The currently selected config (first in list if no selection stored). */
    val configFlow: Flow<ClientConfig> = context.dataStore.data.map { prefs ->
        val json       = prefs[Keys.CONFIGS_JSON]
        val selectedId = prefs[Keys.SELECTED_ID] ?: ""
        when {
            json != null -> {
                val list = parseConfigs(json)
                list.firstOrNull { it.id == selectedId } ?: list.firstOrNull() ?: ClientConfig()
            }
            prefs[Keys.REMOTE_ADDR] != null -> legacyConfig(prefs)
            else -> ClientConfig() // fresh install, nothing selected
        }
    }

    // ── Mutations ──────────────────────────────────────────────────────────────

    /** Insert a new config or update an existing one (matched by [ClientConfig.id]). */
    suspend fun save(config: ClientConfig) {
        context.dataStore.edit { prefs ->
            val list = currentList(prefs).toMutableList()
            val idx = list.indexOfFirst { it.id == config.id }
            if (idx >= 0) list[idx] = config else list.add(config)
            prefs[Keys.CONFIGS_JSON] = serializeConfigs(list)
        }
    }

    /** Remove the config with [id]. Allows deleting all configs. */
    suspend fun deleteConfig(id: String) {
        context.dataStore.edit { prefs ->
            val list = currentList(prefs).toMutableList()
            list.removeAll { it.id == id }
            prefs[Keys.CONFIGS_JSON] = serializeConfigs(list)
            if ((prefs[Keys.SELECTED_ID] ?: "") == id) {
                val next = list.firstOrNull()
                if (next != null) prefs[Keys.SELECTED_ID] = next.id
                else prefs.remove(Keys.SELECTED_ID)
            }
        }
    }

    /** Make [id] the active config. */
    suspend fun selectConfig(id: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_ID] = id
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Read the current list, migrating legacy data if present, empty on fresh install. */
    private fun currentList(prefs: Preferences): List<ClientConfig> {
        val json = prefs[Keys.CONFIGS_JSON]
        return when {
            json != null -> parseConfigs(json)
            prefs[Keys.REMOTE_ADDR] != null -> listOf(legacyConfig(prefs))
            else -> emptyList()
        }
    }

    /** Build a ClientConfig from the old single-config DataStore keys (one-time migration). */
    private fun legacyConfig(prefs: Preferences) = ClientConfig(
        id            = "default",
        name          = "Default",
        remoteAddr    = prefs[Keys.REMOTE_ADDR] ?: "",
        serverPubKey  = prefs[Keys.SERVER_PUB_KEY] ?: "",
        privateKeyFile= prefs[Keys.PRIVATE_KEY_FILE] ?: "",
        clientPublicKey = prefs[Keys.CLIENT_PUBLIC_KEY] ?: "",
        useVpnMode    = prefs[Keys.USE_VPN_MODE] ?: false,
        localSocksAddr= prefs[Keys.LOCAL_SOCKS_ADDR] ?: "0.0.0.0:1080",
        enableUdp     = prefs[Keys.ENABLE_UDP] ?: false,
        authToken     = prefs[Keys.AUTH_TOKEN] ?: "",
        tlsMode       = prefs[Keys.TLS_MODE] ?: "",
        fingerprint   = prefs[Keys.FINGERPRINT] ?: "",
    )

    private fun parseConfigs(json: String): List<ClientConfig> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i -> arr.getJSONObject(i).toClientConfig() }
    }.getOrElse { emptyList() }

    private fun serializeConfigs(list: List<ClientConfig>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJsonObject()) }
        return arr.toString()
    }

    private fun ClientConfig.toJsonObject(): JSONObject = JSONObject().apply {
        put("id",              id)
        put("name",            name)
        put("remoteAddr",      remoteAddr)
        put("serverPubKey",    serverPubKey)
        put("privateKeyFile",  privateKeyFile)
        put("clientPublicKey", clientPublicKey)
        put("useVpnMode",      useVpnMode)
        put("localSocksAddr",  localSocksAddr)
        put("enableUdp",       enableUdp)
        put("authToken",       authToken)
        put("tlsMode",         tlsMode)
        put("fingerprint",     fingerprint)
    }

    private fun JSONObject.toClientConfig() = ClientConfig(
        id             = optString("id").ifBlank { UUID.randomUUID().toString() },
        name           = optString("name").ifBlank { "Config" },
        remoteAddr     = optString("remoteAddr"),
        serverPubKey   = optString("serverPubKey"),
        privateKeyFile = optString("privateKeyFile"),
        clientPublicKey= optString("clientPublicKey"),
        useVpnMode     = optBoolean("useVpnMode", false),
        localSocksAddr = optString("localSocksAddr").ifBlank { "0.0.0.0:1080" },
        enableUdp      = optBoolean("enableUdp", false),
        authToken      = optString("authToken"),
        tlsMode        = optString("tlsMode"),
        fingerprint    = optString("fingerprint"),
    )
}
