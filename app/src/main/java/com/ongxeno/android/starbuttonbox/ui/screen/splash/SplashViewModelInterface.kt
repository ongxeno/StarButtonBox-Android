package com.ongxeno.android.starbuttonbox.ui.screen.splash

import kotlinx.coroutines.flow.StateFlow

interface SplashViewModelInterface {
    val isLoading: StateFlow<Boolean>
    val isFirstLaunch: StateFlow<Boolean>
    val isInitializationComplete: StateFlow<Boolean>
    fun markFirstLaunchCompletedInViewModel()
}