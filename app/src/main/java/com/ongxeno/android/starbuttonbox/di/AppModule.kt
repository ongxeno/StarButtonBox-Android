package com.ongxeno.android.starbuttonbox.di

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import com.ongxeno.android.starbuttonbox.datasource.ConnectionManager
import com.ongxeno.android.starbuttonbox.datasource.LayoutRepository // Import new Repository
import com.ongxeno.android.starbuttonbox.datasource.MacroRepository
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource
import com.ongxeno.android.starbuttonbox.datasource.room.MacroDao
// Removed old Datasource imports
// import com.ongxeno.android.starbuttonbox.datasource.LayoutDatasource
// import com.ongxeno.android.starbuttonbox.datasource.TabDatasource
import com.ongxeno.android.starbuttonbox.utils.VibratorManagerUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier annotation for the application-level CoroutineScope.
 * This helps distinguish it if other scopes were needed.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope

/**
 * Hilt Module dedicated to providing CoroutineScopes.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    /**
     * Provides a singleton application-level CoroutineScope.
     * Uses SupervisorJob to prevent child job failures from cancelling the scope.
     * Uses Dispatchers.IO as a suitable default dispatcher for DataStore operations.
     */
    @Provides
    @Singleton
    @ApplicationScope // Use the qualifier
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}


/**
 * Main Hilt Module to provide application-wide dependencies.
 * Includes datasources, utilities, and system services.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule { // Renamed from DatasourceModule for clarity if it was separate

    // Provides the application context where needed
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    // Provides SettingDatasource as a singleton
    @Provides
    @Singleton
    fun provideSettingDatasource(@ApplicationContext context: Context): SettingDatasource {
        return SettingDatasource(context)
    }

    // Provides Vibrator system service (nullable)
    @Provides
    @Singleton
    fun provideVibrator(@ApplicationContext context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") // Keep for older APIs
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // Provides VibratorManagerUtils as a singleton
    @Provides
    @Singleton
    fun provideVibratorManagerUtils(vibrator: Vibrator?): VibratorManagerUtils {
        return VibratorManagerUtils(vibrator)
    }

    /**
     * Provides the singleton instance of ConnectionManager.
     * Depends on ApplicationContext, SettingDatasource, Json, and ApplicationScope.
     */
    @Provides
    @Singleton
    fun provideConnectionManager(
        @ApplicationContext context: Context,
        settingDatasource: SettingDatasource,
        json: Json, // Hilt will get this from DatabaseModule
        @ApplicationScope appScope: CoroutineScope
    ): ConnectionManager {
        return ConnectionManager(context, settingDatasource, json, appScope)
    }
}
