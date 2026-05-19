package com.calculator.feature.converter.currency

/**
 * State for the Currency Converter screen.
 *
 * @property amount Raw text the user typed in the base-currency field;
 *   kept as a string so partial entries like "1." don't have to be
 *   valid Doubles yet.
 * @property base Currently selected base currency code (e.g. "USD").
 * @property allCodes Every code present in the cached rates table,
 *   alphabetically sorted - used for the picker dialog.
 * @property visibleCodes Codes shown in the converter list, favourites
 *   first then the rest alphabetically.
 * @property rates Map of code → rate-vs-base. Empty until the first
 *   successful fetch / cache load.
 * @property favorites Set of pinned codes.
 * @property fetchedAtUtc Epoch millis when the visible rates were
 *   fetched, or null when there's no data at all.
 * @property isRefreshing True while a network call is in flight.
 * @property errorMessage Non-null when the most recent refresh failed;
 *   the cached rows still render underneath.
 */
data class CurrencyConverterUiState(
    val amount: String = "1",
    val base: String = "USD",
    val allCodes: List<String> = emptyList(),
    val visibleCodes: List<String> = emptyList(),
    val rates: Map<String, Double> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val fetchedAtUtc: Long? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)
