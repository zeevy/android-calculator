package com.calculator.feature.finance.salary

/** The salary questions, in the order the mode switch shows them. */
internal enum class SalaryMode {
    /** The new salary after a hike of a given percent. */
    NewSalary,

    /** The hike percent between an old and a new salary. */
    HikePercent,
}
