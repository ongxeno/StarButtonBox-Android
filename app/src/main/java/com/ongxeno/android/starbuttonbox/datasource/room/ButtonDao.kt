package com.ongxeno.android.starbuttonbox.datasource.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the ButtonEntity.
 * Defines methods for interacting with the 'buttons' table in the Room database.
 */
@Dao
interface ButtonDao {

    /**
     * Inserts a single button. If a button with the same ID already exists, it replaces the old one.
     * @param button The ButtonEntity object to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButton(button: ButtonEntity)

    /**
     * Inserts a list of buttons. If any button has an ID that already exists, it replaces the old one.
     * @param buttons The list of ButtonEntity objects to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllButtons(buttons: List<ButtonEntity>)

    /**
     * Updates an existing button.
     * @param button The ButtonEntity object to update.
     */
    @Update
    suspend fun updateButton(button: ButtonEntity)

    /**
     * Updates a list of existing buttons.
     * @param buttons The list of ButtonEntity objects to update.
     */
    @Update
    suspend fun updateAllButtons(buttons: List<ButtonEntity>)


    /**
     * Deletes a specific button from the database.
     * @param button The ButtonEntity object to delete (matched by primary key).
     */
    @Delete
    suspend fun deleteButton(button: ButtonEntity)

    /**
     * Deletes a button by its unique ID.
     * @param id The ID of the button to delete.
     * @return The number of rows affected.
     */
    @Query("DELETE FROM buttons WHERE id = :id")
    suspend fun deleteButtonById(id: String): Int

    /**
     * Deletes all buttons associated with a specific layout ID.
     * This is useful when a layout is deleted or its buttons are being replaced.
     * @param layoutId The ID of the parent layout.
     * @return The number of rows affected.
     */
    @Query("DELETE FROM buttons WHERE layout_id = :layoutId")
    suspend fun deleteButtonsByLayoutId(layoutId: String): Int

    /**
     * Retrieves a single button by its unique ID as a Flow.
     * @param id The ID of the button to retrieve.
     * @return A Flow emitting the ButtonEntity object or null if not found.
     */
    @Query("SELECT * FROM buttons WHERE id = :id LIMIT 1")
    fun getButtonById(id: String): Flow<ButtonEntity?>

    /**
     * Retrieves all buttons associated with a specific layout ID, ordered by 'order_in_layout', as a Flow.
     * The Flow will automatically emit a new list whenever buttons for that layout change.
     * @param layoutId The ID of the parent layout.
     * @return A Flow emitting the list of ButtonEntity objects for the specified layout.
     */
    @Query("SELECT * FROM buttons WHERE layout_id = :layoutId ORDER BY order_in_layout ASC")
    fun getButtonsForLayout(layoutId: String): Flow<List<ButtonEntity>>

    /**
     * Deletes all buttons from the table. Use with caution!
     */
    @Query("DELETE FROM buttons")
    suspend fun deleteAllButtons()
}
