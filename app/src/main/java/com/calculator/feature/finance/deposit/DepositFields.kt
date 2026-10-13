package com.calculator.feature.finance.deposit

import androidx.annotation.StringRes
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import java.io.Serializable

/**
 * How often an FD adds its interest to the balance, in the order the
 * switch shows them.
 *
 * @property timesPerYear Compounding periods in a year.
 * @property label Text on the switch.
 */
internal enum class FdCompounding(
    val timesPerYear: Int,
    @StringRes val label: Int,
) {
    Monthly(12, R.string.deposit_compounding_monthly),
    Quarterly(4, R.string.deposit_compounding_quarterly),
    HalfYearly(2, R.string.deposit_compounding_half_yearly),
    Yearly(1, R.string.deposit_compounding_yearly),
}

// The field groups are Serializable so rememberSaveable can keep them in
// the saved state Bundle without a custom Saver.

/** FD fields as typed. Defaults give a result on first open. */
internal data class FdFields(
    val amount: String = "100000",
    val rate: String = "7",
    val years: String = "5",
    val months: String = "0",
    val compounding: FdCompounding = FdCompounding.Quarterly,
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

/** RD fields as typed. Defaults give a result on first open. */
internal data class RdFields(
    val amount: String = "1000",
    val rate: String = "7",
    val months: String = "12",
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * PPF fields as typed. The rate default is only a starting value; the
 * government sets the PPF rate every quarter. It is written with the
 * locale's decimal separator, so it reads back as a number in German too.
 */
internal data class PpfFields(
    val amount: String = "150000",
    val rate: String = NumberFormatter.formatForInput(PPF_STARTING_RATE),
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

// The PPF rate since April 2020, unchanged for October to December 2026.
private const val PPF_STARTING_RATE = 7.1
