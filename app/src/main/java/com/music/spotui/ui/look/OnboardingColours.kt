package com.music.spotui.ui.look

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.music.spotui.data.preferences.Look

/**
 * The blue the first-run flow uses instead of the Spotify green, taken from the app icon.
 *
 * It is deliberately NOT an accent swatch: the accent (and with it Home, the player and Settings)
 * stays green, and green remains pickable in the accent row. Only the screens that draw their own
 * accent — Welcome and the setup steps — were asked to go blue, so this constant exists solely for
 * them. Flat on purpose: the icon's mark is a gradient (#1AB9F1 down to #185DEA) and the accent
 * tokens these screens use are single colours.
 */
val OnboardingBlue = Color(0xFF1B78ED)

/**
 * The look step's page while **Nothing** is being previewed. The look's own page
 * ([Nothing.Page], #0A0A0A) is unchanged everywhere else in the app; only the step that offers
 * the look sits on this warmer near-black. Likewise scoped to the step: the steps that follow it
 * keep their own page.
 */
val LookStepNothingPage = Color(0xFF1B171A)

/**
 * The colour of the setup flow's chrome — step dashes, the Continue pill, the card icon tiles —
 * for the look currently in effect. The look step previews a look the moment it is tapped, so this
 * answers live and the chrome changes under the finger.
 *
 * - Classic: [OnboardingBlue] (was the accent, i.e. green).
 * - Nothing: the look's own red, so the chrome matches the red used across the look.
 * - Immersive: white, as before.
 */
@Composable
fun onboardingAccent(): Color = when (LocalLook.current) {
    Look.CLASSIC -> OnboardingBlue
    Look.NOTHING -> Nothing.Red
    Look.IMMERSIVE -> Color.White
}
