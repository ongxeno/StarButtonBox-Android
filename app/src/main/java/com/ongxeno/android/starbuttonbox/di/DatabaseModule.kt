package com.ongxeno.android.starbuttonbox.di

import android.content.Context
import androidx.room.Room
import com.ongxeno.android.starbuttonbox.datasource.room.AppDatabase
import com.ongxeno.android.starbuttonbox.datasource.room.AppDatabaseCallback
import com.ongxeno.android.starbuttonbox.datasource.room.Converters
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
            prettyPrint = false // Use false for smaller storage size
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true // Important if defaults should be stored explicitly
            // Add serializersModule if complex polymorphism is needed later
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
     * Includes a callback to pre-populate the database on first creation.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        converters: Converters,
        macroDaoProvider: Provider<MacroDao>,
        @ApplicationScope scope: CoroutineScope,
        json: Json
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "starbuttonbox_database"
        )
            .addTypeConverter(converters)
            .addCallback(AppDatabaseCallback(context, macroDaoProvider, scope, json))
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

    // Add providers for other DAOs here if created later
}
