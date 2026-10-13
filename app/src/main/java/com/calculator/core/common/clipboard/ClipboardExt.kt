package com.calculator.core.common.clipboard

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry

/**
 * Clipboard label attached to everything the app copies. Android surfaces it
 * in the paste UI on some OEM skins, so it reads as a human name, not an id.
 */
private const val CLIPBOARD_LABEL = "Calculator"

/**
 * Wrap [text] in a plain-text [ClipEntry] ready for `Clipboard.setClipEntry`.
 */
fun plainTextClipEntry(text: String): ClipEntry = ClipData.newPlainText(CLIPBOARD_LABEL, text).toClipEntry()

/**
 * Read the first clipboard item's text, cut to [MAX_PASTE_CHARS], or an
 * empty string when there is none.
 *
 * Only the item's own text is read. A clip that is a content URI is not
 * opened (that would read a file of any size on the caller's thread), and
 * a long text is cut before anyone scans it for a number.
 */
fun ClipEntry?.readPlainText(): String {
    val data = this?.clipData ?: return ""
    if (data.itemCount == 0) return ""
    return data
        .getItemAt(0)
        .text
        ?.take(MAX_PASTE_CHARS)
        ?.toString()
        .orEmpty()
}

/** Longest pasted text looked at; a number never needs more. */
private const val MAX_PASTE_CHARS = 200
