package com.calculator.feature.basic.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calculator.R
import com.calculator.core.common.clipboard.plainTextClipEntry
import com.calculator.core.data.tape.TapeEntry
import kotlinx.coroutines.launch

/**
 * Inline session history shown directly above the calculator display.
 *
 * One row per committed equation (`2 + 5 = 7`), oldest at the top and
 * newest at the bottom, so the list reads like a paper tape unspooling
 * downward. When the rows overflow the available height the column
 * scrolls, a slim thumb appears on the right edge, and the top edge
 * fades out to signal the lines continuing off-screen; a fresh `=`
 * always scrolls the newest row back into view.
 *
 * Rows hug the bottom of the region (`Arrangement.Bottom`) so a short
 * tape sits next to the display instead of floating at the top of an
 * empty gap.
 *
 * @param entries Tape entries in chronological order (oldest first).
 * @param onRecall Invoked with an entry's result when its row is tapped,
 *   mirroring the recall behaviour of the History sheet and Tape screen.
 * @param onDelete Invoked with an entry's id to drop that single line.
 */
@Composable
internal fun InlineTape(
    entries: List<TapeEntry>,
    onRecall: (String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Keyed on the newest id rather than on size: the tape is capped at
    // MAX_ENTRIES, so once it saturates the size stops changing while
    // new equations keep arriving.
    LaunchedEffect(entries.lastOrNull()?.id) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier =
            modifier
                .verticalScrollbar(listState, MaterialTheme.colorScheme.outline)
                .fadingTopEdge(listState),
        verticalArrangement = Arrangement.spacedBy(INLINE_TAPE_ROW_SPACING, Alignment.Bottom),
        contentPadding = PaddingValues(end = INLINE_TAPE_SCROLLBAR_WIDTH + 4.dp),
    ) {
        items(entries, key = { it.id }) { entry ->
            InlineTapeRow(
                entry = entry,
                onTap = { onRecall(entry.result) },
                onDelete = { onDelete(entry.id) },
            )
        }
    }
}

/**
 * Single tape line, right-aligned to sit under the display's own
 * right-aligned expression and result.
 *
 * Tap inserts the result. Long-press opens a two-item menu - copy the
 * whole `expression = result` equation (the same payload a long-press
 * on a History row copies), or drop this one line. A menu rather than a
 * bare long-press-to-copy because the tape needs both actions and the
 * rows are too short to swipe reliably.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InlineTapeRow(
    entry: TapeEntry,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val equation = "${entry.expression} = ${entry.result}"
    var menuOpen by remember { mutableStateOf(false) }

    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val copiedToast = stringResource(R.string.history_copied_toast)
    // Clipboard writes are suspend calls, so the menu item dispatches
    // into a composition-scoped coroutine.
    val clipboardScope = rememberCoroutineScope()

    Box {
        Text(
            text = equation,
            // titleLarge, not a body style: the tape is the thing the user
            // reads back while chaining calculations, and at 14sp it was
            // dwarfed by the display. Still clearly subordinate to the
            // result's displayLarge, and ~7 lines fit the region.
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            // Long equations lose their leading characters rather than
            // their trailing ones: the result at the end is the part
            // worth keeping.
            overflow = TextOverflow.StartEllipsis,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onTap,
                        onLongClick = { menuOpen = true },
                    ).padding(vertical = 2.dp),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tape_copy_line)) },
                onClick = {
                    menuOpen = false
                    clipboardScope.launch {
                        clipboard.setClipEntry(plainTextClipEntry(equation))
                        Toast.makeText(context, copiedToast, Toast.LENGTH_SHORT).show()
                    }
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tape_delete_line)) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

/**
 * Draws a slim scroll indicator along the trailing edge of a lazy list.
 *
 * Compose has no built-in scrollbar on Android, and the inline tape is
 * a small window onto a potentially long list, so the affordance has to
 * be explicit - otherwise there is nothing telling the user the older
 * rows are still up there.
 *
 * Thumb geometry is derived from the item *count* rather than from
 * pixel offsets. That is an approximation in general, but every tape
 * row is a single line of identical height, so it tracks exactly here
 * and costs nothing to compute.
 *
 * @param color Thumb colour; drawn at [INLINE_TAPE_SCROLLBAR_ALPHA] so
 *   it reads as a hint rather than a control.
 */
private fun Modifier.verticalScrollbar(
    state: LazyListState,
    color: Color,
    width: Dp = INLINE_TAPE_SCROLLBAR_WIDTH,
): Modifier =
    drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val visible = layoutInfo.visibleItemsInfo
        val total = layoutInfo.totalItemsCount
        // Nothing to indicate while the whole tape fits on screen.
        if (visible.isEmpty() || total <= visible.size) return@drawWithContent

        val trackHeight = size.height
        val thumbHeight =
            (trackHeight * visible.size / total).coerceAtLeast(width.toPx() * 3f)
        val scrolledFraction =
            visible.first().index.toFloat() / (total - visible.size).toFloat()
        val thumbTop = (trackHeight - thumbHeight) * scrolledFraction.coerceIn(0f, 1f)

        val thumbWidth = width.toPx()
        drawRoundRect(
            color = color,
            alpha = INLINE_TAPE_SCROLLBAR_ALPHA,
            topLeft = Offset(x = size.width - thumbWidth, y = thumbTop),
            size = Size(width = thumbWidth, height = thumbHeight),
            cornerRadius = CornerRadius(thumbWidth / 2f),
        )
    }

/**
 * Fades the content out towards the top edge once there are rows
 * scrolled off above, so the oldest visible line dissolves instead of
 * being chopped off mid-glyph against the chip row.
 *
 * Implemented as a `DstIn` alpha mask over an offscreen layer rather
 * than a gradient painted in the background colour, so it stays correct
 * whatever the surface underneath happens to be (light, dark, or a
 * dynamic-colour background).
 */
private fun Modifier.fadingTopEdge(
    state: LazyListState,
    height: Dp = INLINE_TAPE_FADE_HEIGHT,
): Modifier =
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            // Nothing above to hint at - leave the first row crisp.
            if (!state.canScrollBackward) return@drawWithContent
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = height.toPx(),
                    ),
                size = Size(width = size.width, height = height.toPx()),
                blendMode = BlendMode.DstIn,
            )
        }

private val INLINE_TAPE_ROW_SPACING = 4.dp
private val INLINE_TAPE_SCROLLBAR_WIDTH = 3.dp

// Roughly one titleLarge line, so the oldest visible row dissolves over
// its own height rather than fading mid-glyph.
private val INLINE_TAPE_FADE_HEIGHT = 36.dp
private const val INLINE_TAPE_SCROLLBAR_ALPHA = 0.5f
