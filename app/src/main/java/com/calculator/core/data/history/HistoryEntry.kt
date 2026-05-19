package com.calculator.core.data.history

/**
 * Domain-level history entry. Mirrors the Room entity but lives in the
 * data package so feature/UI code doesn't import Room types directly.
 */
data class HistoryEntry(
    val id: Long,
    val expression: String,
    val result: String,
    val timestampUtc: Long,
    val type: Type,
) {
    enum class Type { Basic, Scientific }
}
