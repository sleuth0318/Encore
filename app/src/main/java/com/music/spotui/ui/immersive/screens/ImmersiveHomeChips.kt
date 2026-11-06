/*
 * Adapted from BitChord (https://github.com/kushagrasinghx/BitChord), GPL-3.0-or-later,
 * sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/SearchScreen.kt (SearchFilterTabs) at edc1e6f.
 * Modified for Myspoti, 2026-10.
 */
package com.music.spotui.ui.immersive.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.music.spotui.ui.immersive.Immersive

/** Rounded, but well short of a capsule: the corner reads as a cut, not a curve. */
private val ChipShape = RoundedCornerShape(12.dp)

/** The selected pill's label ink under the Nothing look: opaque pure black. */
private val ChipInkBlack = Color(0xFF000000)

/**
 * Filter pills rather than a tab row: squarish rounded rectangles, the selected one inverted.
 * They scroll sideways so a long label set never squeezes the text, and the gutter padding sits
 * inside the scroll so it scrolls with them. Shared by the Home facets, the Search filters and
 * the Library kinds.
 */
@Composable
internal fun ImmersiveChipRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = Immersive.Gutter, vertical = 6.dp)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** One pill of an [ImmersiveChipRow]. */
@Composable
internal fun ImmersiveChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .clip(ChipShape)
            // Nothing: the selected pill is WHITE with BLACK text (asked for). Under the other looks
            // Immersive.Text is white too, so the fill is unchanged; what changes is the label ink,
            // which used to be Immersive.Page and read as grey on the white pill.
            .background(if (selected) if (com.music.spotui.ui.look.Nothing.active) Color.White else Immersive.Text else Immersive.Raised)
            // Only a pill that is not already selected has anything to report: re-tapping the
            // current one changes nothing, so buzzing for it would be feedback for a no-op.
            .clickable(role = Role.Button) {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        // The ink is passed BOTH ways on purpose: as the Text style's colour and as the Text's own
        // `color` argument. `color` beats the style, so the selected label cannot inherit
        // Immersive.Subhead's own TextSecondary (white 86 %) and end up near-white on the white pill
        // - which is exactly how the selected chip read before this fix.
        val ink = when {
            !selected -> Immersive.Text
            // Pure black, written out: #FF000000 - no tint, no alpha, no theme token in the way.
            com.music.spotui.ui.look.Nothing.active -> ChipInkBlack
            else -> Immersive.Page
        }
        // Under Nothing the family is Doto at ROND 100: at Medium (500) the dots are thin, so black
        // dots at 14sp stipple out and read grey on the white pill. Every other Nothing label is
        // Bold (700), so the selected pill uses the same weight as the rest of the look. The other
        // looks keep Medium, byte for byte.
        val inkWeight = if (com.music.spotui.ui.look.Nothing.active) androidx.compose.ui.text.font.FontWeight.Bold
            else androidx.compose.ui.text.font.FontWeight.Medium
        Text(
            text = label,
            color = ink,
            style = Immersive.Subhead.copy(color = ink, fontWeight = inkWeight),
            maxLines = 1,
        )
    }
}
