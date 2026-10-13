package com.calculator.feature.finance.investment

/** The investment questions, in the order the mode switch shows them. */
internal enum class InvestmentMode {
    /** A fixed amount every month. */
    Sip,

    /** One amount, invested once. */
    LumpSum,

    /** The yearly growth rate between a start value and an end value. */
    Cagr,
}
