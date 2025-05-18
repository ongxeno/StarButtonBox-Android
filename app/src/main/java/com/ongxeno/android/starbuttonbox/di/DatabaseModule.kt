package com.ongxeno.android.starbuttonbox.di

import android.content.Context
import androidx.room.Room
import com.ongxeno.android.starbuttonbox.datasource.room.AppDatabase
import com.ongxeno.android.starbuttonbox.datasource.room.AppDatabaseCallback
import com.ongxeno.android.starbuttonbox.datasource.room.ButtonDao
import com.ongxeno.android.starbuttonbox.datasource.room.Converters
import com.ongxeno.android.starbuttonbox.datasource.room.LayoutDao
import com.ongxeno.android.starbuttonbox.datasource.room.MacroDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the singleton instance of the kotlinx.serialization Json processor.
     * Configured for leniency and ignoring unknown keys.
     */
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            prettyPrint = false
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            classDiscriminator = "_type" // Ensure this is present if used in your data classes
        }
    }

    /**
     * Provides the singleton instance of the Room Type Converters.
     * Requires the Json instance.
     */
    @Provides
    @Singleton
    fun provideConverters(json: Json): Converters {
        return Converters(json)
    }

    /**
     * Provides the singleton instance of the AppDatabase.
     * Includes a callback to pre-populate the database on first creation (primarily for macros).
     * Default layouts and buttons will be populated by LayoutRepository on first launch.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        converters: Converters, // Converters for InputAction etc.
        // Provider for MacroDao for the AppDatabaseCallback (for default macros)
        macroDaoProvider: Provider<MacroDao>,
        @ApplicationScope scope: CoroutineScope, // For the callback
        jsonForCallback: Json // Json instance specifically for the callback, if needed distinctively
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "starbuttonbox_database" // Database name
        )
            .addTypeConverter(converters) // Add type converters
            // The callback is primarily for default macros.
            // Default layouts/buttons will be handled by LayoutRepository.
            .addCallback(AppDatabaseCallback(context, macroDaoProvider, scope, jsonForCallback))
            // Since we are not migrating data from the old DataStore to the new tables,
            // and we've incremented the version, a destructive migration will occur by default.
            // This is acceptable as per the revised plan.
            // If you wanted to preserve other tables (like macros) and only add new ones,
            // you would add specific migrations.
            .fallbackToDestructiveMigration() // Handles schema changes by rebuilding the DB
            .build()
    }

    /**
     * Provides the singleton instance of the MacroDao.
     * Requires the AppDatabase instance.
     */
    @Provides
    @Singleton
    fun provideMacroDao(appDatabase: AppDatabase): MacroDao {
        return appDatabase.macroDao()
    }

    /**
     * Provides the singleton instance of the LayoutDao.
     * Requires the AppDatabase instance.
     */
    @Provides
    @Singleton
    fun provideLayoutDao(appDatabase: AppDatabase): LayoutDao {
        return appDatabase.layoutDao()
    }

    /**
     * Provides the singleton instance of the ButtonDao.
     * Requires the AppDatabase instance.
     */
    @Provides
    @Singleton
    fun provideButtonDao(appDatabase: AppDatabase): ButtonDao {
        return appDatabase.buttonDao()
    }
}
