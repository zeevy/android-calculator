package com.calculator.core.domain.datetime

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Timezone conversion as a pure-Kotlin function: take a wall-clock
 * [LocalDateTime] in [sourceZone] and return the equivalent wall-clock
 * time in [targetZone].
 *
 * No I/O. No Android imports. The screen layer parses user input into
 * a [LocalDateTime], hands it here, and renders the result.
 *
 * DST and historical offset changes are handled by [ZoneId] / [ZonedDateTime]
 * - the engine itself does no offset math.
 */
object TimezoneConverter {
    /**
     * Convert [time] (interpreted as a wall-clock instant in
     * [sourceZone]) to its equivalent wall-clock instant in [targetZone].
     *
     * Ambiguous local times (the "fall back" hour that exists twice on
     * a DST end) resolve to the EARLIER offset, matching what
     * `ZonedDateTime.of(LocalDateTime, ZoneId)` does by default.
     *
     * Gap times (the "spring forward" hour that never exists) shift
     * forward into the next valid instant, again matching the JDK
     * default.
     */
    fun convert(
        time: LocalDateTime,
        sourceZone: ZoneId,
        targetZone: ZoneId,
    ): LocalDateTime {
        val sourceZdt = ZonedDateTime.of(time, sourceZone)
        return sourceZdt.withZoneSameInstant(targetZone).toLocalDateTime()
    }

    /**
     * Common-use IANA zones, in display order. The screen surfaces
     * these at the top of the zone picker so the most-used choices are
     * a single tap away; the full list (via [ZoneId.getAvailableZoneIds])
     * lives below.
     *
     * Ordering preference: UTC first (it's the reference), then IST
     * (this app is India-leaning), then major financial centres roughly
     * westbound from Asia. Add or shuffle freely; only the zone ids
     * are load-bearing.
     */
    val COMMON_ZONES: List<String> =
        listOf(
            "UTC",
            "Asia/Kolkata",         // IST
            "Asia/Tokyo",           // JST
            "Asia/Shanghai",        // CST
            "Asia/Singapore",
            "Asia/Dubai",           // GST
            "Europe/London",        // GMT / BST
            "Europe/Paris",         // CET / CEST
            "Europe/Berlin",
            "America/New_York",     // EST / EDT
            "America/Chicago",      // CST / CDT
            "America/Los_Angeles",  // PST / PDT
            "Australia/Sydney",     // AEST / AEDT
        )

    /**
     * All IANA zone ids the JDK knows about, sorted alphabetically.
     * Used as the fallback list in the zone picker (after the common
     * zones at the top).
     */
    fun allZoneIds(): List<String> = ZoneId.getAvailableZoneIds().sorted()
}
