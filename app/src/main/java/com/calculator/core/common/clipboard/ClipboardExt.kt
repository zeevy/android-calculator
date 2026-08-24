package com.calculator.core.common.clipboard

import android.content.ClipData
import android.content.Context
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
 * Read the first clipboard item as plain text, returning an empty string when
 * the clipboard is empty or holds something that has no text representation.
 *
 * [context] is needed because non-text items (a URI, for instance) are coerced
 * to text through a ContentResolver lookup.
 */
fun ClipEntry?.readPlainText(context: Context): String {
    val data = this?.clipData ?: return ""
    if (data.itemCount == 0) return ""
    return data
        .getItemAt(0)
        .coerceToText(context)
        ?.toString()
        .orEmpty()
}
