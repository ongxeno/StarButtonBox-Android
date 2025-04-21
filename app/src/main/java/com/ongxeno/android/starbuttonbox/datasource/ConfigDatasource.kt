package com.ongxeno.android.starbuttonbox.datasource

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Define DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages persistent storage for application configuration like target IP and Port.
 * Uses Jetpack DataStore (Preferences).
 *
 * @param context Application context is needed to initialize DataStore.
 */
class ConfigDatasource(private val context: Context) {

    companion object {
        private val TARGET_IP_ADDRESS = stringPreferencesKey("target_ip_address")
        private val TARGET_PORT = intPreferencesKey("target_port")
        private const val TAG = "ConfigDatasource"
    }

    val networkConfigFlow: Flow<NetworkConfig?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading NetworkConfig preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            NetworkConfig(
                ip = preferences[TARGET_IP_ADDRESS],
                port = preferences[TARGET_PORT]
            )
        }

    /**
     * Saves the provided IP address and port number to persistent storage.
     *
     * @param ip The IP address string to save.
     * @param port The port number to save.
     */
    suspend fun saveSettings(ip: String, port: Int) {
        try {
            context.dataStore.edit { settings ->
                settings[TARGET_IP_ADDRESS] = ip
                settings[TARGET_PORT] = port
                Log.i(TAG, "Settings saved - IP: $ip, Port: $port")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving settings.", e)
            // Optionally re-throw or handle error (e.g., notify user)
        }
    }
}
