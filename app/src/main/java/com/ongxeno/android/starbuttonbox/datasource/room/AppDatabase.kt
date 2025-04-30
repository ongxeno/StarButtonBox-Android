package com.ongxeno.android.starbuttonbox.datasource.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The main Room database class for the application.
 * It defines the entities included in the database, the version,
 * and provides access to the DAOs.
 *
 * It also registers the necessary TypeConverters.
 */
@Database(
    entities = [MacroEntity::class], // List all your entities here
    version = 1, // Increment version when schema changes
    exportSchema = false // Set to true if you want to export schema for version control
)
@TypeConverters(Converters::class) // Register the TypeConverter for InputAction
abstract class AppDatabase : RoomDatabase() {

    /**
     * Abstract function to provide access to the MacroDao.
     * Room will generate the implementation for this.
     *
     * @return An instance of MacroDao.
     */
    abstract fun macroDao(): MacroDao

    // Add other DAOs here if you have more entities in the future
    // abstract fun otherDao(): OtherDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time. Use @Volatile to ensure visibility across threads.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "starbuttonbox_database" // Define database file name

        /**
         * Gets the singleton instance of the AppDatabase.
         * Uses the application context to avoid memory leaks.
         * Builds the database if it doesn't exist yet.
         *
         * Crucially, it adds the required `Converters` instance during the build,
         * which requires the `Json` instance (provided via Hilt later).
         *
         * @param context The application context.
         * @param converters The Converters instance (provided by Hilt).
         * @return The singleton AppDatabase instance.
         */
        fun getInstance(
            context: Context,
            converters: Converters // Accept the Converters instance
        ): AppDatabase {
            // If the instance is not null, return it, otherwise create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // Add the TypeConverter instance provided by Hilt
                    .addTypeConverter(converters)
                    // Add migrations here if needed in the future
                    // .addMigrations(MIGRATION_1_2)
                    // Optional: Fallback to destructive migration (clears DB on version mismatch)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // Return instance
                instance
            }
        }
    }
}
