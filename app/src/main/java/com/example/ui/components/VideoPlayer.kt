package com.example.ui.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoseError
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String?,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    autoPlay: Boolean = false,
    initialSpeed: Float = 1.0f,
    isFullscreen: Boolean = false,
    onFullscreenToggle: (() -> Unit)? = null,
    onPlaybackPositionChanged: ((positionMs: Long, durationMs: Long, isPlaying: Boolean) -> Unit)? = null,
    emptyStateContent: (@Composable () -> Unit)? = null,
) {
    val cleanUrl = videoUrl?.trim().orEmpty()
    if (cleanUrl.isEmpty()) {
        Box(
            modifier = modifier
                .clip(MaterialTheme.shapes.medium)
                .background(ObsidianBg)
                .testTag("video_player_empty"),
            contentAlignment = Alignment.Center,
        ) {
            if (emptyStateContent != null) {
                emptyStateContent()
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cloud,
                        contentDescription = "No video loaded",
                        tint = IndigoLight,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = "No cloud or local video selected",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextMain,
                    )
                    Text(
                        text = "Load a cloud stream URL or import a clip to preview.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackSpeed by remember(initialSpeed) { mutableFloatStateOf(initialSpeed.coerceIn(0.5f, 2.0f)) }
    var playerError by remember(cleanUrl) { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }

    val isCloudSource = remember(cleanUrl) {
        cleanUrl.startsWith("http://", ignoreCase = true) ||
            cleanUrl.startsWith("https://", ignoreCase = true)
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    // Listen to ExoPlayer events cleanly
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                val dur = exoPlayer.duration
                if (dur > 0L) durationMs = dur
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                playerError = error.localizedMessage ?: "Unable to stream video (${error.errorCodeName})."
                isPlaying = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Pause automatically when app goes to background
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load or switch media item when URL changes
    LaunchedEffect(cleanUrl, autoPlay) {
        playerError = null
        currentPositionMs = 0L
        durationMs = 0L
        exoPlayer.setMediaItem(MediaItem.fromUri(cleanUrl))
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
        exoPlayer.playWhenReady = autoPlay
        exoPlayer.prepare()
    }

    // Sync speed and mute state
    LaunchedEffect(playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Poll playback position smoothly only while playing
    LaunchedEffect(exoPlayer, isPlaying) {
        if (isPlaying) {
            while (true) {
                if (!isScrubbing) {
                    currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                }
                val dur = exoPlayer.duration
                if (dur > 0L) {
                    durationMs = dur
                }
                onPlaybackPositionChanged?.invoke(currentPositionMs, durationMs, true)
                delay(250L)
            }
        } else {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            if (!isScrubbing) {
                currentPositionMs = pos
            }
            if (dur > 0L) {
                durationMs = dur
            }
            onPlaybackPositionChanged?.invoke(pos, dur, false)
        }
    }

    // Auto-hide controls while playing
    LaunchedEffect(controlsVisible, isPlaying, playerError) {
        if (controlsVisible && isPlaying && playerError == null) {
            delay(3500L)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(ObsidianBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                controlsVisible = !controlsVisible
            }
            .testTag("media3_video_player"),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setKeepContentOnPlayerReset(true)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { view ->
                if (view.player !== exoPlayer) {
                    view.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Buffering Indicator
        if (playbackState == Player.STATE_BUFFERING && playerError == null) {
            CircularProgressIndicator(
                color = IndigoLight,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
                    .testTag("video_player_buffering"),
            )
        }

        // Error Overlay with Retry
        val err = playerError
        if (err != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.88f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Playback error",
                        tint = RoseError,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = RoseError,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Button(
                        onClick = {
                            playerError = null
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.testTag("video_player_retry_button"),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Stream")
                    }
                }
            }
        } else {
            // Controls Overlay
            AnimatedVisibility(
                visible = controlsVisible || !isPlaying,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ObsidianBg.copy(alpha = 0.72f),
                                    Color.Transparent,
                                    ObsidianBg.copy(alpha = 0.86f),
                                )
                            )
                        )
                        .padding(10.dp),
                ) {
                    // Top HUD: Source Badge, Title & Speed Cycle Chip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Surface(
                                color = (if (isCloudSource) CyanAccent else EmeraldSuccess).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, (if (isCloudSource) CyanAccent else EmeraldSuccess).copy(alpha = 0.5f)),
                            ) {
                                Text(
                                    text = if (isCloudSource) "CLOUD" else "CLIP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCloudSource) CyanAccent else EmeraldSuccess,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                            if (!title.isNullOrBlank()) {
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextMain,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (!subtitle.isNullOrBlank()) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            color = StudioSurfaceElevated.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable {
                                    playbackSpeed = when {
                                        playbackSpeed < 0.75f -> 1.0f
                                        playbackSpeed < 1.25f -> 1.5f
                                        playbackSpeed < 1.75f -> 2.0f
                                        else -> 0.5f
                                    }
                                }
                                .testTag("video_player_speed_button"),
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = IndigoLight,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }

                    // Center Transport Row (-10s, Play/Pause/Replay, +10s)
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        IconButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .background(ObsidianBg.copy(alpha = 0.55f), CircleShape)
                                .testTag("video_player_seek_back"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay10,
                                contentDescription = "Rewind 10 seconds",
                                tint = TextMain,
                            )
                        }

                        Surface(
                            color = IndigoPrimary.copy(alpha = 0.92f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(54.dp)
                                .clickable {
                                    if (playbackState == Player.STATE_ENDED) {
                                        exoPlayer.seekTo(0L)
                                        exoPlayer.play()
                                    } else if (isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                }
                                .testTag("video_player_play_pause"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when {
                                        playbackState == Player.STATE_ENDED -> Icons.Filled.Replay
                                        isPlaying -> Icons.Filled.Pause
                                        else -> Icons.Filled.PlayArrow
                                    },
                                    contentDescription = when {
                                        playbackState == Player.STATE_ENDED -> "Replay video"
                                        isPlaying -> "Pause video"
                                        else -> "Play video"
                                    },
                                    tint = TextMain,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val maxDur = if (durationMs > 0L) durationMs else Long.MAX_VALUE
                                val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(maxDur)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .background(ObsidianBg.copy(alpha = 0.55f), CircleShape)
                                .testTag("video_player_seek_forward"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = TextMain,
                            )
                        }
                    }

                    // Bottom Scrubber & Audio/Fullscreen Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val shownPos = if (isScrubbing) scrubPositionMs else currentPositionMs
                        Text(
                            text = formatDurationMmSs(shownPos),
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                        )
                        Slider(
                            value = shownPos.toFloat().coerceIn(0f, durationMs.coerceAtLeast(1L).toFloat()),
                            onValueChange = { value ->
                                isScrubbing = true
                                scrubPositionMs = value.toLong()
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo(scrubPositionMs)
                                currentPositionMs = scrubPositionMs
                                isScrubbing = false
                            },
                            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = EmeraldSuccess,
                                activeTrackColor = IndigoPrimary,
                                inactiveTrackColor = TextMuted.copy(alpha = 0.35f),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("video_player_scrubber"),
                        )
                        Text(
                            text = formatDurationMmSs(durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("video_player_mute_button"),
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute audio" else "Mute audio",
                                tint = TextMain,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        if (onFullscreenToggle != null) {
                            IconButton(
                                onClick = onFullscreenToggle,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("video_player_fullscreen_button"),
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                    contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen preview",
                                    tint = TextMain,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CloudVideoPreviewDialog(
    videoUrl: String,
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = StudioSurface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("cloud_video_preview_dialog"),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("cloud_preview_close_button"),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close preview", tint = TextMuted)
                    }
                }

                VideoPlayer(
                    videoUrl = videoUrl,
                    title = title,
                    subtitle = subtitle,
                    autoPlay = true,
                    isFullscreen = true,
                    onFullscreenToggle = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            }
        }
    }
}

private fun formatDurationMmSs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
