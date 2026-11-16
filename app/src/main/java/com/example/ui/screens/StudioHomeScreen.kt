package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.RenderJob
import com.example.data.model.StudioProject
import com.example.ui.components.CloudVideoPreviewDialog
import com.example.ui.components.VideoPlayer
import com.example.ui.theme.AmberWarning
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
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.UiState

@Composable
fun StudioHomeScreen(
    userName: String,
    projectsState: UiState<List<StudioProject>>,
    renderJobsState: UiState<List<RenderJob>>,
    selectedProjectId: String?,
    onOpenProject: (StudioProject) -> Unit,
    onCreateProject: (title: String, resolution: String, aspectRatio: String, lutPreset: String, proxyEnabled: Boolean) -> Unit,
    onCreateStarterCut: () -> Unit,
    onDispatchRender: (StudioProject, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onOpenAiLab: () -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCloudPlayerDialog by remember { mutableStateOf(false) }
    var renderTargetProject by remember { mutableStateOf<StudioProject?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            StudioHeroCard(
                userName = userName,
                onNewProjectClick = { showCreateDialog = true },
                onOpenAiLab = onOpenAiLab,
            )
        }

        // Cloud Video Player Quick Preview Bar
        item {
            Surface(
                color = StudioSurface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            color = CyanAccent.copy(alpha = 0.16f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Cloud,
                                    contentDescription = "Cloud Video Player",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cloud Media3 Player",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextMain,
                            )
                            Text(
                                text = "Stream & scrub remote MP4, HLS, or DASH files",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { showCloudPlayerDialog = true },
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("cloud_video_preview_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", color = CyanAccent)
                    }
                }
            }
        }

        // Cloud & GPU Render Queue Strip
        if (renderJobsState is UiState.Success && renderJobsState.data.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "Cloud GPU Queue",
                                tint = AmberWarning,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Cloud & GPU Render Queue",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextMain,
                            )
                        }
                        Text(
                            text = "${renderJobsState.data.size} jobs",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(renderJobsState.data.take(6), key = { it.id }) { job ->
                            RenderJobCard(job = job)
                        }
                    }
                }
            }
        }

        // Projects Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Cloud Vault Projects",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextMain,
                    )
                    Text(
                        text = "Synced multi-track timelines & 4K/8K proxies",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                OutlinedButton(
                    onClick = { showCreateDialog = true },
                    border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("new_project_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New Project",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Cut")
                }
            }
        }

        when (projectsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = IndigoPrimary)
                    }
                }
            }

            is UiState.Error -> {
                item {
                    Surface(
                        color = RoseError.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, RoseError.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = projectsState.message,
                            color = RoseError,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            is UiState.Success -> {
                val projects = projectsState.data
                if (projects.isEmpty()) {
                    item {
                        EmptyProjectsCard(
                            onCreateProject = { showCreateDialog = true },
                            onCreateStarterCut = onCreateStarterCut,
                        )
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        StudioProjectCard(
                            project = project,
                            isSelected = project.id == selectedProjectId,
                            onOpenTimeline = { onOpenProject(project) },
                            onDispatchRender = { renderTargetProject = project },
                            onDelete = { onDeleteProject(project.id) },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, res, aspect, lut, proxy ->
                showCreateDialog = false
                onCreateProject(title, res, aspect, lut, proxy)
            },
        )
    }

    renderTargetProject?.let { target ->
        DispatchRenderDialog(
            project = target,
            onDismiss = { renderTargetProject = null },
            onConfirm = { profile ->
                renderTargetProject = null
                onDispatchRender(target, profile)
            },
        )
    }

    if (showCloudPlayerDialog) {
        CloudStreamPreviewModal(
            onDismiss = { showCloudPlayerDialog = false },
        )
    }
}

@Composable
private fun StudioHeroCard(
    userName: String,
    onNewProjectClick: () -> Unit,
    onOpenAiLab: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            Image(
                painter = painterResource(id = R.drawable.img_studio_hero),
                contentDescription = "SuSa studio workspace",
                contentScale = ContentScale.Crop,
                alpha = 0.35f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ObsidianBg.copy(alpha = 0.25f),
                                StudioSurface.copy(alpha = 0.94f),
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess)
                            )
                            Text(
                                text = "CLOUD VAULT SYNCED",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess,
                            )
                        }
                    }
                    Text(
                        text = "AES-256 • TLS 1.3",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }

                Text(
                    text = "Welcome back, ${userName.ifBlank { "Creator" }}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextMain,
                )
                Text(
                    text = "Launch a 4K/8K cloud timeline, run Gemini AI auto-cuts, or scout your next shoot on Google Maps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Button(
                        onClick = onNewProjectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.testTag("hero_new_project_button"),
                    ) {
                        Icon(Icons.Filled.MovieCreation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Project")
                    }
                    OutlinedButton(
                        onClick = onOpenAiLab,
                        border = BorderStroke(1.dp, VioletAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("hero_ai_lab_button"),
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = VioletAccent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Personal AI", color = VioletAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderJobCard(job: RenderJob) {
    val accent = when (job.status) {
        "completed" -> EmeraldSuccess
        "rendering" -> IndigoLight
        else -> AmberWarning
    }
    Surface(
        color = StudioSurfaceElevated,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.width(230.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = job.projectTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${job.progressPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
            Text(
                text = job.profile,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
            )
            LinearProgressIndicator(
                progress = { (job.progressPercent / 100f).coerceIn(0f, 1f) },
                color = accent,
                trackColor = StudioBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
            Text(
                text = job.status.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
    }
}

@Composable
private fun StudioProjectCard(
    project: StudioProject,
    isSelected: Boolean,
    onOpenTimeline: () -> Unit,
    onDispatchRender: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = StudioSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) IndigoPrimary else StudioBorder,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenTimeline)
            .testTag("project_card_${project.id}"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${project.resolution} • ${project.aspectRatio} • LUT: ${project.lutPreset}",
                        style = MaterialTheme.typography.labelMedium,
                        color = IndigoLight,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete project",
                        tint = TextMuted,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusTag(
                    label = if (project.proxyEnabled) "PROXY 4K ON" else "FULL RAW",
                    color = if (project.proxyEnabled) EmeraldSuccess else AmberWarning,
                )
                StatusTag(
                    label = project.status.uppercase(),
                    color = if (project.status == "ready") EmeraldSuccess else VioletAccent,
                )
                if (project.shootLocationName.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = "Shoot location",
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = project.shootLocationName,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onOpenTimeline,
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Timeline")
                }
                OutlinedButton(
                    onClick = onDispatchRender,
                    border = BorderStroke(1.dp, StudioBorder),
                ) {
                    Icon(Icons.Filled.CloudDone, contentDescription = "Cloud Render", tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Render", color = TextMain)
                }
            }
        }
    }
}

@Composable
private fun StatusTag(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EmptyProjectsCard(
    onCreateProject: () -> Unit,
    onCreateStarterCut: () -> Unit,
) {
    Surface(
        color = StudioSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_timeline),
                contentDescription = "Empty studio timeline illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(MaterialTheme.shapes.medium),
            )
            Text(
                text = "Your Cloud Vault is Ready",
                style = MaterialTheme.typography.titleLarge,
                color = TextMain,
            )
            Text(
                text = "Create a custom 4K/8K editing project or load a multi-track starter sequence with LUTs, audio stems, and a Tokyo shoot pin.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onCreateStarterCut,
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("starter_cut_button"),
                ) {
                    Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Load Starter Cut")
                }
                OutlinedButton(
                    onClick = onCreateProject,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Blank Project")
                }
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Boolean) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var resolution by remember { mutableStateOf("4K 60fps") }
    var aspectRatio by remember { mutableStateOf("16:9") }
    var lutPreset by remember { mutableStateOf("Obsidian Cinema") }
    var proxyEnabled by remember { mutableStateOf(true) }

    val resolutions = listOf("4K 60fps", "8K RAW", "9:16 Reels", "1080p 60fps")
    val ratios = listOf("16:9", "9:16", "2.39:1", "1:1")
    val luts = listOf("Obsidian Cinema", "Cyber Indigo", "Teal & Amber", "Noir Monochrome")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        title = { Text("New SuSa Cloud Project", color = TextMain) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title") },
                    placeholder = { Text("e.g. Cyberpunk Docu-Cut") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_title_input"),
                )
                Text("Resolution & Frame Rate", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(resolutions) { res ->
                        FilterChip(
                            selected = resolution == res,
                            onClick = { resolution = res },
                            label = { Text(res) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary),
                        )
                    }
                }
                Text("Aspect Ratio", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ratios) { ratio ->
                        FilterChip(
                            selected = aspectRatio == ratio,
                            onClick = { aspectRatio = ratio },
                            label = { Text(ratio) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary),
                        )
                    }
                }
                Text("Color LUT", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(luts) { lut ->
                        FilterChip(
                            selected = lutPreset == lut,
                            onClick = { lutPreset = lut },
                            label = { Text(lut) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("4K/8K Proxy Workflow", style = MaterialTheme.typography.bodyMedium, color = TextMain)
                    Switch(checked = proxyEnabled, onCheckedChange = { proxyEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, resolution, aspectRatio, lutPreset, proxyEnabled) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.testTag("confirm_create_project"),
            ) {
                Text("Create & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DispatchRenderDialog(
    project: StudioProject,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val profiles = listOf(
        "YouTube 4K H.265",
        "Reels 9:16 60fps",
        "ProRes 422 HQ",
        "DNxHD Cinema",
    )
    var selectedProfile by remember { mutableStateOf(profiles.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        title = { Text("Cloud GPU Render Pipeline", color = TextMain) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Export \"${project.title}\" (${project.lutPreset})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                profiles.forEach { profile ->
                    Surface(
                        color = if (selectedProfile == profile) IndigoPrimary.copy(alpha = 0.2f) else StudioSurface,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(
                            1.dp,
                            if (selectedProfile == profile) IndigoPrimary else StudioBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProfile = profile },
                    ) {
                        Text(
                            text = profile,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMain,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedProfile) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = ObsidianBg),
                modifier = Modifier.testTag("confirm_dispatch_render"),
            ) {
                Text("Start GPU Render", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun CloudStreamPreviewModal(
    onDismiss: () -> Unit,
) {
    var draftUrl by remember { mutableStateOf("") }
    var activeStreamUrl by remember { mutableStateOf<String?>(null) }
    var fullscreenExpanded by remember { mutableStateOf(false) }

    val trimmed = draftUrl.trim()
    val canLoad = trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("http://", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        containerColor = StudioSurfaceElevated,
        title = { Text("Cloud Video Preview", color = TextMain) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draftUrl,
                    onValueChange = { draftUrl = it },
                    label = { Text("Cloud Stream URL") },
                    placeholder = { Text("https://cdn.example.com/clip.mp4") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_cloud_url_input"),
                )
                Button(
                    onClick = { activeStreamUrl = trimmed },
                    enabled = canLoad,
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_load_stream_button"),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Load Cloud Stream")
                }
                VideoPlayer(
                    videoUrl = activeStreamUrl,
                    title = "Cloud Vault Stream",
                    subtitle = "Media3 ExoPlayer",
                    autoPlay = true,
                    onFullscreenToggle = if (activeStreamUrl != null) {
                        { fullscreenExpanded = true }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )

    val stream = activeStreamUrl
    if (fullscreenExpanded && !stream.isNullOrBlank()) {
        CloudVideoPreviewDialog(
            videoUrl = stream,
            title = "Cloud Vault Stream",
            subtitle = "Media3 ExoPlayer",
            onDismiss = { fullscreenExpanded = false },
        )
    }
}
