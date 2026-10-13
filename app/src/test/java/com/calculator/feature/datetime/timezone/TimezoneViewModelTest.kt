package com.calculator.feature.datetime.timezone

import androidx.lifecycle.SavedStateHandle
import com.calculator.testing.FakeSettingsRepository
import com.calculator.testing.MainDispatcherExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** The timezone converter starts from the real current time in each zone. */
class TimezoneViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    // 10:00 on a phone in India is 04:30 UTC.
    private val indiaPhone = Clock.fixed(Instant.parse("2026-10-13T04:30:00Z"), ZoneId.of("Asia/Kolkata"))

    @Test
    fun `the first now is right on a phone that is not on UTC`() {
        val viewModel = TimezoneViewModel(FakeSettingsRepository(), SavedStateHandle(), indiaPhone)
        val state = viewModel.state.value

        assertEquals("UTC", state.fromZone)
        assertEquals(LocalDateTime.of(2026, 10, 13, 4, 30), state.fromTime)
        assertEquals("Asia/Kolkata", state.toZone)
        assertEquals(LocalDateTime.of(2026, 10, 13, 10, 0), state.toTime)
    }

    @Test
    fun `a time the user picked comes back after process death`() {
        val picked = LocalDateTime.of(2026, 1, 2, 18, 15)
        val handle = SavedStateHandle(mapOf("timezone.fromTime" to picked, "timezone.lastEdited" to "From"))
        val viewModel = TimezoneViewModel(FakeSettingsRepository(), handle, indiaPhone)

        assertEquals(picked, viewModel.state.value.fromTime)
        assertEquals(LocalDateTime.of(2026, 1, 2, 23, 45), viewModel.state.value.toTime)
    }
}
