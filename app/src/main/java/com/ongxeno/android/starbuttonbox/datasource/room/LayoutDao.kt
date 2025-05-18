package com.ongxeno.android.starbuttonbox.datasource.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Data Access Object (DAO) for the LayoutEntity.
 * Defines methods for interacting with the 'layouts' table in the Room database.
 */
@Dao
interface LayoutDao {

    /**
     * Inserts a single layout. If a layout with the same ID already exists, it replaces the old one.
     * @param layout The LayoutEntity object to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayout(layout: LayoutEntity)

    /**
     * Updates an existing layout.
     * @param layout The LayoutEntity object to update.
     */
    @Update
    suspend fun updateLayout(layout: LayoutEntity)

    /**
     * Deletes a specific layout from the database.
     * Note: Buttons associated with this layout will be deleted automatically due to cascading delete.
     * @param layout The LayoutEntity object to delete (matched by primary key).
     */
    @Delete
    suspend fun deleteLayout(layout: LayoutEntity)

    /**
     * Deletes a layout by its unique ID.
     * @param id The ID of the layout to delete.
     * @return The number of rows affected.
     */
    @Query("DELETE FROM layouts WHERE id = :id")
    suspend fun deleteLayoutById(id: String): Int

    /**
     * Retrieves a single layout by its unique ID as a Flow.
     * @param id The ID of the layout to retrieve.
     * @return A Flow emitting the LayoutEntity object or null if not found.
     */
    @Query("SELECT * FROM layouts WHERE id = :id LIMIT 1")
    fun getLayoutById(id: String): Flow<LayoutEntity?>

    /**
     * Retrieves all layouts from the database, ordered by their 'order_index', as a Flow.
     * The Flow will automatically emit a new list whenever the data in the 'layouts' table changes.
     * @return A Flow emitting the list of all LayoutEntity objects, ordered by 'order_index'.
     */
    @Query("SELECT * FROM layouts ORDER BY order_index ASC")
    fun getAllLayoutsOrdered(): Flow<List<LayoutEntity>>

    /**
     * Updates the orderIndex for a list of layouts.
     * This is typically called in a transaction.
     * @param layouts The list of LayoutEntity objects with updated orderIndex values.
     */
    @Update
    suspend fun updateLayouts(layouts: List<LayoutEntity>)

    /**
     * Transaction to update the order of layouts.
     * Fetches all layouts, reorders them based on the provided list of IDs,
     * updates their orderIndex, and then updates them in the database.
     *
     * @param orderedIds List of layout IDs in the new desired order.
     */
    @Transaction
    suspend fun updateLayoutOrder(orderedIds: List<String>) {
        val layouts = getAllLayoutsOrdered().first() // Get current layouts
        val layoutMap = layouts.associateBy { it.id }.toMutableMap()
        val updatedLayouts = mutableListOf<LayoutEntity>()

        orderedIds.forEachIndexed { index, id ->
            layoutMap[id]?.let { layout ->
                if (layout.orderIndex != index) {
                    updatedLayouts.add(layout.copy(orderIndex = index))
                }
                layoutMap.remove(id) // Remove processed layouts
            }
        }
        // Handle any layouts not in orderedIds (e.g., newly added but not yet ordered)
        // For simplicity, we assume orderedIds contains all relevant layouts or
        // that unmentioned layouts retain their order or are appended.
        // A more robust solution might re-index all remaining layouts from layoutMap.

        if (updatedLayouts.isNotEmpty()) {
            updateLayouts(updatedLayouts)
        }
    }


    /**
     * Deletes all layouts from the table. Use with caution!
     * This will also delete all associated buttons due to cascading delete.
     */
    @Query("DELETE FROM layouts")
    suspend fun deleteAllLayouts()
}