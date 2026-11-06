package com.music.spotui.ui.screens

import com.music.spotui.ui.components.wideClickable
import com.music.spotui.ui.navigation.Routes
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.myspoti.api.FriendActivity
import app.myspoti.api.TimeRange
import app.myspoti.api.Track
import app.myspoti.api.TopContent
import app.myspoti.api.best
import app.myspoti.engine.R
import app.myspoti.host.android.MyspotiRuntime
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.data.api.TopStats

import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.spotifyImageUrl
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.navigation.profileRoute
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppBackground
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone



private val StatsSubtle: Color get() = AppMuted

internal val rangeLabels = listOf(
    TimeRange.SHORT_TERM to R.string.stats_range_short,
    TimeRange.MEDIUM_TERM to R.string.stats_range_medium,
    TimeRange.LONG_TERM to R.string.stats_range_long,
)

@Composable
private fun StatsChip(label: String, selected: Boolean, onClick: () -> Unit) =
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppAccent, selectedLabelColor = Color.Black,
            labelColor = AppText, containerColor = tone(0xFF2A2A2A)))

/**
 * The account's listening stats as Spotify ranks them, drawn in the layout of the reference stats
 * screen: the three period chips, a "Your Favourite Artist" card, a "Your Favourite Song" card, a
 * collage of the top artwork with the totals beside it, then the ranked songs.
 *
 * WHAT SPOTIFY DOES NOT SEND, and is therefore NOT faked here: play counts and minutes listened.
 * `userTopContent` returns ranks plus track metadata only (see [TopStats]), so the reference
 * screen's "1 songs played / 9:04" and "Total Time Listened 34:43" have no source in this app.
 * The cards say "#1 of ..." instead, the big number is the number of ranked songs, and the
 * per-song line shows the track's LENGTH (which `Track.durationMs` does provide), labelled as
 * such rather than as listening time.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun SpotifyStatsTab(navController: NavController) {
    var range by rememberSaveable { mutableIntStateOf(0) }
    var attempt by remember { mutableIntStateOf(0) }
    val timeRange = rangeLabels[range].first
    val state by produceState<Result<TopContent>?>(TopStats.cached(timeRange)?.let { Result.success(it) }, timeRange, attempt) {
        value = runCatching { TopStats.get(timeRange, force = attempt > 0) }
    }
    val top = state?.getOrNull()
    LazyColumn(contentPadding = PaddingValues(bottom = com.music.spotui.ui.components.bottomBarsSpace())) {
        // The period chips: the reference screen's "1 week / 1 month / 3 months" row.
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(16.dp, 12.dp, 16.dp, 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rangeLabels.forEachIndexed { i, (_, label) -> StatsChip(stringResource(label), i == range) { range = i } }
            }
        }
        when {
            state == null -> item { com.music.spotui.ui.components.SkeletonList(count = 6) }
            top == null -> item {
                com.music.spotui.ui.components.ErrorRetry(stringResource(R.string.stats_failed)) { attempt++ }
            }
            // Spotify answered, and the account has nothing ranked: the empty / call-to-action state.
            top.artists.isEmpty() && top.tracks.isEmpty() -> item {
                statsEmptyState(navController)
            }
            else -> {
                val favouriteArtist = top.artists.firstOrNull()
                val favouriteSong = top.tracks.firstOrNull()
                // a) "Your Favourite Artist": round artwork, the rank, the name.
                favouriteArtist?.let { a ->
                    item {
                        StatsHighlightCard(
                            artworkUrl = spotifyImageUrl(a.images.best(300)?.url),
                            round = true,
                            label = stringResource(R.string.stats_favourite_artist),
                            title = a.name,
                            detail = stringResource(R.string.stats_rank_of, top.artistTotal ?: top.artists.size),
                            onClick = { navController.navigate(artistRoute(a.name, a.uri.substringAfterLast(':'))) },
                        )
                    }
                }
                // b) "Your Favourite Song": square artwork, the rank, the artists.
                favouriteSong?.let { t ->
                    item {
                        StatsHighlightCard(
                            artworkUrl = spotifyImageUrl(t.album?.images?.best(300)?.url),
                            round = false,
                            label = stringResource(R.string.stats_favourite_song),
                            title = t.name,
                            detail = stringResource(R.string.stats_rank_of, top.trackTotal ?: top.tracks.size),
                            onClick = { SongPlayer.playSong(t.uri) },
                        )
                    }
                }
                // c) The total with the collage of the top artwork beside it (the reference donut).
                item { StatsTotalsCollage(tracks = top.tracks) }
                // d) The ranked songs, each with its length.
                if (top.tracks.isNotEmpty()) {
                    item { StatsSectionTitle(stringResource(R.string.stats_ranked_songs, top.tracks.size)) }
                    itemsIndexed(top.tracks, key = { _, t -> t.uri }) { i, t ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { SongPlayer.playSong(t.uri) }.padding(16.dp, 6.dp),
                        ) {
                            Text("${i + 1}.", color = StatsSubtle, fontSize = 15.sp, modifier = Modifier.width(30.dp))
                            GlideImage(transition = CrossFade, model = spotifyImageUrl(t.album?.images?.best(160)?.url), contentDescription = null,
                                contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)))
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(t.name, color = AppText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    listOf(t.artists.joinToString(", ") { it.name }, clockLength(t.durationMs)).joinToString(" • "),
                                    color = StatsSubtle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                // e) The rest of the top artists, so the ranking is not lost.
                if (top.artists.size > 1) {
                    item { StatsSectionTitle(stringResource(R.string.recap_top_artists)) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            itemsIndexed(top.artists, key = { _, a -> a.uri }) { i, a ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(110.dp).clip(RoundedCornerShape(8.dp))
                                        .clickable { navController.navigate(artistRoute(a.name, a.uri.substringAfterLast(':'))) },
                                ) {
                                    com.music.spotui.ui.components.ProfileAvatar(
                                        url = spotifyImageUrl(a.images.best(300)?.url), name = a.name, size = 110.dp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("${i + 1}. ${a.name}", color = AppText, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.stats_spotify_note), color = AppMuted, fontSize = 12.sp, modifier = Modifier.padding(16.dp, 24.dp))
        }
    }
}

/** "1:29" from a track length in ms: the reference screen's per-song time. */
private fun clockLength(ms: Long): String {
    val total = ms / 1000
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

/** A "Your Favourite ..." card: artwork, a wide-tracked label, the name, the rank. */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun StatsHighlightCard(
    artworkUrl: String?,
    round: Boolean,
    label: String,
    title: String,
    detail: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(16.dp, 6.dp).clip(RoundedCornerShape(14.dp))
            .background(tone(0xFF1A1A1A)).clickable(onClick = onClick).padding(14.dp),
    ) {
        if (round) {
            com.music.spotui.ui.components.ProfileAvatar(url = artworkUrl, name = title, size = 76.dp)
        } else {
            GlideImage(
                transition = CrossFade, model = artworkUrl, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(10.dp)),
            )
        }
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(label, color = StatsSubtle, fontSize = 11.sp, letterSpacing = 0.12.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, color = StatsSubtle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * The reference screen's donut: a circle filled with the covers of the top songs, and the totals
 * beside it. Drawn with quarter/half/whole discs of artwork, which is faithful to the picture and
 * needs no new asset.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun StatsTotalsCollage(tracks: List<Track>) {
    if (tracks.isEmpty()) return
    val covers = tracks.take(4).mapNotNull { spotifyImageUrl(it.album?.images?.best(300)?.url) }
    val totalMs = tracks.sumOf { it.durationMs }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(16.dp, 18.dp),
    ) {
        Box(
            Modifier.size(132.dp).clip(CircleShape).background(tone(0xFF1A1A1A)),
        ) {
            // Four quadrants: top-left, top-right, bottom-left, bottom-right.
            listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)
                .forEachIndexed { i, align ->
                val url = covers.getOrNull(i) ?: return@forEachIndexed
                Box(
                    Modifier.align(align).size(66.dp).clip(
                        // The outer two quarters are rounded on the circle's side only.
                        when (i) {
                            0 -> RoundedCornerShape(topStart = 66.dp)
                            1 -> RoundedCornerShape(topEnd = 66.dp)
                            2 -> RoundedCornerShape(bottomStart = 66.dp)
                            else -> RoundedCornerShape(bottomEnd = 66.dp)
                        }
                    ),
                ) {
                    GlideImage(
                        transition = CrossFade, model = url, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }
        Column(Modifier.padding(start = 18.dp)) {
            Text(stringResource(R.string.stats_ranked_songs_total), color = StatsSubtle, fontSize = 12.sp,
                letterSpacing = 0.1.sp, fontWeight = FontWeight.Bold)
            // The summed LENGTH of the ranked songs, not listening minutes: labelled honestly.
            Text(
                if (totalMs >= 3_600_000) "${totalMs / 3_600_000}h ${(totalMs % 3_600_000) / 60_000}m" else "${totalMs / 60_000}m",
                color = AppAccent, fontSize = 32.sp, fontWeight = FontWeight.Bold,
            )
            Text(stringResource(R.string.stats_track_length), color = StatsSubtle, fontSize = 11.sp)
        }
    }
}

/** The recap with no Spotify data at all: what it is, and one way forward. */
@Composable
private fun statsEmptyState(navController: NavController) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.stats_empty_title), color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.stats_empty_body), color = StatsSubtle, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(AppAccent)
                .clickable { navController.navigate(com.music.spotui.ui.navigation.Routes.Home.route) }
                .padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
            Text(stringResource(R.string.stats_empty_cta), color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatsSectionTitle(text: String) =
    Text(text, color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 18.dp, 16.dp, 10.dp))



/**
 * "Your #3 artist of the past 4 weeks" under an artist's name, when the account's Spotify top
 * artists hold them (the closest the Web Player's data comes to the official app's fan badges:
 * Spotify sends no top-listener rank or "early listener" date for an artist).
 */
@Composable
fun ArtistTopBadge(artistId: String, modifier: Modifier = Modifier) {
    if (artistId.isBlank()) return
    val uri = if (artistId.startsWith("spotify:")) artistId else "spotify:artist:$artistId"
    val rank by produceState<Pair<TimeRange, Int>?>(null, uri) {
        value = runCatching {
            TopStats.rankOf(uri, TimeRange.entries.associateWith { r -> TopStats.get(r).artists.map { it.uri } })
        }.getOrNull()
    }
    val (range, n) = rank ?: return
    val text = when (range) {
        TimeRange.SHORT_TERM -> R.string.artist_top_short
        TimeRange.MEDIUM_TERM -> R.string.artist_top_medium
        TimeRange.LONG_TERM -> R.string.artist_top_long
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(16.dp, 10.dp, 16.dp, 0.dp)
            .clip(RoundedCornerShape(50)).background(AppText.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AppAccent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(text, n), color = AppText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Friend activity, as the official feed shows it: each followed person sharing their listening,
 * their latest track and where it was played from, "Now" for the past 15 minutes. Tapping the
 * person opens their profile, tapping the row plays the track in its context.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FriendActivityScreen(navController: NavController) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<Result<List<FriendActivity>>?>(null) }
    LaunchedEffect(attempt) {
        state = runCatching { MyspotiRuntime.api.users.friendActivity() }
    }
    val now = remember(state) { System.currentTimeMillis() }
    Column(Modifier.fillMaxSize().background(AppBackground).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(8.dp, 12.dp, 8.dp, 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.accessibility_navigate_back), tint = AppText,
                modifier = Modifier.wideClickable(horizontal = 8.dp, vertical = 10.dp) { navController.popBackStack() }.size(40.dp).padding(8.dp))
            Text(stringResource(R.string.friends_title), fontWeight = FontWeight.Bold, color = AppText, fontSize = 22.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp).weight(1f))
            Icon(Icons.Filled.Refresh, stringResource(R.string.friends_refresh), tint = AppText,
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable { state = null; attempt++ }.padding(8.dp))
        }
        val s = state
        val friends = s?.getOrNull()
        LazyColumn(contentPadding = PaddingValues(bottom = com.music.spotui.ui.components.bottomBarsSpace())) {
            when {
                s == null -> item { com.music.spotui.ui.components.SkeletonList(count = 6, round = true) }
                friends == null -> item {
                    com.music.spotui.ui.components.ErrorRetry(stringResource(R.string.friends_failed)) { state = null; attempt++ }
                }
                friends.isEmpty() -> item {
                    Text(stringResource(R.string.friends_empty), color = StatsSubtle, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                }
                else -> items(friends, key = { it.user.uri }) { f -> FriendRow(f, now, navController) }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun FriendRow(f: FriendActivity, now: Long, navController: NavController) {
    val track = f.track
    val openProfile = { navController.navigate(profileRoute(f.user.uri.substringAfterLast(':'), f.user.name)) }
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
            .clickable { if (track != null) SongPlayer.playSong(track.uri, contextUri = f.contextUri) else openProfile() }
            .padding(16.dp, 10.dp),
    ) {
        Box(Modifier.clip(CircleShape).clickable(onClick = openProfile)) {
            com.music.spotui.ui.components.ProfileAvatar(url = f.user.imageUrl, name = f.user.name, size = 48.dp)
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.user.name, color = AppText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).clickable(onClick = openProfile))
                Spacer(Modifier.weight(1f))
                if (f.isNowPlaying(now)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(AppAccent))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.friends_now), color = AppAccent, fontSize = 12.sp, maxLines = 1)
                } else if (f.timestampMs > 0) {
                    Text(
                        android.text.format.DateUtils.getRelativeTimeSpanString(f.timestampMs, now,
                            android.text.format.DateUtils.MINUTE_IN_MILLIS, android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE).toString(),
                        color = StatsSubtle, fontSize = 12.sp, maxLines = 1,
                    )
                }
            }
            if (track != null) {
                Text(listOf(track.name, track.artists.joinToString(", ") { it.name }).filter { it.isNotBlank() }.joinToString(" • "),
                    color = AppText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                val where = f.contextName ?: track.album?.name
                if (!where.isNullOrBlank()) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Outlined.QueueMusic, contentDescription = null, tint = StatsSubtle, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(where, color = StatsSubtle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        f.trackImageUrl?.let { cover ->
            GlideImage(transition = CrossFade, model = spotifyImageUrl(cover.replaceFirst("http://", "https://")), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.padding(start = 10.dp).size(44.dp).clip(RoundedCornerShape(4.dp)))
        }
    }
}
