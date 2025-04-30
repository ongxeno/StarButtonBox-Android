package com.ongxeno.android.starbuttonbox.datasource.room

import android.content.Context
import com.ongxeno.android.starbuttonbox.R // Import your project's R class
import com.ongxeno.android.starbuttonbox.data.Game
import com.ongxeno.android.starbuttonbox.data.InputAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID // For generating IDs if needed

/**
 * Represents the structure of each object in the default_macros.json file.
 */
@Serializable
data class DefaultMacroJsonItem(
    val xmlCategoryName: String,
    val xmlActionName: String,
    val label: String,
    val title: String,
    val description: String,
    val inputAction: String?, // The serialized InputAction JSON string
    val gameId: String
)

/**
 * Provides functionality to load default macro data from the raw JSON resource.
 */
object DefaultMacrosData {

    /**
     * Reads the default_macros.json file from raw resources, parses it,
     * and converts it into a list of MacroEntity objects.
     *
     * @param context Application context to access resources.
     * @param json Kotlinx serialization Json instance to parse InputAction strings.
     * @return A list of MacroEntity objects representing the default data.
     */
    fun getDefaultMacros(context: Context, json: Json): List<MacroEntity> {
        val macroEntities = mutableListOf<MacroEntity>()
        try {
            // 1. Read the raw JSON file content
            val inputStream = context.resources.openRawResource(R.raw.default_macros)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            reader.close()
            inputStream.close()

            // 2. Deserialize the main JSON array
            val jsonItems = json.decodeFromString<List<DefaultMacroJsonItem>>(jsonString)

            // 3. Process each item and convert to MacroEntity
            for (item in jsonItems) {
                // 3a. Deserialize the nested inputAction string
                val defaultInputActionObject: InputAction? = item.inputAction?.let { actionJson ->
                    try {
                        json.decodeFromString<InputAction>(actionJson)
                    } catch (e: Exception) {
                        println("Error deserializing InputAction for ${item.xmlActionName}: $e")
                        null // Assign null if deserialization fails
                    }
                }

                // 3b. Create the MacroEntity
                // Generate a unique ID here based on XML names for consistency
                val entityId = generateUniqueId(item.xmlCategoryName, item.xmlActionName)

                macroEntities.add(
                    MacroEntity(
                        id = entityId, // Use generated consistent ID
                        xmlCategoryName = item.xmlCategoryName,
                        xmlActionName = item.xmlActionName,
                        title = item.title, // Use title from JSON
                        label = item.label,
                        // Consider using the generated 'label' from JSON if you add it to MacroEntity
                        description = item.description, // Use description from JSON
                        gameId = item.gameId, // Use gameId from JSON (should be constant)
                        defaultInputAction = defaultInputActionObject, // Assign the deserialized object
                        customInputAction = null, // Always null for defaults
                        isUserCreated = false // Always false for defaults
                    )
                )
            }
        } catch (e: Exception) {
            // Handle errors reading/parsing the JSON file
            println("Error loading default macros from JSON: $e")
            // Optionally return an empty list or throw an exception
        }
        return macroEntities
    }

    /**
     * Generates a unique ID based on raw category and action names.
     * (Helper function, same as used in the Python script potentially)
     */
    private fun generateUniqueId(categoryRaw: String, actionRaw: String): String {
        val safeCategory = categoryRaw.replace(Regex("\\W+"), "_").takeIf { it.isNotEmpty() } ?: "default"
        val safeAction = actionRaw.replace(Regex("\\W+"), "_")
        return "sc_${safeCategory}_${safeAction}"
    }
}
