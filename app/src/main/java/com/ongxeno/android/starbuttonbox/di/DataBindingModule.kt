package com.ongxeno.android.starbuttonbox.di

import com.ongxeno.android.starbuttonbox.datasource.LayoutRepository
import com.ongxeno.android.starbuttonbox.datasource.MacroRepository // Import Repository interface
import com.ongxeno.android.starbuttonbox.datasource.MacroRepositoryImpl // Import Repository implementation
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingModule {

    // Bind MacroRepository interface to its implementation
    @Binds
    @Singleton
    abstract fun bindMacroRepository(impl: MacroRepositoryImpl): MacroRepository
}
