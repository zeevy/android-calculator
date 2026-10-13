package com.calculator.feature.finance.deposit

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.common.input.InputResult
import com.calculator.core.designsystem.component.LifeCalcCard
import com.calculator.core.designsystem.component.LifeCalcNumberField
import com.calculator.core.designsystem.component.LifeCalcOutputRow
import com.calculator.core.designsystem.component.LifeCalcSectionLabel
import com.calculator.core.designsystem.component.LifeCalcSegmented
import com.calculator.core.designsystem.component.LifeCalculatorScaffold
import com.calculator.core.designsystem.component.errorFor
import com.calculator.core.domain.finance.DepositCalculator
import com.calculator.core.domain.finance.DepositResult
import com.calculator.core.domain.finance.PpfResult
import com.calculator.core.domain.finance.PpfYear

/**
 * Maturity calculator for the three common Indian deposits: FD, RD and
 * PPF. The switch at the top picks the deposit; each one keeps its own
 * typed values, so switching back and forth loses nothing.
 *
 * The math follows the banks and the PPF scheme rules (see
 * [DepositCalculator]), and each result card says which rule it used.
 * The wording stays neutral: this is a calculator, not an offer.
 *
 * @param onOpenMenu Opens the app's tools menu.
 */
@Composable
fun DepositScreen(onOpenMenu: () -> Unit) {
    // Saveable, so the mode and typed values survive the app being
    // killed in the background.
    var mode by rememberSaveable { mutableStateOf(DepositMode.Fd) }
    var fd by rememberSaveable { mutableStateOf(FdFields()) }
    var rd by rememberSaveable { mutableStateOf(RdFields()) }
    var ppf by rememberSaveable { mutableStateOf(PpfFields()) }
    val modeSwitch: @Composable () -> Unit = { DepositModeSwitch(mode) { mode = it } }

    LifeCalculatorScaffold(
        title = stringResource(R.string.deposit_title),
        onOpenMenu = onOpenMenu,
    ) {
        when (mode) {
            DepositMode.Fd -> FdSection(fd, onChange = { fd = it }, header = modeSwitch)
            DepositMode.Rd -> RdSection(rd, onChange = { rd = it }, header = modeSwitch)
            DepositMode.Ppf -> PpfSection(ppf, onChange = { ppf = it }, header = modeSwitch)
        }
    }
}

@Composable
private fun DepositModeSwitch(
    mode: DepositMode,
    onSelect: (DepositMode) -> Unit,
) {
    LifeCalcSectionLabel(stringResource(R.string.deposit_section_type))
    LifeCalcSegmented(
        options = listOf(
            stringResource(R.string.deposit_mode_fd),
            stringResource(R.string.deposit_mode_rd),
            stringResource(R.string.deposit_mode_ppf),
        ),
        selectedIndex = mode.ordinal,
        onSelect = { onSelect(DepositMode.entries[it]) },
    )
}

@Composable
private fun FdSection(
    fields: FdFields,
    onChange: (FdFields) -> Unit,
    header: @Composable () -> Unit,
) {
    val input = remember(fields) { fdInput(fields) }
    LifeCalcCard {
        header()
        LifeCalcNumberField(
            label = stringResource(R.string.deposit_amount),
            value = fields.amount,
            onValueChange = { onChange(fields.copy(amount = it)) },
            error = input.errorFor(R.string.deposit_amount),
        )
        RateField(fields.rate, input) { onChange(fields.copy(rate = it)) }
        LifeCalcNumberField(
            label = stringResource(R.string.deposit_years),
            value = fields.years,
            onValueChange = { onChange(fields.copy(years = it)) },
            suffix = stringResource(R.string.deposit_years_suffix),
            error = input.errorFor(R.string.deposit_years),
        )
        LifeCalcNumberField(
            label = stringResource(R.string.deposit_months),
            value = fields.months,
            onValueChange = { onChange(fields.copy(months = it)) },
            suffix = stringResource(R.string.deposit_months_suffix),
            error = input.errorFor(R.string.deposit_months),
        )
        LifeCalcSectionLabel(stringResource(R.string.deposit_section_compounding))
        LifeCalcSegmented(
            options = FdCompounding.entries.map { stringResource(it.label) },
            selectedIndex = fields.compounding.ordinal,
            onSelect = { onChange(fields.copy(compounding = FdCompounding.entries[it])) },
        )
    }
    ResultCard(input, note = R.string.deposit_fd_note) { DepositRows(it, showDeposited = false) }
}

@Composable
private fun RdSection(
    fields: RdFields,
    onChange: (RdFields) -> Unit,
    header: @Composable () -> Unit,
) {
    val input = remember(fields) { rdInput(fields) }
    LifeCalcCard {
        header()
        LifeCalcNumberField(
            label = stringResource(R.string.deposit_monthly),
            value = fields.amount,
            onValueChange = { onChange(fields.copy(amount = it)) },
            error = input.errorFor(R.string.deposit_monthly),
        )
        RateField(fields.rate, input) { onChange(fields.copy(rate = it)) }
        LifeCalcNumberField(
            label = stringResource(R.string.deposit_tenure),
            value = fields.months,
            onValueChange = { onChange(fields.copy(months = it)) },
            suffix = stringResource(R.string.deposit_months_suffix),
            error = input.errorFor(R.string.deposit_tenure),
        )
    }
    ResultCard(input, note = R.string.deposit_rd_note) { DepositRows(it, showDeposited = true) }
}

@Composable
private fun PpfSection(
    fields: PpfFields,
    onChange: (PpfFields) -> Unit,
    header: @Composable () -> Unit,
) {
    val input = remember(fields) { ppfInput(fields) }
    LifeCalcCard {
        header()
        LifeCalcNumberField(
            label = stringResource(R.string.deposit_yearly),
            value = fields.amount,
            onValueChange = { onChange(fields.copy(amount = it)) },
            error = input.errorFor(R.string.deposit_yearly),
        )
        RateField(fields.rate, input) { onChange(fields.copy(rate = it)) }
    }
    ResultCard(input, note = R.string.deposit_ppf_note) { PpfRows(it) }
    if (input is InputResult.Ok) PpfTable(input.value.years)
}

@Composable
private fun RateField(
    rate: String,
    input: InputResult<*>,
    onValueChange: (String) -> Unit,
) {
    LifeCalcNumberField(
        label = stringResource(R.string.deposit_rate),
        value = rate,
        onValueChange = onValueChange,
        suffix = "%",
        error = input.errorFor(R.string.deposit_rate),
    )
}

@Composable
private fun <T> ResultCard(
    input: InputResult<T>,
    @StringRes note: Int,
    rows: @Composable (T) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    LifeCalcCard {
        LifeCalcSectionLabel(stringResource(R.string.deposit_section_result))
        when (input) {
            is InputResult.Invalid ->
                Text(
                    text = stringResource(R.string.deposit_error_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                )
            is InputResult.Ok -> rows(input.value)
        }
        Text(
            text = stringResource(note),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DepositRows(
    result: DepositResult,
    showDeposited: Boolean,
) {
    if (showDeposited) {
        LifeCalcOutputRow(
            label = stringResource(R.string.deposit_deposited),
            value = NumberFormatter.money(result.deposited),
        )
    }
    LifeCalcOutputRow(
        label = stringResource(R.string.deposit_interest),
        value = NumberFormatter.money(result.interest),
    )
    LifeCalcOutputRow(
        label = stringResource(R.string.deposit_maturity),
        value = NumberFormatter.money(result.maturity),
        accent = true,
    )
}

@Composable
private fun PpfRows(result: PpfResult) {
    LifeCalcOutputRow(
        label = stringResource(R.string.deposit_tenure),
        value = stringResource(R.string.deposit_ppf_years, DepositCalculator.PPF_YEARS),
    )
    LifeCalcOutputRow(
        label = stringResource(R.string.deposit_deposited),
        value = NumberFormatter.money(result.deposited),
    )
    LifeCalcOutputRow(
        label = stringResource(R.string.deposit_interest),
        value = NumberFormatter.money(result.interest),
    )
    LifeCalcOutputRow(
        label = stringResource(R.string.deposit_maturity),
        value = NumberFormatter.money(result.maturity),
        accent = true,
    )
}

@Composable
private fun PpfTable(years: List<PpfYear>) {
    LifeCalcCard {
        LifeCalcSectionLabel(stringResource(R.string.deposit_section_years))
        PpfTableRow(
            cells = listOf(
                stringResource(R.string.deposit_col_year),
                stringResource(R.string.deposit_col_deposit),
                stringResource(R.string.deposit_col_interest),
                stringResource(R.string.deposit_col_balance),
            ),
            header = true,
        )
        years.forEach { year ->
            PpfTableRow(
                cells = listOf(year.year.toString(), rupees(year.deposit), rupees(year.interest), rupees(year.closing)),
                header = false,
            )
        }
    }
}

@Composable
private fun PpfTableRow(
    cells: List<String>,
    header: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    // One node per row, so TalkBack reads a year's figures together.
    Row(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        cells.forEachIndexed { index, cell ->
            Text(
                text = cell,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                color = if (header) scheme.onSurfaceVariant else scheme.onSurface,
                textAlign = if (index == 0) TextAlign.Start else TextAlign.End,
                modifier = Modifier.weight(if (index == 0) YEAR_COLUMN_WEIGHT else 1f),
            )
        }
    }
}

// Whole rupees keep four columns readable on a narrow phone; the result
// card above shows the exact totals.
private fun rupees(value: Double): String = NumberFormatter.format(value, maxFractionDigits = 0)

private const val YEAR_COLUMN_WEIGHT = 0.5f
