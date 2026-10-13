package com.calculator.feature.basic.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * DTMF tone generator for the keypad.
 *
 * Maps digits to the standard phone touch-tones (so 7 sounds like 7
 * and 9 sounds like 9), and gives operators, equals, and modifier keys
 * their own tones so the audio cues differentiate by key category, not
 * just by pitch. Falls back silently if the system refuses to allocate
 * a [ToneGenerator] (some OEM devices restrict STREAM_DTMF).
 */
internal class KeyToneGenerator(private val tg: ToneGenerator) {
    fun play(key: Key) {
        val tone = toneFor(key) ?: return
        // 80ms is short enough that overlapping repeats don't smear into
        // one long buzz when a digit auto-repeats.
        runCatching { tg.startTone(tone, KEY_TONE_DURATION_MS) }
    }

    private fun toneFor(key: Key): Int? =
        when (key) {
            is Key.Symbol -> {
                val label = key.label
                when {
                    label.length == 1 && label[0].isDigit() ->
                        ToneGenerator.TONE_DTMF_0 + (label[0] - '0')
                    label == "." -> ToneGenerator.TONE_DTMF_P
                    else -> ToneGenerator.TONE_PROP_BEEP
                }
            }
            Key.Equals -> ToneGenerator.TONE_PROP_ACK
            Key.Backspace -> ToneGenerator.TONE_PROP_PROMPT
            Key.LeftParen, Key.RightParen -> ToneGenerator.TONE_PROP_BEEP
            Key.Clear -> ToneGenerator.TONE_PROP_NACK
            Key.MemoryClear, Key.MemoryRecall, Key.MemoryAdd, Key.MemorySubtract ->
                ToneGenerator.TONE_PROP_BEEP
            Key.SignFlip, Key.Factorial, Key.Squared, Key.Cubed -> ToneGenerator.TONE_PROP_BEEP
            Key.Empty -> null
            is Key.Function -> ToneGenerator.TONE_PROP_BEEP
        }

    fun release() {
        runCatching { tg.release() }
    }
}

private const val KEY_TONE_DURATION_MS = 80
private const val KEY_TONE_VOLUME = 80

/**
 * Constructs and remembers a [KeyToneGenerator] scoped to the current
 * composition. Released automatically on dispose. Returns null if the
 * device wouldn't give us a tone generator (we then play no audio at
 * all rather than crashing).
 */
@Composable
internal fun rememberKeyToneGenerator(): KeyToneGenerator? {
    val generator =
        remember {
            runCatching {
                ToneGenerator(AudioManager.STREAM_DTMF, KEY_TONE_VOLUME)
            }.getOrNull()
        }
    val keyTones = remember(generator) { generator?.let(::KeyToneGenerator) }
    DisposableEffect(keyTones) {
        onDispose { keyTones?.release() }
    }
    return keyTones
}

/** CompositionLocal so any key in the tree can play tones without prop drilling. */
internal val LocalKeyTones = compositionLocalOf<KeyToneGenerator?> { null }

/**
 * Whether key-press haptic feedback should fire. Defaults to true so
 * existing code paths and previews continue to vibrate.
 */
internal val LocalHapticsEnabled = compositionLocalOf { true }
