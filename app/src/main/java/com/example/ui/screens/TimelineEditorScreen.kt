package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ReviewComment
import com.example.data.model.StudioProject
import com.example.data.model.TimelineClip
import com.example.ui.components.CloudVideoPreviewDialog
import com.example.ui.components.VideoPlayer
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHigh
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay

@Composable
fun TimelineEditorScreen(
    projectsState: UiState<List<StudioProject>>,
    selectedProjectId: String?,
    clipsState: UiState<List<TimelineClip>>,
    commentsState: UiState<List<ReviewComment>>,
    selectedMediaUri: String?,
    onSelectProject: (String) -> Unit,
    onMediaPicked: (String?) -> Unit,
    onUpdateProjectSettings: (StudioProject, String, Boolean) -> Unit,
    onAddClip: (projectId: String, trackType: String, label: String, startMs: Long, durationMs: Long, speedMultiplier: Double, mediaUri: String, notes: String) -> Unit,
    onUpdateClip: (projectId: String, clip: TimelineClip, durationMs: Long, speedMultiplier: Double, notes: String) -> Unit,
    onDeleteClip: (projectId: String, clipId: String) -> Unit,
    onAddReviewComment: (projectId: String, timestampMs: Long, body: String) -> Unit,
    onToggleCommentResolved: (projectId: String, comment: ReviewComment) -> Unit,
    onDispatchRender: (StudioProject, String) -> Unit,
    onCreateStarterCut: () -> Unit,
) {
    val projects = (projectsState as? UiState.Success)?.data.orEmpty()
    val activeProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()

    LaunchedEffect(activeProject?.id) {
        if (activeProject != null && activeProject.id != selectedProjectId) {
            onSelectProject(activeProject.id)
        }
    }

    val clips = (clipsState as? UiState.Success)?.data.orEmpty()
    val comments = (commentsState as? UiState.Success)?.data.orEmpty()

    var playheadMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var showAddClipDialog by remember { mutableStateOf(false) }
    var showCloudUrlDialog by remember { mutableStateOf(false) }
    var showFullscreenPreview by remember { mutableStateOf(false) }
    var editingClip by remember { mutableStateOf<TimelineClip?>(null) }
    var commentDraft by remember { mutableStateOf("") }

    val activeMediaUri = remember(selectedMediaUri, clips) {
        selectedMediaUri?.takeIf { it.isNotBlank() }
            ?: clips.firstOrNull { it.mediaUri.isNotBlank() }?.mediaUri
    }
    val activeMediaClip = remember(activeMediaUri, clips) {
        if (activeMediaUri.isNullOrBlank()) null
        else clips.firstOrNull { it.mediaUri == activeMediaUri }
    }

    val totalTimelineMs = remember(clips) {
        clips.maxOfOrNull { it.startMs + it.durationMs }?.coerceAtLeast(15_000L) ?: 30_000L
    }

    // Advance virtual playhead when playing in waveform-only monitor mode
    LaunchedEffect(isPlaying, totalTimelineMs, activeMediaUri) {
        if (activeMediaUri.isNullOrBlank()) {
            while (isPlaying) {
                delay(100L)
                playheadMs = if (playheadMs + 100L >= totalTimelineMs) 0L else playheadMs + 100L
            }
        }
    }

    // Zero-permission Android Photo Picker for real local video playback
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onMediaPicked(uri.toString())
            if (activeProject != null) {
                onAddClip(
                    activeProject.id,
                    "video",
                    "Imported Device Clip",
                    playheadMs,
                    8000L,
                    1.0,
                    uri.toString(),
                    "Imported via Android Photo Picker"
                )
            }
        }
    }

    if (activeProject == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "No Active Timeline Selected",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextMain,
                )
                Text(
                    text = "Load a starter multi-track sequence or create a project in the Studio tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                Button(
                    onClick = onCreateStarterCut,
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                ) {
                    Text("Load Starter Cut")
                }
            }
        }
        return
    }

    val activeCaption = clips.firstOrNull {
        it.trackType == "caption" && playheadMs in it.startMs..(it.startMs + it.durationMs)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Project Selector & Proxy Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(projects, key = { it.id }) { proj ->
                        FilterChip(
                            selected = proj.id == activeProject.id,
                            onClick = { onSelectProject(proj.id) },
                            label = { Text(proj.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("4K/8K Proxy", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Switch(
                            checked = activeProject.proxyEnabled,
                            onCheckedChange = { checked ->
                                onUpdateProjectSettings(activeProject, activeProject.lutPreset, checked)
                            },
                        )
                    }
                    Button(
                        onClick = { onDispatchRender(activeProject, "YouTube 4K H.265") },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = ObsidianBg),
                        modifier = Modifier.testTag("timeline_render_button"),
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export", fontWeight = FontWeight.Bold)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_video_button"),
                    ) {
                        Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Video", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { showCloudUrlDialog = true },
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_cloud_video_button"),
                    ) {
                        Icon(
                            Icons.Filled.Cloud,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cloud URL", color = CyanAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Video Preview Monitor (Media3 ExoPlayer VideoPlayer when media loaded, or Live Studio Canvas)
        item {
            PreviewMonitorCard(
                project = activeProject,
                mediaUri = activeMediaUri,
                mediaTitle = activeMediaClip?.label ?: activeProject.title,
                playheadMs = playheadMs,
                totalMs = totalTimelineMs,
                isPlaying = isPlaying,
                activeCaption = activeCaption?.label,
                onTogglePlay = { isPlaying = !isPlaying },
                onReset = {
                    isPlaying = false
                    playheadMs = 0L
                },
                onScrub = { newMs -> playheadMs = newMs },
                onPlaybackPositionChanged = { posMs, _, playerPlaying ->
                    playheadMs = posMs
                    isPlaying = playerPlaying
                },
                onOpenFullscreen = { showFullscreenPreview = true },
                onClearMedia = { onMediaPicked(null) },
            )
        }

        // Multi-Track Timeline Canvas
        item {
            Surface(
                color = StudioSurface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Multi-Track Timeline", style = MaterialTheme.typography.titleMedium, color = TextMain)
                            Text("${clips.size} active layers • Tap clip to trim/speed-ramp", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        OutlinedButton(
                            onClick = { showAddClipDialog = true },
                            border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("add_timeline_clip_button"),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Layer")
                        }
                    }

                    val tracks = listOf(
                        "video" to ("V1" to IndigoPrimary),
                        "vfx" to ("FX" to VioletAccent),
                        "audio" to ("A1" to EmeraldSuccess),
                        "caption" to ("CC" to CyanAccent),
                    )
                    val horizontalScroll = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScroll),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        tracks.forEach { (trackKey, meta) ->
                            val (trackBadge, trackColor) = meta
                            val trackClips = clips.filter { it.trackType == trackKey }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    color = trackColor.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(width = 36.dp, height = 44.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = trackBadge,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = trackColor,
                                        )
                                    }
                                }
                                if (trackClips.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .width(260.dp)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(StudioSurfaceElevated)
                                            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                                            .clickable { showAddClipDialog = true },
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        Text(
                                            text = "Empty $trackBadge track — tap + Add Layer",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted,
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                        )
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        trackClips.forEach { clip ->
                                            val blockWidth = ((clip.durationMs / 1000f) * 26f).coerceIn(110f, 340f).dp
                                            Surface(
                                                color = trackColor.copy(alpha = 0.22f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, trackColor.copy(alpha = 0.65f)),
                                                modifier = Modifier
                                                    .width(blockWidth)
                                                    .height(44.dp)
                                                    .clickable { editingClip = clip },
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Text(
                                                        text = clip.label,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextMain,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "${clip.speedMultiplier}x",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = trackColor,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // VFX & Color LUT Selector
        item {
            val luts = listOf("Obsidian Cinema", "Cyber Indigo", "Teal & Amber", "Noir Monochrome", "Natural Rec.709")
            Surface(
                color = StudioSurface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("VFX & Color Grading LUTs", style = MaterialTheme.typography.titleMedium, color = TextMain)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(luts) { lut ->
                            FilterChip(
                                selected = activeProject.lutPreset == lut,
                                onClick = {
                                    onUpdateProjectSettings(activeProject, lut, activeProject.proxyEnabled)
                                },
                                label = { Text(lut) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VioletAccent.copy(alpha = 0.28f)),
                            )
                        }
                    }
                }
            }
        }

        // Team Collaboration & Timestamped Review Notes
        item {
            Surface(
                color = StudioSurface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, tint = IndigoLight, modifier = Modifier.size(18.dp))
                        Text("Team Review Notes (${comments.size})", style = MaterialTheme.typography.titleMedium, color = TextMain)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = commentDraft,
                            onValueChange = { commentDraft = it },
                            label = { Text("Note at ${formatTimecode(playheadMs)}") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("review_comment_input"),
                        )
                        Button(
                            onClick = {
                                onAddReviewComment(activeProject.id, playheadMs, commentDraft)
                                commentDraft = ""
                            },
                            enabled = commentDraft.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier.testTag("post_comment_button"),
                        ) {
                            Text("Post")
                        }
                    }

                    comments.forEach { comment ->
                        Surface(
                            color = StudioSurfaceElevated,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { playheadMs = comment.timestampMs },
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = formatTimecode(comment.timestampMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyanAccent,
                                        )
                                        Text(
                                            text = comment.authorName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = comment.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (comment.resolved) TextMuted else TextMain,
                                    )
                                }
                                IconButton(onClick = { onToggleCommentResolved(activeProject.id, comment) }) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Resolve note",
                                        tint = if (comment.resolved) EmeraldSuccess else TextMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddClipDialog) {
        AddTimelineClipDialog(
            defaultStartMs = playheadMs,
            onDismiss = { showAddClipDialog = false },
            onConfirm = { trackType, label, durationMs, speed, notes ->
                showAddClipDialog = false
                onAddClip(activeProject.id, trackType, label, playheadMs, durationMs, speed, "", notes)
            },
        )
    }

    editingClip?.let { clip ->
        EditClipDialog(
            clip = clip,
            onDismiss = { editingClip = null },
            onPreviewClip = { mediaUrl ->
                editingClip = null
                onMediaPicked(mediaUrl)
            },
            onSave = { durMs, speed, notes ->
                editingClip = null
                onUpdateClip(activeProject.id, clip, durMs, speed, notes)
            },
            onDelete = {
                editingClip = null
                onDeleteClip(activeProject.id, clip.id)
            },
        )
    }

    if (showCloudUrlDialog) {
        CloudVideoUrlDialog(
            initialUrl = activeMediaUri?.takeIf { it.startsWith("http", ignoreCase = true) }.orEmpty(),
            onDismiss = { showCloudUrlDialog = false },
            onLoadStream = { url, label, saveToTimeline ->
                showCloudUrlDialog = false
                onMediaPicked(url)
                if (saveToTimeline) {
                    onAddClip(
                        activeProject.id,
                        "video",
                        label.ifBlank { "Cloud Video Clip" },
                        playheadMs,
                        12_000L,
                        1.0,
                        url,
                        "Streamed from cloud storage URL"
                    )
                }
            },
        )
    }

    if (showFullscreenPreview && !activeMediaUri.isNullOrBlank()) {
        CloudVideoPreviewDialog(
            videoUrl = activeMediaUri,
            title = activeMediaClip?.label ?: activeProject.title,
            subtitle = "${activeProject.resolution} • ${activeProject.lutPreset}",
            onDismiss = { showFullscreenPreview = false },
        )
    }
}

@Composable
private fun PreviewMonitorCard(
    project: StudioProject,
    mediaUri: String?,
    mediaTitle: String,
    playheadMs: Long,
    totalMs: Long,
    isPlaying: Boolean,
    activeCaption: String?,
    onTogglePlay: () -> Unit,
    onReset: () -> Unit,
    onScrub: (Long) -> Unit,
    onPlaybackPositionChanged: (positionMs: Long, durationMs: Long, isPlaying: Boolean) -> Unit,
    onOpenFullscreen: () -> Unit,
    onClearMedia: () -> Unit,
) {
    val lutTint = when (project.lutPreset) {
        "Cyber Indigo" -> IndigoPrimary.copy(alpha = 0.24f)
        "Teal & Amber" -> CyanAccent.copy(alpha = 0.20f)
        "Noir Monochrome" -> Color.White.copy(alpha = 0.08f)
        else -> VioletAccent.copy(alpha = 0.14f)
    }

    Surface(
        color = StudioSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!mediaUri.isNullOrBlank()) {
                VideoPlayer(
                    videoUrl = mediaUri,
                    title = mediaTitle,
                    subtitle = "${project.resolution} • ${project.lutPreset}",
                    autoPlay = false,
                    onFullscreenToggle = onOpenFullscreen,
                    onPlaybackPositionChanged = onPlaybackPositionChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (!activeCaption.isNullOrBlank()) "CC: $activeCaption" else "Cloud & Device Media3 Preview",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!activeCaption.isNullOrBlank()) CyanAccent else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearMedia) {
                        Text("Show Waveform")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(ObsidianBg)
                        .border(1.dp, StudioBorder, MaterialTheme.shapes.medium),
                ) {
                    // Live Studio Waveform & Timecode Canvas Monitor
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val midY = size.height / 2f
                        val bars = 36
                        val step = size.width / bars
                        val progressRatio = (playheadMs.toFloat() / totalMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        for (i in 0 until bars) {
                            val norm = ((i * 7 + (playheadMs / 200).toInt()) % 11) / 11f
                            val barH = size.height * (0.14f + 0.48f * norm)
                            val x = i * step + step * 0.3f
                            val active = (i.toFloat() / bars) <= progressRatio
                            drawLine(
                                color = if (active) IndigoLight else StudioSurfaceHigh,
                                start = Offset(x, midY - barH / 2f),
                                end = Offset(x, midY + barH / 2f),
                                strokeWidth = step * 0.45f,
                            )
                        }
                        val needleX = size.width * progressRatio
                        drawLine(
                            color = EmeraldSuccess,
                            start = Offset(needleX, 0f),
                            end = Offset(needleX, size.height),
                            strokeWidth = 3f,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(lutTint, Color.Transparent)))
                    )

                    // HUD overlays
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Surface(color = ObsidianBg.copy(alpha = 0.78f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "${project.resolution} • ${project.lutPreset}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMain,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        Surface(color = ObsidianBg.copy(alpha = 0.78f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = formatTimecode(playheadMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    if (!activeCaption.isNullOrBlank()) {
                        Surface(
                            color = ObsidianBg.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                        ) {
                            Text(
                                text = activeCaption,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMain,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = onTogglePlay, modifier = Modifier.testTag("timeline_play_pause_button")) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = IndigoLight,
                        )
                    }
                    IconButton(onClick = onReset) {
                        Icon(Icons.Filled.Replay, contentDescription = "Rewind", tint = TextMuted)
                    }
                    Slider(
                        value = playheadMs.toFloat(),
                        onValueChange = { onScrub(it.toLong()) },
                        valueRange = 0f..totalMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldSuccess,
                            activeTrackColor = IndigoPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatTimecode(totalMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTimelineClipDialog(
    defaultStartMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Double, String) -> Unit,
) {
    var trackType by remember { mutableStateOf("video") }
    var label by remember { mutableStateOf("") }
    var durationSec by remember { mutableFloatStateOf(6f) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        title = { Text("Add Timeline Layer (${formatTimecode(defaultStartMs)})", color = TextMain) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("video", "vfx", "audio", "caption").forEach { t ->
                        FilterChip(
                            selected = trackType == t,
                            onClick = { trackType = t },
                            label = { Text(t.uppercase()) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary),
                        )
                    }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Layer Label / Caption") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Duration: ${durationSec.toInt()}s", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Slider(
                    value = durationSec,
                    onValueChange = { durationSec = it },
                    valueRange = 1f..30f,
                )
                Text("Speed Ramp: ${String.format("%.2f", speed)}x", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.25f..4.0f,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Keyframe / Transition Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(trackType, label, (durationSec * 1000).toLong(), speed.toDouble(), notes)
                },
                enabled = label.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            ) {
                Text("Insert Clip")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EditClipDialog(
    clip: TimelineClip,
    onDismiss: () -> Unit,
    onPreviewClip: (String) -> Unit,
    onSave: (Long, Double, String) -> Unit,
    onDelete: () -> Unit,
) {
    var durationSec by remember { mutableFloatStateOf((clip.durationMs / 1000f).coerceIn(1f, 60f)) }
    var speed by remember { mutableFloatStateOf(clip.speedMultiplier.toFloat().coerceIn(0.25f, 4f)) }
    var notes by remember { mutableStateOf(clip.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        title = { Text("Edit ${clip.label}", color = TextMain) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (clip.mediaUri.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onPreviewClip(clip.mediaUri) },
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preview_clip_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load & Preview in VideoPlayer", color = CyanAccent)
                    }
                }
                Text("Ripple Trim Duration: ${durationSec.toInt()}s", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Slider(value = durationSec, onValueChange = { durationSec = it }, valueRange = 1f..60f)
                Text("Speed Ramp: ${String.format("%.2f", speed)}x", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.25f..4.0f)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Layer Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave((durationSec * 1000).toLong(), speed.toDouble(), notes) },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete clip", tint = RoseErrorFallback)
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

@Composable
private fun CloudVideoUrlDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onLoadStream: (url: String, label: String, saveToTimeline: Boolean) -> Unit,
) {
    var streamUrl by remember { mutableStateOf(initialUrl) }
    var clipLabel by remember { mutableStateOf("") }
    var saveToTimeline by remember { mutableStateOf(true) }

    val trimmedUrl = streamUrl.trim()
    val isValidUrl = trimmedUrl.startsWith("https://", ignoreCase = true) ||
        trimmedUrl.startsWith("http://", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        containerColor = StudioSurfaceElevated,
        title = { Text("Stream Cloud Video File", color = TextMain) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter a direct cloud video or adaptive stream URL (MP4, WebM, HLS .m3u8, or DASH .mpd).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { streamUrl = it },
                    label = { Text("Cloud Video URL") },
                    placeholder = { Text("https://cdn.example.com/video.mp4") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cloud_video_url_input"),
                )
                OutlinedTextField(
                    value = clipLabel,
                    onValueChange = { clipLabel = it },
                    label = { Text("Clip Title (optional)") },
                    placeholder = { Text("e.g. A-Roll Cloud Proxy") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cloud_video_label_input"),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Save to V1 timeline track",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMain,
                    )
                    Switch(
                        checked = saveToTimeline,
                        onCheckedChange = { saveToTimeline = it },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLoadStream(trimmedUrl, clipLabel.trim(), saveToTimeline) },
                enabled = isValidUrl,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.testTag("confirm_load_cloud_video"),
            ) {
                Text("Preview Stream")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private val RoseErrorFallback = Color(0xFFF43F5E)

private fun formatTimecode(ms: Long): String {
    val totalSec = ms / 1000
    val mins = totalSec / 60
    val secs = totalSec % 60
    val tenths = (ms % 1000) / 100
    return String.format("%02d:%02d.%d", mins, secs, tenths)
}
