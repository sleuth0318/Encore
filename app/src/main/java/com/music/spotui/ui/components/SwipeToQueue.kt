package com.music.spotui.ui.components

import app.myspoti.engine.R

import androidx.compose.ui.res.stringResource

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.music.spotui.data.entity.SongsModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Spotify's gesture: swipe a song row sideways to add it to the queue. The row
 * follows the finger over a green strip, ticks once with a vibration when it
 * passes the threshold, queues the song ONCE on release, then springs back.
 *
 * The two directions differ, as asked in the round that reversed them:
 * - swipe LEFT  -> the song goes LAST in the queue ([SongPlayer.addToQueue]),
 *   and the toast reads "Added to Queue";
 * - swipe RIGHT -> the song goes FIRST in the queue, right after the current
 *   track ([SongPlayer.playNext]), and the toast reads "Playing next".
 *
 * Nothing else about the gesture changed: same strip, same threshold, same
 * single haptic tick, same spring back.
 */
@Composable
fun SwipeToQueue(song: SongsModel, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
    val maxDrag = threshold * 1.4f
    val offset = remember { Animatable(0f) }
    var armed by remember { mutableStateOf(false) }
    // The direction of the swipe in flight: LEFT appends, RIGHT goes to the top of the queue
    // (asked for; the two were the other way round).
    var toTop by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        if (offset.value != 0f) {
            Box(
                contentAlignment = if (offset.value > 0) Alignment.CenterStart else Alignment.CenterEnd,
                modifier = Modifier
                    .matchParentSize()
                    .background(if (armed) com.music.spotui.ui.theme.AppAccent else com.music.spotui.ui.theme.AppAccent.copy(alpha = 0.6f)),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = stringResource(R.string.add_to_queue_action_content_description),
                    tint = Color.Black,
                    modifier = Modifier.padding(horizontal = 24.dp).size(26.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(com.music.spotui.ui.theme.AppBackground)
                .pointerInput(song.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (armed && song.spotifyTrackId.isNotBlank()) {
                                // Left: last in the queue. Right: first in the queue ("Playing next").
                                if (toTop) {
                                    com.music.spotui.di.SongPlayer.playNext(song.spotifyTrackId)
                                    android.widget.Toast.makeText(context, context.getString(R.string.snackbar_playing_next), android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    com.music.spotui.di.SongPlayer.addToQueue(song.spotifyTrackId)
                                    android.widget.Toast.makeText(context, context.getString(R.string.snackbar_added_to_queue), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            armed = false
                            scope.launch { offset.animateTo(0f, spring()) }
                        },
                        onDragCancel = {
                            armed = false
                            scope.launch { offset.animateTo(0f, spring()) }
                        },
                    ) { change, dx ->
                        change.consume()
                        val next = (offset.value + dx).coerceIn(-maxDrag, maxDrag)
                        scope.launch { offset.snapTo(next) }
                        toTop = next > 0
                        val nowArmed = abs(next) >= threshold
                        if (nowArmed && !armed) haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        armed = nowArmed
                    }
                },
        ) { content() }
    }
}
