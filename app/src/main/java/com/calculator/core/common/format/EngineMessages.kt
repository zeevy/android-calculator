package com.calculator.core.common.format

import androidx.annotation.StringRes
import com.calculator.R
import com.calculator.core.math.DomainKind
import com.calculator.core.math.EvaluationResult
import com.calculator.core.math.FunctionId

/**
 * The message shown for an engine error, as a string resource, so the
 * calculator, the floating calculator and any future screen say the same
 * thing in every language. Messages name the rule the user bumped into,
 * not the engine's technical reason.
 */
@StringRes
fun EvaluationResult.Error.messageRes(): Int =
    when (this) {
        EvaluationResult.Error.DivisionByZero -> R.string.basic_error_division_by_zero
        is EvaluationResult.Error.Syntax -> R.string.basic_error_syntax
        is EvaluationResult.Error.UnknownToken -> R.string.basic_error_unknown_token
        is EvaluationResult.Error.Domain ->
            when (kind) {
                DomainKind.Function -> functionMessage(function)
                DomainKind.FactorialOfNegative -> R.string.basic_error_factorial_negative
                DomainKind.FactorialOfNonInteger -> R.string.basic_error_factorial_whole
                DomainKind.FactorialTooLarge -> R.string.basic_error_factorial_large
                DomainKind.PowerOverflow -> R.string.basic_error_too_large
                DomainKind.InvalidPower -> R.string.basic_error_power
            }
    }

@StringRes
private fun functionMessage(function: FunctionId?): Int =
    when (function) {
        FunctionId.Sqrt -> R.string.basic_error_sqrt
        FunctionId.Cbrt -> R.string.basic_error_cbrt
        FunctionId.Log -> R.string.basic_error_log
        FunctionId.Ln -> R.string.basic_error_ln
        FunctionId.Asin -> R.string.basic_error_asin
        FunctionId.Acos -> R.string.basic_error_acos
        FunctionId.Tan -> R.string.basic_error_tan
        else -> R.string.basic_error_domain
    }
