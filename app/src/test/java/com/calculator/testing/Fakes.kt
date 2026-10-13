package com.calculator.testing

import com.calculator.core.data.history.HistoryEntry
import com.calculator.core.data.history.HistoryRepository
import com.calculator.core.data.settings.SettingsRepository
import com.calculator.core.data.settings.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/** In-memory [SettingsRepository]; every setter updates [snapshot]. */
class FakeSettingsRepository(
    initial: UserSettings = UserSettings(),
) : SettingsRepository {
    val snapshot = MutableStateFlow(initial)

    override val settings: Flow<UserSettings> = snapshot.asStateFlow()

    override suspend fun setTheme(theme: UserSettings.ThemeOption) = edit { copy(theme = theme) }

    override suspend fun setDynamicColor(enabled: Boolean) = edit { copy(dynamicColor = enabled) }

    override suspend fun setHaptics(enabled: Boolean) = edit { copy(haptics = enabled) }

    override suspend fun setSound(enabled: Boolean) = edit { copy(sound = enabled) }

    override suspend fun setPrecision(precision: Int) = edit { copy(precision = precision) }

    override suspend fun setLastUnitCategory(category: String) = edit { copy(lastUnitCategory = category) }

    override suspend fun setGstIntraState(intra: Boolean) = edit { copy(gstIntraState = intra) }

    override suspend fun setGstRate(rate: String) = edit { copy(gstRate = rate) }

    override suspend fun setBmiHeightImperial(imperial: Boolean) = edit { copy(bmiHeightImperial = imperial) }

    override suspend fun setBmiWeightImperial(imperial: Boolean) = edit { copy(bmiWeightImperial = imperial) }

    override suspend fun setLastTzPair(
        from: String,
        to: String,
    ) = edit { copy(lastTzFromZone = from, lastTzToZone = to) }

    private fun edit(change: UserSettings.() -> UserSettings) {
        snapshot.value = snapshot.value.change()
    }
}

/**
 * In-memory [HistoryRepository]. [observe] emits whatever is set on
 * [entries]; [add], [delete] and [clearAll] calls are recorded.
 */
class FakeHistoryRepository : HistoryRepository {
    /** One recorded [add] call. */
    data class AddCall(
        val expression: String,
        val result: String,
        val type: HistoryEntry.Type,
    )

    val added = mutableListOf<AddCall>()
    val deleted = mutableListOf<Long>()
    var clearAllCalls = 0
        private set
    val entries = MutableStateFlow<List<HistoryEntry>>(emptyList())

    override fun observe(): Flow<List<HistoryEntry>> = entries.asStateFlow()

    override suspend fun add(
        expression: String,
        result: String,
        type: HistoryEntry.Type,
    ) {
        added += AddCall(expression, result, type)
    }

    override suspend fun delete(id: Long) {
        deleted += id
    }

    override suspend fun clearAll() {
        clearAllCalls++
    }
}

/** JUnit 5 extension that swaps `Dispatchers.Main` for [dispatcher] around each test. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : BeforeEachCallback,
    AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) = Dispatchers.setMain(dispatcher)

    override fun afterEach(context: ExtensionContext) = Dispatchers.resetMain()
}
