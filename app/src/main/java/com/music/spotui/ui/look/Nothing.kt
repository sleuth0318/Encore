package com.music.spotui.ui.look

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import app.myspoti.engine.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * The Nothing look's tokens: the third look, beside Classic and Immersive.
 *
 * The Nothing brand is a monochrome, high-contrast, transparent-surfaces skin punctuated by one
 * red dot. The whole look is expressed here, once, and read by the screens through
 * [com.music.spotui.ui.immersive.Immersive] (which hands these values out while Look.NOTHING is
 * active) and through the app-wide header colours in ui/theme. That keeps the look app-wide
 * without a third parallel screen set, and keeps every tweak that already composes with the
 * Immersive look (Liquid glass, AMOLED, Music videos, Hide the DJ, ...) composing with this one.
 *
 * ARTWORK IS EXEMPT. Nothing here is ever applied to a cover, a thumbnail or a video frame: the
 * tokens below colour chrome only (pages, bars, sheets, text, icons, dividers, accents). No
 * saturation matrix or tint is defined in this file, on purpose.
 */
object Nothing {

    /**
     * The red the name is known for. Kept identical to the "Nothing red" accent swatch
     * ([com.music.spotui.data.api.SupportReminder.Accent.NOTHING_RED]) so the default accent of the
     * look and the pickable accent are the same colour.
     */
    val Red = Color(0xFFFF073A)

    // Pages and surfaces: near-black, and deliberately flat, with transparency as the hierarchy
    // signal instead of raised greys.
    /** The page under everything. AMOLED turns it fully black (see Immersive.Page). */
    val Page = Color(0xFF0A0A0A)
    /** A sheet or card over the page: translucent, so what is behind it stays faintly visible. */
    val Surface = Color(0xFF111111).copy(alpha = 0.86f)
    /** The same, a step up (menus, dialogs). */
    val SurfaceHigh = Color(0xFF161616).copy(alpha = 0.92f)
    /** Glass fills and edges for the bars (the Immersive "glass" chrome, restated flat). */
    val GlassFill = Color(0xFF141414).copy(alpha = 0.80f)
    val GlassEdge = Color.White.copy(alpha = 0.14f)

    // Ink. Only three steps, to keep the look quiet. Brighter than the first version (asked for:
    // the reference screenshot has near-white text where this had grey), at exactly the same sizes.
    val Text = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.86f)
    val TextTertiary = Color.White.copy(alpha = 0.68f)
    /** Hairlines: the look leans on 1dp rules instead of shadows. */
    val Separator = Color.White.copy(alpha = 0.12f)
    /** A dot that is not the accent red (inactive indicators). */
    val Dot = Color.White.copy(alpha = 0.28f)
    /** Destructive actions keep a red distinct enough from the accent dot to read as "danger". */
    val Destructive = Color(0xFFFF3B30)

    /**
     * The accent: the user's chosen accent colour when they picked one, else the Nothing red.
     * Read from [com.music.spotui.data.api.SupportReminder] so item B's swatch works here too.
     */
    val Accent: Color get() = com.music.spotui.data.api.SupportReminder.run {
        if (immersiveAccentChosen) immersiveAccent.color else Red
    }

    /**
     * Type: a real dot-matrix face.
     *
     * **Doto** by Oliver Lalan (SIL Open Font License 1.1, `licenses/Doto-OFL.txt`), bundled as
     * `res/font/nothing_dots.ttf`. It is one variable file with two axes: `wght` (100-900) and
     * `ROND` (0-100), the roundness of each dot. The look uses full roundness ([ROUNDNESS]), which
     * is what makes the glyphs read as the round dot matrix Nothing's own face is built from,
     * rather than as square pixels.
     *
     * This replaces the earlier stopgap (the platform monospace face), which had the rhythm but
     * not the dots.
     */
    @OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
    private fun doto(weight: Int) = Font(
        R.font.nothing_dots,
        weight = FontWeight(weight),
        variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(
            androidx.compose.ui.text.font.FontVariation.weight(weight),
            // ROND: 0 = square pixels, 100 = fully round dots.
            androidx.compose.ui.text.font.FontVariation.Setting("ROND", ROUNDNESS),
        ),
    )

    /** The weights the look's type scale asks for. */
    private val dotsWeights = listOf(400, 500, 600, 700, 800)

    /** The look's family: Doto at full roundness. */
    val Dots: FontFamily = FontFamily(dotsWeights.map { doto(it) })

    /** ROND axis: 100 = round dots. */
    const val ROUNDNESS: Float = 100f

    /**
     * Former name of [Dots], kept so the call sites that predate the bundled font still compile.
     * (The look used the platform monospace face before the OFL face was added.)
     */
    val Mono: FontFamily get() = Dots

    /**
     * Doto's dots already sit on a grid, so it needs less tracking than a monospace face did:
     * this is the tracking that keeps a line legible at body sizes without breaking the grid.
     */
    const val TRACKING: Float = 0.10f

    private fun style(size: Int, weight: FontWeight, tracking: Float = TRACKING) = TextStyle(
        fontFamily = Mono,
        color = Text,
        fontSize = size.sp,
        lineHeight = (size * 1.24f).sp,
        fontWeight = weight,
        letterSpacing = tracking.sp,
    )

    // The look's type scale. Labels are uppercased at the call site (see [label]).
    val Display = style(34, FontWeight.Bold)
    val Title = style(24, FontWeight.Bold)
    val Headline = style(20, FontWeight.Bold)
    val Body = style(16, FontWeight.Normal, tracking = 0.06f)
    val BodyStrong = style(16, FontWeight.Bold, tracking = 0.06f)
    val Caption = style(12, FontWeight.Normal, tracking = 0.10f).copy(color = TextSecondary)

    /** The look's signature "all caps, wide" label. */
    fun label(size: Int = 12) = style(size, FontWeight.Bold, tracking = 0.24f)

    /**
     * True under the active look. Screens that must branch on "is this Nothing" (the red dot, a
     * flatter card) call this instead of comparing the enum at each site.
     */
    val active: Boolean
        get() = com.music.spotui.data.preferences.LookPref.look == com.music.spotui.data.preferences.Look.NOTHING
}

/** The look's signature mark: one small red dot (its "record" light). Used by the tab bar and
 *  the player's state line. Nothing is drawn when the look is off, so no other look changes. */
@Composable
fun NothingDot(size: androidx.compose.ui.unit.Dp = 6.dp, modifier: Modifier = Modifier) {
    // Qualified: NothingDot is top-level, so the object's members are not in scope here.
    if (!Nothing.active) return
    Box(modifier.size(size).clip(CircleShape).background(Nothing.Red))
}
