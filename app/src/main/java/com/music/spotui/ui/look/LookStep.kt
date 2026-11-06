package com.music.spotui.ui.look

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import com.music.spotui.data.preferences.Look
import com.music.spotui.data.preferences.LookPref
import com.music.spotui.ui.theme.AppBackground
import com.music.spotui.ui.theme.AppSans
import kotlin.math.abs

/** Set when the step is the "new look" popup shown to existing users: its own title, buttons and note. */
class LookOffer(val onKeep: () -> Unit)

/**
 * Onboarding, the look: two segments over a live preview that plays the demo song in the chosen
 * look. The choice is saved as soon as a segment is touched, and the page itself takes the look
 * (its backdrop, type and button), so the difference shows twice.
 */
@Composable
fun LookStep(onNext: () -> Unit, offer: LookOffer? = null) {
    val context = LocalContext.current
    val look = LocalLook.current
    // Nothing is styled like Immersive here (it is the same chooser, skinned), so only Classic
    // takes the Classic treatment.
    val immersive = look != Look.CLASSIC
    val reduceMotion = rememberReduceMotion()
    var paused by remember { mutableStateOf(false) }
    val clock = rememberDemoClock(running = !paused && !reduceMotion)
    val choose: (Look) -> Unit = { if (it != look) LookPref.set(context, it) }
    val family = if (immersive) ImmersiveSans else AppSans
    val text = TextStyle(fontFamily = family, color = Color.White)
    val dim = Color.White.copy(alpha = if (immersive) 0.72f else 1f)
    val subtle = if (immersive) Color.White.copy(alpha = 0.7f) else Color(0xFFB3B3B3)

    // Line boxes: Manrope (Classic) builds one 1.366x its font size, Inter (Immersive and Nothing)
    // 1.21x. Left natural, every line in Classic is ~13% taller, so the header pushes the preview
    // down and the whole page jumps when the look changes. Classic draws its line boxes at Inter's
    // ratio, which leaves Immersive and Nothing on their natural metrics — untouched — and makes the
    // two lay out to the same height.
    val lh: (Float) -> androidx.compose.ui.unit.TextUnit =
        { size -> if (immersive) androidx.compose.ui.unit.TextUnit.Unspecified else (size * INTER_LINE_RATIO).sp }

    Box(Modifier.fillMaxSize()) {
        Crossfade(look, animationSpec = tween(350), label = "lookBackdrop") {
            when (it) {
                Look.IMMERSIVE -> MeshGradientBackground(cyclingMeshPalette(LookMeshPalettes), colorFadeMillis = MESH_CYCLE_MILLIS)
                // Nothing is flat and near-black: no mesh, no tint behind the preview.
                Look.NOTHING -> Box(Modifier.fillMaxSize().background(LookStepNothingPage))
                Look.CLASSIC -> Box(Modifier.fillMaxSize().background(AppBackground))
            }
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(if (offer != null) R.string.look_offer_title else R.string.setup_look_title), style = text, fontSize = 26.sp, lineHeight = lh(26f),
                fontWeight = if (immersive) FontWeight.SemiBold else FontWeight.Bold)
            Text(stringResource(R.string.setup_look_subtitle), style = text, color = subtle, fontSize = 14.sp, lineHeight = lh(14f))
            LookSegments(look, immersive, family, choose)
            Box(Modifier.fillMaxWidth().weight(1f)) {
                val description = stringResource(R.string.setup_look_preview, stringResource(look.nameRes()))
                if (reduceMotion) {
                    LookStills(look, subtle, Modifier.fillMaxSize().semantics { contentDescription = description })
                } else {
                    PhoneFrame(Modifier.fillMaxSize().semantics { contentDescription = description }) {
                        Crossfade(look, animationSpec = tween(350), label = "lookPreview") { LookDemo(it, clock) }
                    }
                }
                // Above the preview, so nothing in it reacts: a tap pauses or resumes, a swipe
                // changes the look.
                Box(Modifier.matchParentSize().pointerInput(look) {
                    val swipe = 56.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var dx = 0f
                        var moved = false
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            dx = change.position.x - down.position.x
                            if (abs(dx) > viewConfiguration.touchSlop || abs(change.position.y - down.position.y) > viewConfiguration.touchSlop) moved = true
                            change.consume()
                        }
                        // A swipe walks the three looks in order (Classic, Immersive, Nothing).
                        when {
                            !moved -> paused = !paused
                            dx < -swipe -> choose(Look.entries[(look.ordinal + 1) % Look.entries.size])
                            dx > swipe -> choose(Look.entries[(look.ordinal + Look.entries.size - 1) % Look.entries.size])
                        }
                    }
                })
            }
            Text(
                stringResource(if (reduceMotion) R.string.setup_look_still_hint else R.string.setup_look_tap_hint),
                style = text, color = dim.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = lh(12f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onNext,
                shape = if (immersive) RoundedCornerShape(14.dp) else ButtonDefaults.shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = onboardingAccent(),
                    contentColor = Color.Black,
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text(stringResource(if (offer != null) R.string.look_offer_try else R.string.setup_continue), fontFamily = family, fontSize = 16.sp, lineHeight = lh(16f),
                    fontWeight = if (immersive) FontWeight.SemiBold else FontWeight.Bold)
            }
            if (offer != null) {
                TextButton(onClick = offer.onKeep, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.look_offer_keep), color = Color.White, fontFamily = family, fontSize = 15.sp, lineHeight = lh(15f))
                }
                Text(stringResource(R.string.look_offer_note), style = text, color = dim.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = lh(12f),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun Look.nameRes() = when (this) {
    Look.IMMERSIVE -> R.string.look_immersive
    Look.NOTHING -> R.string.look_nothing
    Look.CLASSIC -> R.string.look_classic
}
private fun Look.detailRes() = when (this) {
    Look.IMMERSIVE -> R.string.look_immersive_detail
    Look.NOTHING -> R.string.look_nothing_detail
    Look.CLASSIC -> R.string.look_classic_detail
}

/** "Classic | Immersive", each with a line on what it shows; drawn in the chosen look. */
@Composable
private fun LookSegments(look: Look, immersive: Boolean, family: androidx.compose.ui.text.font.FontFamily, choose: (Look) -> Unit) {
    val outer = if (immersive) RoundedCornerShape(18.dp) else RoundedCornerShape(24.dp)
    val inner = if (immersive) RoundedCornerShape(14.dp) else RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (immersive) Color.White.copy(alpha = 0.14f) else Color(0xFF242424), outer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Look.entries.forEach { option ->
            val selected = option == look
            val fill by animateColorAsState(
                when {
                    !selected -> Color.Transparent
                    // Nothing's own box is the look's red; the ink on it stays black, as it is on the
                    // Continue pill below.
                    option == Look.NOTHING -> Nothing.Red
                    immersive -> Color.White.copy(alpha = 0.92f)
                    else -> OnboardingBlue
                },
                label = "segmentFill",
            )
            val ink = if (selected) Color.Black else Color.White
            Column(
                Modifier
                    .weight(1f)
                    .background(fill, inner)
                    .clickable { choose(option) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(option.nameRes()), color = ink, fontFamily = family, fontSize = 15.sp,
                    lineHeight = if (immersive) androidx.compose.ui.unit.TextUnit.Unspecified else (15f * INTER_LINE_RATIO).sp,
                    fontWeight = if (immersive) FontWeight.SemiBold else FontWeight.Bold)
                Text(stringResource(option.detailRes()), color = ink.copy(alpha = 0.72f), fontFamily = family, fontSize = 11.sp,
                    lineHeight = 14.sp, textAlign = TextAlign.Center, minLines = 2, maxLines = 2)
            }
        }
    }
}

/** "Remove animations": three still frames of the loop instead of the loop. */
@Composable
private fun LookStills(look: Look, caption: Color, modifier: Modifier) {
    val captions = listOf(R.string.setup_look_still_player, R.string.setup_look_still_paused, R.string.setup_look_still_lyrics)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DemoScript.stills.forEachIndexed { index, ms ->
            Column(Modifier.weight(1f).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                PhoneFrame(Modifier.fillMaxWidth().weight(1f)) { LookDemo(look, rememberStillClock(ms), still = true) }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(captions[index]), color = caption, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

/** Inter's natural line box, as a multiple of the font size (hhea 1984 + 494 over 2048 upem). */
private const val INTER_LINE_RATIO = 1.21f
