package com.music.spotui.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * One pointer handler that sorts a gesture into LEFT, RIGHT, UP or DOWN before anything acts on
 * it, so the player's horizontal track swipes cannot be mistaken for its vertical pull-to-close
 * (items C4/C5 vs C1/C2), and neither can be mistaken for a tap or for a slider drag.
 *
 * How it decides: nothing happens until the finger has travelled [touchSlop] from where it went
 * down. At that moment the axis with the larger displacement wins — a mostly-horizontal first move
 * is a horizontal gesture for the rest of the press, a mostly-vertical one stays vertical. That is
 * the same rule the platform uses to choose between a horizontal pager and a vertical list, and it
 * means a swipe that starts crooked still commits, while a straight-down pull can never skip a
 * track. Only the winning direction reports movement, so the handlers below never both fire.
 *
 * A press that never passes the slop (a tap, a long press on a control) reports nothing, which
 * leaves buttons, sliders and taps behaving exactly as they did before this modifier existed.
 */
enum class SwipeAxis { HORIZONTAL, VERTICAL }

/** What [swipeGestures] decided for one press, and how far the finger has moved since. */
class SwipeDrag internal constructor(
    val axis: SwipeAxis,
    /** Total displacement on the winning axis, in pixels, signed. */
    val total: Float,
) {
    val horizontal: Boolean get() = axis == SwipeAxis.HORIZONTAL
    val vertical: Boolean get() = axis == SwipeAxis.VERTICAL
}

/**
 * Attaches an axis-locked swipe detector.
 *
 * @param onMove called for every movement once the axis is decided, with the displacement on that
 *   axis. Return true when you have taken the gesture (normally always).
 * @param onEnd called once when the finger lifts, with the total displacement on the axis that won.
 *   [SwipeDrag.total] is what a release threshold is compared against.
 * @param onCancel called when the gesture is taken from us (another pointer, a scroll parent).
 * @param enabled false detaches the whole thing, so the caller can leave the area to something else.
 */
fun Modifier.swipeGestures(
    enabled: Boolean = true,
    slop: Float? = null,
    onMove: (SwipeDrag) -> Boolean,
    onEnd: (SwipeDrag) -> Unit,
    onCancel: () -> Unit = {},
): Modifier = composed {
    if (!enabled) return@composed this
    val density = LocalDensity.current
    val threshold = slop ?: with(density) { 12.dp.toPx() }
    // rememberUpdatedState: a new lambda from recomposition must not restart the gesture.
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onEnd)
    val cancel by rememberUpdatedState(onCancel)
    this.pointerInput(threshold) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var axis: SwipeAxis? = null
            var total = 0f
            var dx = 0f
            var dy = 0f
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    // Released: report the gesture if it ever committed to an axis.
                    if (axis != null) end(SwipeDrag(axis, total))
                    break
                }
                val delta = change.positionChange()
                dx += delta.x
                dy += delta.y
                if (axis == null) {
                    if (abs(dx) < threshold && abs(dy) < threshold) continue
                    axis = if (abs(dx) > abs(dy)) SwipeAxis.HORIZONTAL else SwipeAxis.VERTICAL
                }
                // Only the winning axis moves the gesture on: a wobbly horizontal swipe never
                // nudges the vertical handler, which is the whole point.
                val step = if (axis == SwipeAxis.HORIZONTAL) delta.x else delta.y
                if (step != 0f) {
                    total += step
                    if (move(SwipeDrag(axis, total))) change.consume()
                }
            }
        }
    }
}

/**
 * The mini player bar's own gestures, for the looks whose bar is not the Immersive capsule:
 * up opens the player, down dismisses it (the bar draws nothing once nothing is playing), left
 * goes to the next track and right to the previous, the same directions as the full player.
 * A tap is left to the bar's own click, which also opens the player.
 */
fun Modifier.miniPlayerTrackGestures(
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier = composed {
    val density = LocalDensity.current
    val commit = with(density) { 56.dp.toPx() }
    val open by rememberUpdatedState(onOpen)
    val dismiss by rememberUpdatedState(onDismiss)
    val left by rememberUpdatedState(onSwipeLeft)
    val right by rememberUpdatedState(onSwipeRight)
    Modifier.swipeGestures(
        slop = with(density) { 10.dp.toPx() },
        // The horizontal axis alone is consumed, so the row's own tap still works.
        onMove = { d -> d.horizontal },
        onEnd = { d ->
            when {
                d.horizontal -> if (abs(d.total) >= commit) { if (d.total < 0f) left() else right() }
                d.total <= -commit -> open()
                d.total >= commit -> dismiss()
            }
        },
    )
}

/**
 * How far a finger must travel before the app treats the movement as a deliberate swipe rather than a
 * wobble: 72 dp. The player's pull-down uses it as a dead zone (nothing moves below it), and
 * [swipePassed] uses it for the queue's own swipes.
 */
const val SWIPE_PASS_DP = 72f

/** True when [total] px of travel is far enough to count as a deliberate swipe ([dp] wide). */
fun swipePassed(total: Float, density: androidx.compose.ui.unit.Density, dp: Float = SWIPE_PASS_DP): Boolean =
    abs(total) >= with(density) { dp.dp.toPx() }

/**
 * A left/right pair over a whole area (the player). It shares [swipeGestures], so it can never
 * fire from a vertical pull, and it needs a real horizontal travel ([MIN_SWIPE_DP]) so a sideways
 * wobble during a pull is not a skip.
 *
 * The parameters name the GESTURE, not the track, because the two were being confused: the first
 * version mapped "swipe left" to the previous track, and that direction was reported as wrong.
 * Swiping LEFT now calls [onSwipeLeft] and the callers wire that to the next track; the mapping
 * lives at the call site where it can be read against the requirement.
 */
fun Modifier.horizontalTrackSwipes(
    enabled: Boolean = true,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier = composed {
    if (!enabled) return@composed this
    val density = LocalDensity.current
    val min = with(density) { MIN_SWIPE_DP.dp.toPx() }
    val left by rememberUpdatedState(onSwipeLeft)
    val right by rememberUpdatedState(onSwipeRight)
    Modifier.swipeGestures(
        enabled = true,
        // Take the horizontal axis only. Reaching for the vertical one too starved the player's
        // pull-down, which lives on its list's nested scroll and needs a drag nobody has consumed
        // (reported: pulling the player down did nothing).
        onMove = { d -> d.horizontal },
        onEnd = { d ->
            // A leftward drag is a negative displacement on x.
            if (d.horizontal && abs(d.total) >= min) {
                if (d.total < 0f) left() else right()
            }
        },
    )
}

private const val MIN_SWIPE_DP = 64f
