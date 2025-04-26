package com.ongxeno.android.starbuttonbox.datasource

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey // Added import
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

// Define DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages persistent storage for application settings.
 * Includes network configuration and display preferences.
 *
 * @param context Application context needed for DataStore.
 */
class SettingDatasource(private val context: Context) {

    companion object {
        // Key for the target IP address preference
        private val TARGET_IP_ADDRESS = stringPreferencesKey("target_ip_address")
        // Key for the target port preference
        private val TARGET_PORT = intPreferencesKey("target_port")
        // New key for the keep screen on setting preference
        private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        // Logging tag
        private const val TAG = "SettingDatasource"
    }

    /**
     * Flow emitting the current network configuration (IP and Port).
     * Emits null if settings are not yet configured.
     * Handles potential read errors and emits empty preferences in case of IOException.
     */
    val networkConfigFlow: Flow<NetworkConfig?> = context.dataStore.data
        .catch { exception ->
            handleReadError(exception, "NetworkConfig")
            emit(emptyPreferences()) // Emit empty on error to recover gracefully
        }
        .map { preferences ->
            // Map the stored preferences to a NetworkConfig object
            NetworkConfig(
                ip = preferences[TARGET_IP_ADDRESS],
                port = preferences[TARGET_PORT]
            )
        }

    /**
     * New flow emitting the current state of the 'Keep Screen On' setting.
     * Defaults to false if the preference hasn't been set yet.
     * Handles potential read errors.
     */
    val keepScreenOnFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            handleReadError(exception, "KeepScreenOn")
            emit(emptyPreferences()) // Emit empty on error
        }
        .map { preferences ->
            // Read the boolean preference, defaulting to false if not found
            preferences[KEEP_SCREEN_ON] ?: false
        }

    /**
     * Saves the network connection settings (IP address and port) to DataStore.
     *
     * @param ip The IP address string to save.
     * @param port The port number to save.
     */
    suspend fun saveSettings(ip: String, port: Int) {
        try {
            context.dataStore.edit { settings ->
                settings[TARGET_IP_ADDRESS] = ip
                settings[TARGET_PORT] = port
                Log.i(TAG, "Network Settings saved - IP: $ip, Port: $port")
            }
        } catch (e: Exception) {
            // Log any errors during the save operation
            Log.e(TAG, "Error saving network settings.", e)
        }
    }

    /**
     * Saves the 'Keep Screen On' preference to DataStore.
     *
     * @param enabled The desired state (true to keep screen on, false otherwise).
     */
    suspend fun saveKeepScreenOn(enabled: Boolean) {
        try {
            context.dataStore.edit { settings ->
                settings[KEEP_SCREEN_ON] = enabled
                Log.i(TAG, "Keep Screen On setting saved: $enabled")
            }
        } catch (e: Exception) {
            // Log any errors during the save operation
            Log.e(TAG, "Error saving Keep Screen On setting.", e)
        }
    }

    /**
     * Helper function to log read errors from DataStore consistently.
     *
     * @param exception The caught exception.
     * @param settingName A descriptive name of the setting being read (for logging).
     */
    private fun handleReadError(exception: Throwable, settingName: String) {
        if (exception is IOException) {
            // Log IOExceptions specifically, as they are expected during file access
            Log.e(TAG, "Error reading $settingName preferences.", exception)
        } else {
            // Rethrow other unexpected exceptions
            Log.e(TAG, "Unexpected error reading $settingName preferences.", exception)
            throw exception
        }
    }
}
