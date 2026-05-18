package com.calculator.core.math.di

import com.calculator.core.math.Evaluator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the math engine.
 *
 * The engine itself lives in `core/math/` and intentionally contains no
 * Android or DI imports - this module is the only bridge. Keeping it
 * separate means the engine can be reused from Kotlin Multiplatform or
 * a plain JVM context in the future without dragging Hilt along.
 *
 * Single-instance binding is safe because [Evaluator] is stateless and
 * thread-safe across calls.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object MathModule {
    @Provides
    @Singleton
    fun provideEvaluator(): Evaluator = Evaluator()
}
