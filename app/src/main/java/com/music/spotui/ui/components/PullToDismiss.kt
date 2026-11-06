package com.music.spotui.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A full-screen sheet pulled down from the top of its list closes, like the official
 * player: the sheet follows the finger (with some resistance) and closes as soon as the finger
 * lifts after a downward pull — however short or long that pull was, with no distance and no
 * speed to reach. Attach [connection] to the list's `nestedScroll` and move the sheet by [offset].
 *
 * A pull only begins once the list under the finger can no longer scroll down, so a scrolling
 * page keeps its own drags to itself: this is the "anywhere but the scrolling parts" rule.
 */
class PullToDismissState internal constructor(
    private val scope: CoroutineScope,
    /** Finger travel, in px, that moves nothing at all before the sheet starts to follow. */
    private val deadPx: Float,
    private val dismiss: () -> Unit,
) {
    var enabled = true
    private val pull = Animatable(0f)
    private var raw = 0f
    private var dismissed = false

    /** How far the sheet is pulled down, in pixels. */
    val offset: Float get() = pull.value

    private fun follow() {
        // The dead zone comes off first: below [deadPx] the sheet does not move at all, and past it
        // the sheet starts from zero and follows the finger, so the crossing cannot jump.
        scope.launch { pull.snapTo((raw - deadPx).coerceAtLeast(0f) * RESISTANCE) }
    }

    val connection = object : NestedScrollConnection {
        // Pushing back up first undoes the pull, before the list scrolls.
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (raw <= 0f || available.y >= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
            val used = maxOf(available.y, -raw)
            raw += used
            follow()
            return Offset(0f, used)
        }

        // What the list could not scroll (it is at its top) pulls the sheet down.
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (!enabled || dismissed || available.y <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
            raw += available.y
            follow()
            return Offset(0f, available.y)
        }

        // Released: close only if the finger got past the dead zone. A shorter pull is nothing at
        // all — the sheet never left zero, so nothing has to spring back. Speed is not a shortcut.
        // (The pull cannot start until the list above has nothing left to scroll, so a scrolling
        // page is never closed by its own drag.)
        override suspend fun onPreFling(available: Velocity): Velocity {
            if (raw <= deadPx) {
                // A short pull: the sheet never moved, and the count starts over, so the next pull
                // needs the whole dead zone again.
                raw = 0f
                pull.snapTo(0f)
                return Velocity.Zero
            }
            dismissed = true
            dismiss()
            return available
        }
    }

    private companion object {
        const val RESISTANCE = 0.6f
    }
}

@Composable
fun rememberPullToDismiss(onDismiss: () -> Unit): PullToDismissState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val latest = rememberUpdatedState(onDismiss)
    val deadPx = with(density) { SWIPE_PASS_DP.dp.toPx() }
    return remember(deadPx) {
        PullToDismissState(scope, deadPx) { latest.value() }
    }
}
