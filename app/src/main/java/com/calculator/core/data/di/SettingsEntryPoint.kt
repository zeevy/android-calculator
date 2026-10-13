package com.calculator.core.data.di

import com.calculator.core.data.settings.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Gives the home-screen widget and the floating calculator, which Hilt
 * cannot inject, the app's [SettingsRepository].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsEntryPoint {
    /** The app's single settings repository. */
    fun settingsRepository(): SettingsRepository
}
