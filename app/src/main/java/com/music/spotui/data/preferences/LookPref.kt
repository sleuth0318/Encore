package com.music.spotui.data.preferences

/*
 * The app's looks: Classic (today's), Immersive, or Nothing (the minimal Nothing-brand skin),
 * picked during the onboarding or in Settings > Display.
 */

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The looks. [CLASSIC] is the default, so nothing changes for anyone who has not chosen.
 *
 * [NOTHING] is the third look added on request: it is NOT a fourth copy of the UI. It reuses the
 * Immersive screens (the ones with their own token object, [com.music.spotui.ui.immersive.Immersive])
 * because those read their colours and type from a single place: under [NOTHING] that place hands
 * out the Nothing skin (monochrome chrome, dot-matrix type, the #FF073A red dot), so the whole app
 * takes the look without a second parallel screen set. Artwork is never recoloured.
 */
enum class Look {
    CLASSIC,
    IMMERSIVE,
    NOTHING;

    companion object {
        fun of(name: String?): Look = entries.firstOrNull { it.name == name } ?: CLASSIC
    }
}

object LookPref {
    private const val PREF = "settings_prefs"
    private const val KEY = "look"
    private const val KEY_CHOSEN = "look_chosen"
    private const val KEY_OFFERED = "look_offered"

    /** The chosen look, as Compose state: everything reading it redraws when it changes. */
    var look by mutableStateOf(Look.of(prefs(com.music.spotui.di.Graph.app).getString(KEY, null)))
        private set

    fun set(c: Context, value: Look) {
        look = value
        prefs(c).edit().putString(KEY, value.name).apply()
    }

    /** The look step of the onboarding was seen (new installs and updates alike). */
    fun isChosen(c: Context): Boolean = prefs(c).getBoolean(KEY_CHOSEN, false)
    fun setChosen(c: Context) = prefs(c).edit().putBoolean(KEY_CHOSEN, true).putBoolean(KEY_OFFERED, true).apply()

    /** The new-look popup was shown to an existing user (or the onboarding look step covered it). */
    fun isOffered(c: Context): Boolean = prefs(c).getBoolean(KEY_OFFERED, false)
    fun setOffered(c: Context) = prefs(c).edit().putBoolean(KEY_OFFERED, true).apply()
    fun clearOffered(c: Context) = prefs(c).edit().remove(KEY_OFFERED).apply()

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}
