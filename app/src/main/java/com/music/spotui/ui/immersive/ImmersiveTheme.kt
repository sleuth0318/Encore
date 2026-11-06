package com.music.spotui.ui.immersive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.ui.look.ImmersiveDisplay
import com.music.spotui.ui.look.ImmersiveSans

/*
 * The Immersive look's tokens, shared by every Immersive screen so they read as one app:
 * a black page, Inter, large bold titles, rounded covers, translucent "glass" bars and an
 * accent taken from the playing cover where there is one.
 */
object Immersive {
    /*
     * Two looks read these tokens: Immersive, and Nothing (the third look, added on request).
     * Nothing is a skin over this same set, so every screen drawn from these tokens takes it
     * without a parallel screen set, and every display tweak that already composes with the
     * Immersive look composes with Nothing too. Under Classic none of this is read.
     */
    private val nothing: Boolean get() = com.music.spotui.ui.look.Nothing.active

    // Colours.
    /** The page. Nothing is near-black rather than black, so its red dot has something to sit on. */
    val Page: Color get() = if (nothing) com.music.spotui.ui.look.Nothing.Page else Color.Black
    val Raised: Color get() = if (nothing) com.music.spotui.ui.look.Nothing.SurfaceHigh else Color(0xFF1C1C1E)
    val Separator: Color get() = if (nothing) com.music.spotui.ui.look.Nothing.Separator else Color.White.copy(alpha = 0.10f)
    val Text: Color get() = if (nothing) com.music.spotui.ui.look.Nothing.Text else Color.White
    val TextSecondary: Color get() = if (nothing) com.music.spotui.ui.look.Nothing.TextSecondary else Color.White.copy(alpha = 0.60f)
    val TextTertiary: Color get() = if (nothing) com.music.spotui.ui.look.Nothing.TextTertiary else Color.White.copy(alpha = 0.38f)
    /** The look's own pink until an Immersive accent colour is chosen in Settings, then that colour (links, the selected tab, toggles). Nothing falls back to its red instead. */
    val Accent: Color get() = if (nothing) com.music.spotui.ui.look.Nothing.Accent else com.music.spotui.data.api.SupportReminder.run {
        if (immersiveAccentChosen) immersiveAccent.color else Color(0xFFFF4F7B)
    }
    val GlassFill: Color get() = if (nothing) com.music.spotui.ui.look.Nothing.GlassFill else Color(0xFF2C2C34).copy(alpha = 0.88f)
    val GlassEdge: Color get() = if (nothing) com.music.spotui.ui.look.Nothing.GlassEdge else Color.White.copy(alpha = 0.12f)
    /** The red of a destructive swipe, menu entry or confirmation (Leave, Remove, an error). */
    val Destructive: Color get() = if (nothing) com.music.spotui.ui.look.Nothing.Destructive else Color(0xFFFF453A)

    // Shapes.
    val CoverSmall: Shape = RoundedCornerShape(6.dp)
    val CoverMedium: Shape = RoundedCornerShape(10.dp)
    val CoverLarge: Shape = RoundedCornerShape(14.dp)
    val Card: Shape = RoundedCornerShape(14.dp)
    val Button: Shape = RoundedCornerShape(14.dp)

    // Type (Inter; the bundled Doto dot-matrix scale under the Nothing look).
    private val base: TextStyle get() = if (nothing) TextStyle(fontFamily = com.music.spotui.ui.look.Nothing.Mono, color = Text, letterSpacing = com.music.spotui.ui.look.Nothing.TRACKING.sp) else TextStyle(fontFamily = ImmersiveSans, color = Text)
    private val display: TextStyle get() = if (nothing) TextStyle(fontFamily = com.music.spotui.ui.look.Nothing.Mono, color = Text, letterSpacing = com.music.spotui.ui.look.Nothing.TRACKING.sp) else TextStyle(fontFamily = ImmersiveDisplay, color = Text)
    val LargeTitle: TextStyle get() = if (nothing) com.music.spotui.ui.look.Nothing.Display else display.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
    val Title: TextStyle get() = if (nothing) com.music.spotui.ui.look.Nothing.Title else display.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
    val Headline: TextStyle get() = if (nothing) com.music.spotui.ui.look.Nothing.Headline else display.copy(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
    val Body: TextStyle get() = if (nothing) com.music.spotui.ui.look.Nothing.Body else base.copy(fontSize = 16.sp, lineHeight = 21.sp)
    val BodyStrong: TextStyle get() = if (nothing) com.music.spotui.ui.look.Nothing.BodyStrong else Body.copy(fontWeight = FontWeight.SemiBold)
    // Same size and ink in both looks; only the family (via base) changes.
    val Subhead: TextStyle get() = base.copy(fontSize = 14.sp, lineHeight = 19.sp, color = TextSecondary)
    val Caption: TextStyle get() = if (nothing) com.music.spotui.ui.look.Nothing.Caption else base.copy(fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
    val Lyric: TextStyle get() = if (nothing) display.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold) else display.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)

    // Spacing.
    val Gutter = 20.dp
    val RowHeight = 60.dp
    /** Room the floating tab bar and mini player take at the bottom of every page. */
    val BottomChrome = 170.dp
}

/** A translucent bar or capsule (tab bar, mini player, menus). */
fun Modifier.glass(shape: Shape = CircleShape): Modifier =
    this.background(Immersive.GlassFill, shape).border(1.dp, Immersive.GlassEdge, shape)
