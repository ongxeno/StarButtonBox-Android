package com.ongxeno.android.starbuttonbox.datasource

import com.ongxeno.android.starbuttonbox.datasource.room.MacroEntity // Use MacroEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface for accessing and manipulating Macro data.
 * Abstracts the underlying data source (Room DAO).
 */
interface MacroRepository {

    /**
     * Provides a Flow emitting the current list of all stored macros.
     */
    fun getAllMacros(): Flow<List<MacroEntity>>

    /**
     * Provides a Flow emitting the list of macros for a specific game.
     *
     * @param gameId The ID of the game to filter by.
     */
    fun getMacrosByGameId(gameId: String): Flow<List<MacroEntity>>

    /**
     * Retrieves a single macro by its ID. Returns null if not found.
     *
     * @param macroId The unique ID of the macro to retrieve.
     */
    suspend fun getMacroById(macroId: String): MacroEntity?

    /**
     * Adds or updates a macro in the repository.
     *
     * @param macro The MacroEntity object to add or update.
     */
    suspend fun addOrUpdateMacro(macro: MacroEntity)

    /**
     * Adds or updates a list of macros.
     *
     * @param macros The list of MacroEntity objects to add or update.
     */
    suspend fun addOrUpdateAllMacros(macros: List<MacroEntity>)

    /**
     * Deletes a macro from the repository based on its ID.
     *
     * @param macroId The unique ID of the macro to delete.
     */
    suspend fun deleteMacroById(macroId: String)

    /**
     * Deletes a specific macro object.
     *
     * @param macro The MacroEntity object to delete.
     */
    suspend fun deleteMacro(macro: MacroEntity)


    /**
     * Clears all macros from the repository. Use with caution.
     */
    suspend fun clearAllMacros()
}
