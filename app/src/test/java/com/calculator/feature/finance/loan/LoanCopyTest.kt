package com.calculator.feature.finance.loan

import org.junit.jupiter.api.Test
import java.io.File

/**
 * Phase 11 / Play-policy guard.
 *
 * The Loan screen is an **estimator** - Play's personal-loans policy
 * bans wording that would imply the app is a lender or originates
 * loans. This test grep-checks the screen source for the forbidden
 * words and fails the build if any slip in.
 *
 * If a real loan-related word genuinely needs to appear (e.g. the
 * disclaimer that says "not a lending tool"), include it in the
 * [Allowlist] set below, with a comment explaining why.
 */
class LoanCopyTest {
    private val sourceRoot =
        File("src/main/java/com/calculator/feature/finance/loan/LoanScreen.kt")

    private val forbidden =
        listOf(
            "apply",
            "qualify",
            "lender",
            "borrow",
            "loan us",
            "get a loan",
        )

    /** Substrings of allowed occurrences. None today. */
    private val allowlist = emptyList<String>()

    @Test
    fun `loan screen copy has no Play-policy-blocked wording`() {
        require(sourceRoot.exists()) { "Loan screen source not found at $sourceRoot" }
        // 1. Strip comments. The Loan screen's own KDoc explains why
        //    the policy exists and intentionally names the forbidden
        //    words; we don't want to flag the warning copy itself.
        // 2. Pull double-quoted string literals from the rest. That's
        //    what the user actually reads; Kotlin code like `.apply {}`
        //    is irrelevant to Play's loan-copy policy.
        val blockComment = Regex("/\\*[\\s\\S]*?\\*/")
        val lineComment = Regex("//[^\\n]*")
        val stringLiteralRegex = Regex("\"([^\"\\n\\\\]|\\\\.)*\"")
        val codeOnly =
            sourceRoot
                .readText()
                .replace(blockComment, "")
                .replace(lineComment, "")
        val userVisibleText =
            stringLiteralRegex
                .findAll(codeOnly)
                .map { it.value.lowercase() }
                .joinToString("\n")
        val hits =
            forbidden.filter { needle ->
                userVisibleText.contains(needle) &&
                    allowlist.none { allow -> userVisibleText.contains(allow.lowercase()) }
            }
        check(hits.isEmpty()) {
            "Loan screen contains banned wording in a user-visible string: $hits. " +
                "Play's personal-loans policy bans words that imply lending/origination."
        }
    }
}
