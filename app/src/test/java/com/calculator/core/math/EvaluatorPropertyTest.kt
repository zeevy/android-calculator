package com.calculator.core.math

import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.math.BigDecimal
import java.math.MathContext

/**
 * Property-based tests for [Evaluator].
 *
 * Where the golden catalogue ([EvaluatorGoldenTest]) is hand-curated -
 * each row is a known case the author chose - these tests generate
 * hundreds of random inputs per property and assert a mathematical
 * identity holds across all of them. The point is to find the edge
 * cases nobody thought to add to the golden table: combinations of
 * sign, magnitude and precision the author wouldn't catalogue but
 * Math.tan / Math.pow might still mishandle.
 *
 * Conventions
 * -----------
 * - JUnit 5 (Jupiter) is the test engine; Kotest's `checkAll` is
 *   invoked inside each `@Test` method so we don't need the Kotest
 *   runner plugin.
 * - `checkAll` is suspend; we wrap in [runBlocking] because none of
 *   these properties need coroutine concurrency.
 * - Sample count defaults to 200 per property via [config]; cranking
 *   higher trades runtime for shrink fidelity.
 * - Identities involving Double-routed math (trig / log / sqrt /
 *   power) compare within [TOLERANCE] because the last 1-2 digits of
 *   Double precision aren't reliable; identities on exact arithmetic
 *   use BigDecimal.compareTo for bit-exact equality.
 * - Generators avoid the well-known asymptotes (tan near π/2 + kπ,
 *   log near 0, sqrt of negative) so the *property* under test is
 *   what fails - not the input being out of domain. Asymptote
 *   coverage lives in the golden catalogue.
 */
class EvaluatorPropertyTest {
    private val rad = Evaluator(angleMode = AngleMode.Radian)

    /**
     * 200 samples per property keeps the whole file under ~1s on JVM
     * while still surfacing the typical Math.* edge case. Bump to a
     * few thousand temporarily when hunting for a specific class of
     * bug; revert before committing so CI stays fast.
     */
    private val config = PropTestConfig(iterations = 200)

    /** Absolute tolerance for Double-precision identities. */
    private val tolerance = 1e-9

    // ------------------------------------------------------------------
    // Exact arithmetic identities - the engine routes these through
    // BigDecimal with no MathContext (since the recent exactness fix),
    // so results must be bit-exact.
    // ------------------------------------------------------------------

    @Test
    fun additionIsCommutative(): Unit = runBlocking {
        checkAll(config, smallDecimal(), smallDecimal()) { a, b ->
            val ab = rad.evaluate("$a+$b").expectExact()
            val ba = rad.evaluate("$b+$a").expectExact()
            assertExact(ab, ba, "a+b ≠ b+a for a=$a b=$b")
        }
    }

    @Test
    fun multiplicationIsCommutative(): Unit = runBlocking {
        checkAll(config, smallDecimal(), smallDecimal()) { a, b ->
            val ab = rad.evaluate("$a×$b").expectExact()
            val ba = rad.evaluate("$b×$a").expectExact()
            assertExact(ab, ba, "a×b ≠ b×a for a=$a b=$b")
        }
    }

    @Test
    fun additionIsAssociative(): Unit = runBlocking {
        checkAll(config, smallDecimal(), smallDecimal(), smallDecimal()) { a, b, c ->
            val left = rad.evaluate("($a+$b)+$c").expectExact()
            val right = rad.evaluate("$a+($b+$c)").expectExact()
            assertExact(left, right, "(a+b)+c ≠ a+(b+c) for a=$a b=$b c=$c")
        }
    }

    @Test
    fun multiplicationIsAssociative(): Unit = runBlocking {
        checkAll(config, smallDecimal(), smallDecimal(), smallDecimal()) { a, b, c ->
            val left = rad.evaluate("($a×$b)×$c").expectExact()
            val right = rad.evaluate("$a×($b×$c)").expectExact()
            assertExact(left, right, "(a×b)×c ≠ a×(b×c) for a=$a b=$b c=$c")
        }
    }

    @Test
    fun multiplicationDistributesOverAddition(): Unit = runBlocking {
        checkAll(config, smallDecimal(), smallDecimal(), smallDecimal()) { a, b, c ->
            val expanded = rad.evaluate("$a×$b+$a×$c").expectExact()
            val factored = rad.evaluate("$a×($b+$c)").expectExact()
            assertExact(
                expanded,
                factored,
                "a×b + a×c ≠ a×(b+c) for a=$a b=$b c=$c",
            )
        }
    }

    @Test
    fun additiveInverseHolds(): Unit = runBlocking {
        // x + (-x) = 0 for any x. Uses parenthesised unary minus.
        checkAll(config, smallDecimal()) { x ->
            val result = rad.evaluate("$x+(-($x))").expectExact()
            assertExact(result, BigDecimal.ZERO, "x + (-x) ≠ 0 for x=$x")
        }
    }

    @Test
    fun negationInvolutes(): Unit = runBlocking {
        // -(-x) = x. The tokenizer's `-1 ×` rewrite + a real negation
        // should cancel back to the input.
        checkAll(config, smallDecimal()) { x ->
            val result = rad.evaluate("-(-($x))").expectExact()
            assertExact(result, BigDecimal(x), "-(-(x)) ≠ x for x=$x")
        }
    }

    // ------------------------------------------------------------------
    // Transcendental identities - compared within [tolerance].
    // ------------------------------------------------------------------

    @Test
    fun pythagoreanIdentityHoldsInRadians(): Unit = runBlocking {
        // sin²(x) + cos²(x) = 1 for every x. Avoid the very-large
        // angles where Double range-reduction loses precision.
        checkAll(config, Arb.double(min = -10.0, max = 10.0)) { x ->
            val str = formatForExpression(x)
            val result = rad.evaluate("sin($str)^2+cos($str)^2").expectSuccessLocal()
            assertNear(result, BigDecimal.ONE, "sin²+cos² ≠ 1 for x=$str")
        }
    }

    @Test
    fun sinIsOddFunction(): Unit = runBlocking {
        // sin(-x) = -sin(x). Stays away from asymptotes since sin has none.
        checkAll(config, Arb.double(min = -10.0, max = 10.0)) { x ->
            val str = formatForExpression(x)
            val sinX = rad.evaluate("sin($str)").expectSuccessLocal()
            val sinNegX = rad.evaluate("sin(-($str))").expectSuccessLocal()
            assertNear(sinNegX, sinX.negate(), "sin(-x) ≠ -sin(x) for x=$str")
        }
    }

    @Test
    fun cosIsEvenFunction(): Unit = runBlocking {
        // cos(-x) = cos(x).
        checkAll(config, Arb.double(min = -10.0, max = 10.0)) { x ->
            val str = formatForExpression(x)
            val cosX = rad.evaluate("cos($str)").expectSuccessLocal()
            val cosNegX = rad.evaluate("cos(-($str))").expectSuccessLocal()
            assertNear(cosNegX, cosX, "cos(-x) ≠ cos(x) for x=$str")
        }
    }

    @Test
    fun asinRoundtripsForUnitInterval(): Unit = runBlocking {
        // sin(asin(x)) = x for x in [-1, 1].
        checkAll(config, Arb.double(min = -0.999, max = 0.999)) { x ->
            val str = formatForExpression(x)
            val result = rad.evaluate("sin(asin($str))").expectSuccessLocal()
            assertNear(result, BigDecimal(x), "sin(asin(x)) ≠ x for x=$str")
        }
    }

    @Test
    fun acosRoundtripsForUnitInterval(): Unit = runBlocking {
        // cos(acos(x)) = x for x in [-1, 1].
        checkAll(config, Arb.double(min = -0.999, max = 0.999)) { x ->
            val str = formatForExpression(x)
            val result = rad.evaluate("cos(acos($str))").expectSuccessLocal()
            assertNear(result, BigDecimal(x), "cos(acos(x)) ≠ x for x=$str")
        }
    }

    @Test
    fun atanRoundtripsForAllReals(): Unit = runBlocking {
        // tan(atan(x)) = x. Avoid the magnitudes where atan saturates
        // numerically near ±π/2 (e.g. x = 1e15 round-trips, but the
        // last few digits depend on the tan/atan implementation chain).
        checkAll(config, Arb.double(min = -1000.0, max = 1000.0)) { x ->
            val str = formatForExpression(x)
            val result = rad.evaluate("tan(atan($str))").expectSuccessLocal()
            // Tolerance scaled to magnitude: tan steepens fast, so for
            // large x the round-trip naturally accumulates more error.
            assertNear(
                result,
                BigDecimal(x),
                "tan(atan(x)) ≠ x for x=$str",
                tolerance = 1e-6 * (1.0 + kotlin.math.abs(x)),
            )
        }
    }

    @Test
    fun sqrtSquaredRecoversInput(): Unit = runBlocking {
        // sqrt(x)² = x for x ≥ 0.
        checkAll(config, Arb.double(min = 0.0, max = 1_000_000.0)) { x ->
            val str = formatForExpression(x)
            val result = rad.evaluate("sqrt($str)^2").expectSuccessLocal()
            assertNear(
                result,
                BigDecimal(x),
                "sqrt(x)² ≠ x for x=$str",
                // tolerance scaled to magnitude because Double sqrt
                // loses ~1 ulp per operation and squaring doubles that.
                tolerance = 1e-9 * (1.0 + x),
            )
        }
    }

    @Test
    fun cbrtCubedRecoversInput(): Unit = runBlocking {
        checkAll(config, Arb.double(min = -1000.0, max = 1000.0)) { x ->
            val str = formatForExpression(x)
            val result = rad.evaluate("cbrt($str)^3").expectSuccessLocal()
            assertNear(
                result,
                BigDecimal(x),
                "cbrt(x)³ ≠ x for x=$str",
                tolerance = 1e-9 * (1.0 + kotlin.math.abs(x)),
            )
        }
    }

    @Test
    fun logExpRoundtripBase10() = runBlocking {
        // log(10^x) = x for any x (within Math.pow's representable range).
        checkAll(config, Arb.double(min = -50.0, max = 50.0)) { x ->
            val str = formatForExpression(x)
            val result = rad.evaluate("log(10^($str))").expectSuccessLocal()
            assertNear(result, BigDecimal(x), "log(10^x) ≠ x for x=$str")
        }
    }

    @Test
    fun lnExpRoundtripBaseE(): Unit = runBlocking {
        // ln(e^x) = x. Smaller range than log10 because e is closer to 1.
        checkAll(config, Arb.double(min = -50.0, max = 50.0)) { x ->
            val str = formatForExpression(x)
            val result = rad.evaluate("ln(e^($str))").expectSuccessLocal()
            assertNear(result, BigDecimal(x), "ln(e^x) ≠ x for x=$str")
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Generator of small decimals suitable for exact arithmetic
     * identities. Range and scale are chosen so the expression strings
     * stay readable in failure messages and so multiplied/squared
     * results stay inside a few dozen digits.
     */
    private fun smallDecimal(): Arb<String> {
        return Arb.double(min = -1000.0, max = 1000.0).map { d ->
            // Round to 4 decimal places so the literal we feed back into
            // the engine is exact (BigDecimal(String) is exact;
            // BigDecimal(Double) carries the binary-float wobble).
            BigDecimal(d).setScale(4, java.math.RoundingMode.HALF_EVEN).toPlainString()
        }
    }

    /**
     * Format a Double for embedding in an evaluator expression string.
     * Uses 12 significant digits so the literal we feed back in is
     * faithful to the generated value but stays bounded in length.
     */
    private fun formatForExpression(x: Double): String {
        // BigDecimal(Double, MathContext) avoids the binary-fraction
        // expansion that BigDecimal(Double) alone would produce.
        return BigDecimal(x, MathContext(12)).toPlainString()
    }

    private fun EvaluationResult.expectExact(): BigDecimal {
        return when (this) {
            is EvaluationResult.Success -> value
            else -> fail("expected Success but got $this")
        }
    }

    private fun EvaluationResult.expectSuccessLocal(): BigDecimal {
        return when (this) {
            is EvaluationResult.Success -> value
            else -> fail("expected Success but got $this")
        }
    }

    private fun assertExact(a: BigDecimal, b: BigDecimal, message: String) {
        if (a.compareTo(b) != 0) fail("$message ($a vs $b)")
    }

    private fun assertNear(
        actual: BigDecimal,
        expected: BigDecimal,
        message: String,
        tolerance: Double = this.tolerance,
    ) {
        val diff = (actual - expected).abs().toDouble()
        if (diff > tolerance) {
            fail("$message: |$actual - $expected| = $diff > $tolerance")
        }
    }
}

