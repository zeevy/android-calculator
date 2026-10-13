package com.calculator.feature.finance.loan

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Play-policy guard for the loan estimator's wording.
 *
 * Play's personal-loans policy bans wording that implies the app is a
 * lender or originates loans. All user-visible loan text lives in the
 * string resources, so this test reads every `strings.xml` (English and
 * every translation) and checks the loan strings, the loan tool label
 * and the launcher shortcut labels against the banned words.
 *
 * If a banned word must appear (for example in a disclaimer that says
 * what the tool is not), add the exact phrase to [allowlist] with a
 * reason.
 */
class LoanCopyTest {
    private val resRoot = File("src/main/res")

    /** Phrases allowed even though they contain a banned word. None today. */
    private val allowlist = emptyList<String>()

    @Test
    fun `loan strings have no Play-policy-blocked wording`() {
        val files = resRoot
            .listFiles { f -> f.isDirectory && f.name.startsWith("values") }
            .orEmpty()
            .map { File(it, "strings.xml") }
            .filter { it.exists() }
        assertTrue(files.size >= 2) { "expected English and Hindi strings under $resRoot" }

        val checked = files.flatMap { file -> loanStrings(file.readText()).map { (key, text) -> "${file.parentFile.name}/$key" to text } }
        assertTrue(checked.any { it.first.endsWith("/loan_title") }) { "loan strings not found" }

        assertEquals(emptyList<String>(), bannedHits(checked, allowlist))
    }

    @Test
    fun `the guard catches a banned word`() {
        val bad = loanStrings("""<resources><string name="loan_cta">Apply for a loan today</string></resources>""")
        assertEquals(listOf("loan_cta: apply"), bannedHits(bad, allowlist))
    }

    private companion object {
        val BANNED = listOf("apply", "qualify", "lender", "borrow", "loan us", "get a loan", "eligib", "approval")

        private val StringEntry = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

        /** Loan strings, the loan tool label and shortcut labels, as (key, text). */
        fun loanStrings(xml: String): List<Pair<String, String>> =
            StringEntry
                .findAll(xml)
                .map { it.groupValues[1] to it.groupValues[2] }
                .filter { (key, _) -> key.startsWith("loan_") || key == "tool_loan" || key.startsWith("shortcut_") }
                .toList()

        /** "key: word" for every banned word found outside the allowlist. */
        fun bannedHits(
            strings: List<Pair<String, String>>,
            allowlist: List<String>,
        ): List<String> =
            strings.flatMap { (key, text) ->
                val lower = allowlist.fold(text.lowercase()) { acc, allowed -> acc.replace(allowed.lowercase(), "") }
                BANNED.filter { it in lower }.map { "$key: $it" }
            }
    }
}
