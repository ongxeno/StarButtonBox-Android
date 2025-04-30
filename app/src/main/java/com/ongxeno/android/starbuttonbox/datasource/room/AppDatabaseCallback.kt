package com.ongxeno.android.starbuttonbox.datasource.room

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ongxeno.android.starbuttonbox.R
import com.ongxeno.android.starbuttonbox.data.InputAction
import com.ongxeno.android.starbuttonbox.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Provider

class AppDatabaseCallback @Inject constructor(
    private val context: Context,
    private val macroDaoProvider: Provider<MacroDao>,
    @ApplicationScope private val scope: CoroutineScope,
    private val json: Json
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch {
            populateDatabase()
        }
    }

    /** Loads default data from JSON and inserts it into the database. */
    suspend fun populateDatabase() {
        println("Database created. Populating with default macros...")
        try {
            val defaultMacros = loadDefaultMacrosFromJson(context, json)
            if (defaultMacros.isNotEmpty()) {
                macroDaoProvider.get().insertOrUpdateAllMacros(defaultMacros)
                println("Successfully populated ${defaultMacros.size} default macros.")
            } else {
                println("No default macros found to populate.")
            }
        } catch (e: Exception) {
            println("Error populating database with default macros: $e")
            // Consider more robust error handling/logging
        }
    }

    /** Reads and parses the default macros from res/raw/default_macros.json */
    private fun loadDefaultMacrosFromJson(context: Context, json: Json): List<MacroEntity> {
        val macroEntities = mutableListOf<MacroEntity>()
        try {
            context.resources.openRawResource(R.raw.default_macros).use { inputStream ->
                 BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val jsonString = reader.readText()
                    val jsonItems = json.decodeFromString<List<DefaultMacroJsonItem>>(jsonString)

                    for (item in jsonItems) {
                        val defaultInputActionObject: InputAction? = item.inputAction?.let { actionJson ->
                            try {
                                json.decodeFromString<InputAction>(actionJson)
                            } catch (e: Exception) {
                                println("Error deserializing InputAction for ${item.xmlActionName}: $e")
                                null
                            }
                        }
                        val entityId = generateUniqueId(item.xmlCategoryName, item.xmlActionName)

                        macroEntities.add(
                            MacroEntity(
                                id = entityId,
                                xmlCategoryName = item.xmlCategoryName,
                                xmlActionName = item.xmlActionName,
                                label = item.label, // Use label from JSON
                                title = item.title,
                                description = item.description,
                                gameId = item.gameId, // Use gameId from JSON
                                defaultInputAction = defaultInputActionObject,
                                customInputAction = null,
                                isUserCreated = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println("Error reading or parsing default_macros.json: $e")
            // Return empty list or rethrow depending on desired error handling
        }
        return macroEntities
    }

    /** Generates a unique ID based on raw category and action names. */
    private fun generateUniqueId(categoryRaw: String, actionRaw: String): String {
        val safeCategory = categoryRaw.replace(Regex("\\W+"), "_").takeIf { it.isNotEmpty() } ?: "default"
        val safeAction = actionRaw.replace(Regex("\\W+"), "_")
        return "sc_${safeCategory}_${safeAction}"
    }
}