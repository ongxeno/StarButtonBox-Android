package com.ongxeno.android.starbuttonbox.datasource.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ongxeno.android.starbuttonbox.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import javax.inject.Provider

/**
 * The main Room database class for the application.
 * It defines the entities included in the database, the version,
 * and provides access to the DAOs.
 *
 * It also registers the necessary TypeConverters.
 */
@Database(
    entities = [
        MacroEntity::class,
        LayoutEntity::class,
        ButtonEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Abstract function to provide access to the MacroDao.
     * Room will generate the implementation for this.
     */
    abstract fun macroDao(): MacroDao

    /**
     * Abstract function to provide access to the LayoutDao.
     */
    abstract fun layoutDao(): LayoutDao // Added LayoutDao accessor

    /**
     * Abstract function to provide access to the ButtonDao.
     */
    abstract fun buttonDao(): ButtonDao // Added ButtonDao accessor

}
