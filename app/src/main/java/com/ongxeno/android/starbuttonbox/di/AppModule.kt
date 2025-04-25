package com.ongxeno.android.starbuttonbox.di // Create a 'di' package

import android.content.Context
// Removed SoundPool import
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat.getSystemService
import com.ongxeno.android.starbuttonbox.datasource.LayoutDatasource
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource
import com.ongxeno.android.starbuttonbox.datasource.TabDatasource
// Removed SoundPlayer import - Hilt finds it via @Inject constructor
import com.ongxeno.android.starbuttonbox.utils.VibratorManagerUtils // Keep VibratorManagerUtils import
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module to provide application-wide dependencies.
 * Installed in SingletonComponent means these dependencies live as long as the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

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

    // Provides LayoutDatasource as a singleton
    @Provides
    @Singleton
    fun provideLayoutDatasource(@ApplicationContext context: Context): LayoutDatasource {
        return LayoutDatasource(context)
    }

    // Provides TabDatasource as a singleton
    @Provides
    @Singleton
    fun provideTabDatasource(@ApplicationContext context: Context): TabDatasource {
        return TabDatasource(context)
    }

    // Removed provideSoundPool() - SoundPlayer manages its own SoundPool internally

    // Removed provideSoundPlayer() - Hilt can now inject SoundPlayer directly
    // because its constructor is annotated with @Inject and the class with @Singleton.

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
    // It depends on the nullable Vibrator? provided above
    @Provides
    @Singleton
    fun provideVibratorManagerUtils(vibrator: Vibrator?): VibratorManagerUtils {
        // Pass the Vibrator? (nullable) to the constructor
        // VibratorManagerUtils class should handle the null case internally
        return VibratorManagerUtils(vibrator)
    }

    // Note: UdpSender is not provided here as a Singleton because its target IP/Port
    // depends on the SettingDatasource flow. It's better managed within the ViewModel
    // or a component that observes the flow.
}
