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
     * Provides AppDatabaseCallback as a Hilt-managed singleton.
     * This ensures its dependencies are resolved by Hilt before it's used.
     */
    @Provides
    @Singleton
    fun provideAppDatabaseCallback(
        @ApplicationContext context: Context,
        macroDaoProvider: Provider<MacroDao>,
        @ApplicationScope scope: CoroutineScope,
        json: Json
    ): AppDatabaseCallback {
        return AppDatabaseCallback(context, macroDaoProvider, scope, json)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        converters: Converters,
        appDatabaseCallback: AppDatabaseCallback
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "starbuttonbox_database"
        )
            .addTypeConverter(converters)
            .addCallback(appDatabaseCallback)
            .fallbackToDestructiveMigration()
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
