package com.ongxeno.android.starbuttonbox.datasource.room

import android.content.Context
import android.util.Log // Import Android Log
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

    private val TAG = "AppDatabaseCallback" // Added TAG for logging

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch {
            populateDatabase()
        }
    }

    suspend fun populateDatabase() {
        Log.i(TAG, "Database created. Populating with default macros...")
        try {
            val defaultMacros = loadDefaultMacrosFromJson(context, json)
            if (defaultMacros.isNotEmpty()) {
                macroDaoProvider.get().insertOrUpdateAllMacros(defaultMacros)
                Log.i(TAG, "Successfully populated ${defaultMacros.size} default macros.")
            } else {
                Log.w(TAG, "No default macros found or loaded to populate.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error populating database with default macros", e)
        }
    }

    private fun loadDefaultMacrosFromJson(context: Context, json: Json): List<MacroEntity> {
        val macroEntities = mutableListOf<MacroEntity>()
        try {
            context.resources.openRawResource(R.raw.default_macros).use { inputStream ->
                 BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val jsonString = reader.readText()
                    val jsonItems = json.decodeFromString<List<DefaultMacroJsonItem>>(jsonString)

                    for (item in jsonItems) {
                        val entityId = generateUniqueId(item.xmlCategoryName, item.xmlActionName)
                        val defaultInputActionObject: InputAction? =
                            item.inputAction?.takeIf { it.isNotBlank() }?.let { actionJson ->
                                try {
                                    json.decodeFromString<InputAction>(actionJson)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error deserializing InputAction for Macro ID '$entityId' (XML: ${item.xmlCategoryName}/${item.xmlActionName}). JSON: '$actionJson'", e)
                                    null
                                }
                            }

                        macroEntities.add(
                            MacroEntity(
                                id = entityId,
                                xmlCategoryName = item.xmlCategoryName,
                                xmlActionName = item.xmlActionName,
                                label = item.label,
                                title = item.title,
                                description = item.description,
                                gameId = item.gameId,
                                defaultInputAction = defaultInputActionObject,
                                customInputAction = null,
                                isUserCreated = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading or parsing default_macros.json", e)
        }
        return macroEntities
    }

    private fun generateUniqueId(categoryRaw: String, actionRaw: String): String {
        val safeCategory = categoryRaw.replace(Regex("\\W+"), "_").takeIf { it.isNotEmpty() } ?: "default"
        val safeAction = actionRaw.replace(Regex("\\W+"), "_")
        return "sc_${safeCategory}_${safeAction}"
    }
}
