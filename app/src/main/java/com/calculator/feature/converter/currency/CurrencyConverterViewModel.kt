package com.calculator.feature.converter.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculator.core.data.rates.Rates
import com.calculator.core.data.rates.RatesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the currency converter screen.
 *
 * Reads cached rates from the repository (Flow) so the UI always
 * renders something even offline. On first launch with an empty cache,
 * fires a refresh; the user can also tap the refresh button.
 *
 * Conversion is `amount × rate` after re-basing if needed - we always
 * compare against the cached base. If the user picks a different base
 * we re-fetch (so all rates are expressed against the new base).
 */
@HiltViewModel
class CurrencyConverterViewModel
    @Inject
    constructor(
        private val repository: RatesRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(CurrencyConverterUiState())
        val state: StateFlow<CurrencyConverterUiState> = _state.asStateFlow()

        init {
            // Combine rates + favourites into one downstream emission
            // so the UI never sees a half-applied state where rates
            // updated but favourites haven't (which would briefly
            // reshuffle the list). `combine` waits for both upstreams
            // to have emitted at least once, then re-emits whenever
            // either one changes.
            viewModelScope.launch {
                combine(
                    repository.observeCached(),
                    repository.observeFavorites(),
                ) { rates, favs -> rates to favs }
                    .collect { (rates, favs) -> applySnapshot(rates, favs.toSet()) }
            }
            // Fire a refresh on launch. If we already have cached data
            // the UI shows it immediately while the call runs - the
            // observe-combine above keeps emitting whatever is in the
            // DB, and the network result lands via the same path when
            // `refresh()` writes new rows.
            viewModelScope.launch { refresh() }
        }

        fun setAmount(text: String) {
            _state.update { it.copy(amount = text) }
        }

        fun setBase(code: String) {
            _state.update { it.copy(base = code) }
            viewModelScope.launch { refresh() }
        }

        fun toggleFavorite(code: String) {
            viewModelScope.launch {
                if (state.value.favorites.contains(code)) {
                    repository.removeFavorite(code)
                } else {
                    repository.addFavorite(code)
                }
            }
        }

        fun refreshManually() {
            viewModelScope.launch { refresh() }
        }

        private suspend fun refresh() {
            _state.update { it.copy(isRefreshing = true, errorMessage = null) }
            runCatching { repository.refresh(state.value.base) }
                .onSuccess { _state.update { it.copy(isRefreshing = false) } }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage =
                                "Couldn't refresh rates. Showing cached values. (${e.message ?: "offline"})",
                        )
                    }
                }
        }

        /**
         * Merge a rates snapshot + favourites set into the UI state.
         *
         * Ordering rules baked in:
         *  - Favourites first (pinned), in the order they appear in
         *    the alphabetical code list.
         *  - Then the remaining codes alphabetically.
         *  - The current base is hidden from the list - converting USD
         *    to USD is always 1.00 and clutters the view.
         *
         * @param rates Latest cached snapshot or null when the cache is
         *   empty (e.g. before the first successful fetch).
         * @param favorites Codes the user has pinned.
         */
        private fun applySnapshot(rates: Rates?, favorites: Set<String>) {
            _state.update { current ->
                if (rates == null) {
                    // No rates yet, but we can still show pinned codes
                    // in the list so the user sees them as placeholders.
                    current.copy(
                        favorites = favorites,
                        visibleCodes = favorites.toList(),
                    )
                } else {
                    val codes = rates.rates.keys.sorted()
                    // partition() preserves the input ordering inside
                    // both buckets, so pinned currencies render in
                    // alphabetical order among themselves.
                    val (pinned, rest) = codes.partition { favorites.contains(it) }
                    val ordered = pinned + rest.filter { it != rates.base }
                    current.copy(
                        base = rates.base,
                        rates = rates.rates,
                        allCodes = codes,
                        visibleCodes = ordered,
                        favorites = favorites,
                        fetchedAtUtc = rates.fetchedAtUtc,
                    )
                }
            }
        }
    }
