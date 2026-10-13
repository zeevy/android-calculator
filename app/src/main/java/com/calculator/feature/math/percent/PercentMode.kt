package com.calculator.feature.math.percent

/** The three percentage questions, in the order the mode switch shows them. */
internal enum class PercentMode {
    /** X% of Y. */
    PercentOf,

    /** X of Y is what %. */
    WhatPercent,

    /** % change from A to B. */
    Change,
}
