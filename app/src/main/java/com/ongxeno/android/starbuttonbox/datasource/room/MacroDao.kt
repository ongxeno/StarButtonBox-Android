package com.ongxeno.android.starbuttonbox.datasource.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the Macro entity.
 * Defines methods for interacting with the 'macros' table in the Room database.
 */
@Dao
interface MacroDao {

    /**
     * Inserts a single macro. If a macro with the same ID already exists, it replaces the old one.
     *
     * @param macro The Macro object to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMacro(macro: MacroEntity)

    /**
     * Inserts a list of macros. If any macro has an ID that already exists, it replaces the old one.
     * Useful for initializing or replacing data in bulk.
     *
     * @param macros The list of Macro objects to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAllMacros(macros: List<MacroEntity>)

    /**
     * Updates an existing macro. Finds the macro based on its primary key (id).
     * Consider using insertOrUpdateMacro instead if you want insert-or-replace behavior.
     *
     * @param macro The Macro object to update.
     */
    @Update
    suspend fun updateMacro(macro: MacroEntity)

    /**
     * Deletes a specific macro from the database.
     *
     * @param macro The Macro object to delete (matched by primary key).
     */
    @Delete
    suspend fun deleteMacro(macro: MacroEntity)

    /**
     * Deletes a macro by its unique ID.
     *
     * @param id The ID of the macro to delete.
     * @return The number of rows affected (should be 0 or 1).
     */
    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun deleteMacroById(id: String): Int

    /**
     * Retrieves a single macro by its unique ID. Returns null if not found.
     *
     * @param id The ID of the macro to retrieve.
     * @return The Macro object or null.
     */
    @Query("SELECT * FROM macros WHERE id = :id LIMIT 1")
    suspend fun getMacroById(id: String): MacroEntity?

    /**
     * Retrieves all macros from the database as a Flow.
     * The Flow will automatically emit a new list whenever the data in the 'macros' table changes.
     *
     * @return A Flow emitting the list of all Macro objects.
     */
    @Query("SELECT * FROM macros ORDER BY title ASC") // Example ordering by title
    fun getAllMacros(): Flow<List<MacroEntity>>

    /**
     * Retrieves all macros for a specific game ID as a Flow.
     * The Flow will automatically emit a new list whenever macros for that game change.
     *
     * @param gameId The ID of the game to filter by.
     * @return A Flow emitting the list of Macro objects for the specified game.
     */
    @Query("SELECT * FROM macros WHERE game_id = :gameId ORDER BY title ASC")
    fun getMacrosByGameId(gameId: String): Flow<List<MacroEntity>>

    /**
     * Deletes all macros from the table. Use with caution!
     */
    @Query("DELETE FROM macros")
    suspend fun clearAllMacros()

}
