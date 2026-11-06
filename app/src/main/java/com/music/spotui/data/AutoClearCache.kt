package com.music.spotui.data

import android.content.Context
import kotlinx.coroutines.launch
import java.io.File

/**
 * "Automatically clear Cache" (Settings › Data and battery): the replaceable files the app makes
 * for itself are deleted when it is really closed — and whatever the exit could not remove (the
 * player's open clip streams) at the next start, before there is anything new to lose.
 *
 * What goes: cover art (through Glide's own clear, so its index stays valid), the Canvas clip
 * streams, music-video answers and the audio snippets downloaded to compare them, the "share as
 * image" pictures, a downloaded update, and any other temporary file in the app's cache folders.
 *
 * What stays, whatever the switch says: downloaded songs and their records (`filesDir/offline`),
 * the lyrics cache, and every setting. None of it is cache, and none of it is inside the folders
 * this walks, so a clear cannot reach it even by accident.
 */
object AutoClearCache {
    /** Glide's own folder: cleared through Glide's API, never deleted under its feet. */
    private const val COVERS = "image_manager_disk_cache"

    /** The Canvas clip streams: open while the music plays, so they are left until nothing holds them. */
    private const val MEDIA = "media"

    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO,
    )

    /**
     * The app has just started: everything in the cache folders is, by definition, the previous
     * session's leftovers, so this is where the clear the exit began is finished. Nothing of the new
     * session exists yet. (Resuming from the background never reaches here: no new process, so
     * nothing is cleared while the app is in use.)
     */
    fun onLaunch(context: Context) {
        if (!com.music.spotui.data.preferences.AutoClearCachePref.on) return
        val app = context.applicationContext
        scope.launch {
            // After the first screen has the disk to itself: this is housework, not start-up work.
            kotlinx.coroutines.delay(2_000)
            clear(app, duringExit = false)
        }
    }

    /** The app is really closing (not a rotation): the files nothing is holding go now. */
    fun onExit(context: Context) {
        if (!com.music.spotui.data.preferences.AutoClearCachePref.on) return
        val app = context.applicationContext
        scope.launch { clear(app, duringExit = true) }
    }

    private fun clear(context: Context, duringExit: Boolean) {
        val freed = runCatching { run(context, duringExit) }.getOrElse {
            app.myspoti.host.android.Logbook.log("cache: clear failed (${it.javaClass.simpleName})")
            -1L
        }
        if (freed >= 0) app.myspoti.host.android.Logbook.log(
            "cache: ${freed / 1024} KiB cleared ${if (duringExit) "as the app closed" else "at start"}"
        )
    }

    /** Deletes the replaceable files and answers how many bytes they took. */
    private fun run(context: Context, duringExit: Boolean): Long {
        // A clip stream is open while the player runs: deleting it under the Canvas would make the
        // picture stutter for a file the decoder is reading right now. Left for a later clear.
        val playing = runCatching { com.music.spotui.di.SongPlayer.isPlaying() }.getOrDefault(false) ||
            runCatching { com.music.spotui.di.LocalPlayer.active.value }.getOrDefault(false)
        // Covers: Glide's own clear keeps its disk index valid — a raw delete would leave it
        // pointing at files that are gone. It must not run on the main thread, and this never does.
        if (DataSaverGlide.installed) runCatching { com.bumptech.glide.Glide.get(context).clearDiskCache() }
        var freed = 0L
        for (dir in listOfNotNull(context.cacheDir, context.externalCacheDir)) {
            for (file in dir.listFiles().orEmpty()) {
                if (file.name == COVERS) continue
                if (file.name == MEDIA && (duringExit || playing)) continue
                freed += size(file)
                runCatching { file.deleteRecursively() }
            }
        }
        return freed
    }

    private fun size(file: File): Long =
        if (file.isDirectory) file.listFiles().orEmpty().sumOf { size(it) } else file.length()
}
