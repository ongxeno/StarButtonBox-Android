package com.ongxeno.android.starbuttonbox.datasource.room

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.ongxeno.android.starbuttonbox.data.InputAction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject // Import Inject

/**
 * Type converters to allow Room to reference complex data types.
 * We will store the InputAction object as a JSON string in the database.
 *
 * @property json The kotlinx.serialization Json instance provided by Hilt.
 */
@ProvidedTypeConverter // Allows Hilt to inject dependencies (like Json)
class Converters @Inject constructor(
    private val json: Json // Inject the Json instance
) {

    /**
     * Converts an InputAction object into its JSON string representation.
     * Returns null if the input InputAction is null.
     *
     * @param inputAction The InputAction object to convert.
     * @return JSON string representation or null.
     */
    @TypeConverter
    fun fromInputAction(inputAction: InputAction?): String? {
        return inputAction?.let { json.encodeToString(it) }
    }

    /**
     * Converts a JSON string representation back into an InputAction object.
     * Returns null if the input string is null or blank.
     * Includes error handling for JSON parsing.
     *
     * @param jsonString The JSON string to convert.
     * @return The deserialized InputAction object or null.
     */
    @TypeConverter
    fun toInputAction(jsonString: String?): InputAction? {
        return if (jsonString.isNullOrBlank()) {
            null
        } else {
            try {
                json.decodeFromString<InputAction>(jsonString)
            } catch (e: Exception) {
                // Log the error or handle it appropriately
                println("Error decoding InputAction JSON: $e")
                null // Return null if decoding fails
            }
        }
    }

    // --- Add converters for other complex types if needed ---
    // Example: If you stored List<String> directly (though often handled by Room)
    // @TypeConverter
    // fun fromStringList(list: List<String>?): String? {
    //     return list?.joinToString(",")
    // }
    //
    // @TypeConverter
    // fun toStringList(data: String?): List<String>? {
    //     return data?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    // }
}
