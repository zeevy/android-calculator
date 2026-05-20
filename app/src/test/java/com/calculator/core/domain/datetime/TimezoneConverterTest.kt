package com.calculator.core.domain.datetime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Golden + invariant tests for [TimezoneConverter].
 *
 * Verified against published TZ-database offsets and well-known DST
 * transition dates. Each parameterized row pins one (input wall-time,
 * source zone, target zone, expected wall-time) tuple.
 */
class TimezoneConverterTest {
    @DisplayName("convert between fixed-offset zones (no DST)")
    @ParameterizedTest(name = "{0} {1} -> {2} = {3}")
    @CsvSource(
        // UTC <-> IST (always +05:30)
        "'2025-05-20T12:00',  UTC,            Asia/Kolkata,        '2025-05-20T17:30'",
        "'2025-05-20T17:30',  Asia/Kolkata,   UTC,                 '2025-05-20T12:00'",
        // UTC <-> Tokyo (always +09:00)
        "'2025-05-20T00:00',  UTC,            Asia/Tokyo,          '2025-05-20T09:00'",
        "'2025-05-20T09:00',  Asia/Tokyo,     UTC,                 '2025-05-20T00:00'",
        // Day-boundary crossing
        "'2025-05-20T23:00',  Asia/Kolkata,   UTC,                 '2025-05-20T17:30'",
        "'2025-05-20T01:00',  Asia/Tokyo,     UTC,                 '2025-05-19T16:00'",
        // Cross-Pacific
        "'2025-05-20T10:00',  Asia/Tokyo,     America/Los_Angeles, '2025-05-19T18:00'",
        // Dubai (+04:00)
        "'2025-05-20T12:00',  Asia/Dubai,     UTC,                 '2025-05-20T08:00'",
        // Singapore (+08:00)
        "'2025-05-20T12:00',  Asia/Singapore, UTC,                 '2025-05-20T04:00'",
    )
    fun convertsFixedOffsets(
        input: String,
        sourceZone: String,
        targetZone: String,
        expected: String,
    ) {
        val out =
            TimezoneConverter.convert(
                time = LocalDateTime.parse(input),
                sourceZone = ZoneId.of(sourceZone),
                targetZone = ZoneId.of(targetZone),
            )
        assertEquals(LocalDateTime.parse(expected), out)
    }

    @DisplayName("convert during DST-active windows")
    @ParameterizedTest(name = "{0} {1} -> {2} = {3}")
    @CsvSource(
        // London BST (= UTC+1) in mid-summer
        "'2025-07-15T12:00',  Europe/London,     UTC,             '2025-07-15T11:00'",
        // London GMT (= UTC+0) in mid-winter
        "'2025-01-15T12:00',  Europe/London,     UTC,             '2025-01-15T12:00'",
        // New York EDT (= UTC-4) in mid-summer
        "'2025-07-15T12:00',  America/New_York,  UTC,             '2025-07-15T16:00'",
        // New York EST (= UTC-5) in mid-winter
        "'2025-01-15T12:00',  America/New_York,  UTC,             '2025-01-15T17:00'",
        // Sydney AEST/AEDT - southern hemisphere DST is INVERTED
        // (active around our winter, dormant around our summer)
        "'2025-07-15T12:00',  Australia/Sydney,  UTC,             '2025-07-15T02:00'",
        "'2025-01-15T12:00',  Australia/Sydney,  UTC,             '2025-01-15T01:00'",
    )
    fun convertsAcrossDstWindows(
        input: String,
        sourceZone: String,
        targetZone: String,
        expected: String,
    ) {
        val out =
            TimezoneConverter.convert(
                time = LocalDateTime.parse(input),
                sourceZone = ZoneId.of(sourceZone),
                targetZone = ZoneId.of(targetZone),
            )
        assertEquals(LocalDateTime.parse(expected), out)
    }

    @Test
    fun roundTripRecoversOriginal() {
        // Any zone-to-zone-and-back conversion must return the same
        // wall-clock instant for non-DST-transition times.
        val src = ZoneId.of("Asia/Kolkata")
        val dst = ZoneId.of("America/New_York")
        val original = LocalDateTime.parse("2025-05-20T17:30")
        val toNyc = TimezoneConverter.convert(original, src, dst)
        val back = TimezoneConverter.convert(toNyc, dst, src)
        assertEquals(original, back)
    }

    @Test
    fun sameZoneIsIdentity() {
        // Converting a time within the same zone is a no-op.
        val zone = ZoneId.of("Asia/Tokyo")
        val time = LocalDateTime.parse("2025-05-20T09:30")
        assertEquals(time, TimezoneConverter.convert(time, zone, zone))
    }

    @Test
    fun commonZonesAreAllValid() {
        // Every entry in COMMON_ZONES must be a JDK-known zone id, or
        // the picker will explode when the user taps it.
        TimezoneConverter.COMMON_ZONES.forEach { id ->
            // ZoneId.of throws for unknown ids; the assertion is that
            // none do.
            ZoneId.of(id)
        }
    }

    @Test
    fun allZoneIdsListIsNonEmptyAndSorted() {
        val ids = TimezoneConverter.allZoneIds()
        assert(ids.isNotEmpty()) { "expected a non-empty zone list" }
        assertEquals(ids, ids.sorted())
    }
}
