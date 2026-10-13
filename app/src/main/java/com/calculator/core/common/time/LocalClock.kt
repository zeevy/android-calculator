package com.calculator.core.common.time

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * The clock screens read "today" from, so tests and previews can fix the
 * date with `CompositionLocalProvider(LocalClock provides Clock.fixed(...))`.
 * The default follows the phone's time zone, also after it changes.
 */
val LocalClock = staticCompositionLocalOf<Clock> { DeviceClock }

/** System time in the phone's current time zone, looked up on every call. */
private object DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
