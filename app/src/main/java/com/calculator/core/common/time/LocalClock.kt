package com.calculator.core.common.time

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Clock

/**
 * The clock screens read "today" from, so tests and previews can fix the
 * date with `CompositionLocalProvider(LocalClock provides Clock.fixed(...))`.
 */
val LocalClock = staticCompositionLocalOf<Clock> { Clock.systemDefaultZone() }
