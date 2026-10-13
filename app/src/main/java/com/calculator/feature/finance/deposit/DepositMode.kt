package com.calculator.feature.finance.deposit

/** The deposit types, in the order the mode switch shows them. */
internal enum class DepositMode {
    /** Fixed deposit: one amount for a fixed tenure. */
    Fd,

    /** Recurring deposit: the same amount every month. */
    Rd,

    /** Public Provident Fund: a yearly deposit for 15 years. */
    Ppf,
}
