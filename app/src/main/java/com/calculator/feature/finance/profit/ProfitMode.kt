package com.calculator.feature.finance.profit

/** What the profit screen is given besides the cost price, in the order the mode switch shows them. */
internal enum class ProfitMode {
    /** The selling price. */
    Prices,

    /** A markup on the cost price. */
    Markup,

    /** A margin on the selling price. */
    Margin,
}
