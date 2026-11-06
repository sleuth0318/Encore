package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * The page behind every Classic screen. AMOLED still wins over the look: it asks for pure black,
 * and gets it whichever look is on (the Nothing look is a near-black, not a fixed black).
 */
val AppBackground: Color get() = when {
    app.myspoti.power.EnergySettings.oledBlack -> Color.Black
    com.music.spotui.data.preferences.LookPref.look == com.music.spotui.data.preferences.Look.NOTHING ->
        com.music.spotui.ui.look.Nothing.Page
    else -> Color(0xFF121212)
}
val GridBackground: Color get() = tone(0xFF2A2A2A)
/** The accent (Spotify green, or the colour a supporter chose). */
val AppPalette: Color get() = AppAccent
val AppAccent: Color get() = com.music.spotui.data.api.SupportReminder.accentColor

/** Primary text and icons. */
val AppText: Color get() =
    if (com.music.spotui.data.preferences.LookPref.look == com.music.spotui.data.preferences.Look.NOTHING)
        com.music.spotui.ui.look.Nothing.Text else Color.White
/** Secondary text and icons. */
val AppMuted: Color get() =
    if (com.music.spotui.data.preferences.LookPref.look == com.music.spotui.data.preferences.Look.NOTHING)
        com.music.spotui.ui.look.Nothing.TextSecondary else Color(0xFFB3B3B3)
/** Destructive actions (delete, leave, block) and failures shown as text. */
val AppDestructive = Color(0xFFF15E6C)
/** What a tint is mixed with to make a header. */
val AppShade: Color = Color.Black

/** One of the palette's raised greys (#181818 ... #3E3E3E): "a bit raised from the page". */
fun tone(argb: Long): Color = Color(argb)

/** The cover's colour as a page header: darkened toward black, so the title stays readable. */
fun headerTint(tint: Color, amount: Float): Color = lerp(tint, Color.Black, amount)
