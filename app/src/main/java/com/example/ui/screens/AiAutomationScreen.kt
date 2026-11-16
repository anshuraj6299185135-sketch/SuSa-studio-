package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AiCaptionLine
import com.example.data.model.AiChatMessage
import com.example.data.model.AiChatThread
import com.example.data.model.AiLocationSuggestion
import com.example.data.model.AiStoryboardResult
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
import com.example.ui.viewmodel.AiTaskState
import com.example.ui.viewmodel.UiState

private enum class AiToolTab(val label: String) {
    STORYBOARD("Script-to-Video"),
    CAPTIONS("Auto-Captions"),
    MAGIC_CUT("Magic Cut"),
    SCOUT("Location Scout"),
}

@Composable
fun AiAutomationScreen(
    userName: String,
    activeMode: String,
    onModeChange: (String) -> Unit,
    threadsState: UiState<List<AiChatThread>>,
    selectedThreadId: String?,
    messagesState: UiState<List<AiChatMessage>>,
    streamingText: String?,
    isGenerating: Boolean,
    chatError: String?,
    selectedModel: String,
    customInstructions: String,
    onSelectThread: (AiChatThread) -> Unit,
    onNewChat: () -> Unit,
    onRenameThread: (String, String) -> Unit,
    onDeleteThread: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onSaveInstructions: (String) -> Unit,
    onSendMessage: (String, Bitmap?) -> Unit,
    onStopGeneration: () -> Unit,
    onRegenerateLast: () -> Unit,
    onDismissChatError: () -> Unit,
    onCopyFeedback: (String) -> Unit,
    storyboardState: AiTaskState<AiStoryboardResult>,
    captionsState: AiTaskState<List<AiCaptionLine>>,
    magicCutState: AiTaskState<String>,
    scoutState: AiTaskState<List<AiLocationSuggestion>>,
    onGenerateStoryboard: (String) -> Unit,
    onApplyStoryboardToTimeline: (AiStoryboardResult) -> Unit,
    onGenerateCaptions: (String, String) -> Unit,
    onApplyCaptionsToTimeline: (List<AiCaptionLine>) -> Unit,
    onStreamMagicCut: (String) -> Unit,
    onScoutLocations: (String) -> Unit,
    onPinScoutedLocation: (AiLocationSuggestion) -> Unit,
) {
    var selectedTool by remember { mutableStateOf(AiToolTab.STORYBOARD) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
    ) {
        // Mode Switcher Strip: Personal AI (ChatGPT-style) vs Studio Video AI Tools
        Surface(
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorder),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ModePillButton(
                    label = "Personal AI",
                    icon = Icons.Filled.Forum,
                    selected = activeMode == "chat",
                    onClick = { onModeChange("chat") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_personal_ai"),
                )
                ModePillButton(
                    label = "Video AI Tools",
                    icon = Icons.Filled.AutoAwesome,
                    selected = activeMode != "chat",
                    onClick = { onModeChange("tools") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_video_tools"),
                )
            }
        }

        if (activeMode == "chat") {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                PersonalAiChatScreen(
                    userName = userName,
                    threadsState = threadsState,
                    selectedThreadId = selectedThreadId,
                    messagesState = messagesState,
                    streamingText = streamingText,
                    isGenerating = isGenerating,
                    chatError = chatError,
                    selectedModel = selectedModel,
                    customInstructions = customInstructions,
                    onSelectThread = onSelectThread,
                    onNewChat = onNewChat,
                    onRenameThread = onRenameThread,
                    onDeleteThread = onDeleteThread,
                    onSelectModel = onSelectModel,
                    onSaveInstructions = onSaveInstructions,
                    onSendMessage = onSendMessage,
                    onStopGeneration = onStopGeneration,
                    onRegenerateLast = onRegenerateLast,
                    onDismissError = onDismissChatError,
                    onCopyFeedback = onCopyFeedback,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(ObsidianBg),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = VioletAccent)
                            Text("AI Automation Studio", style = MaterialTheme.typography.headlineSmall, color = TextMain)
                        }
                        Text(
                            text = "Powered by Firebase AI Logic • Gemini 3.8 Flash",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AiToolTab.entries) { tab ->
                            FilterChip(
                                selected = selectedTool == tab,
                                onClick = { selectedTool = tab },
                                label = { Text(tab.label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary),
                                modifier = Modifier.testTag("ai_tab_${tab.name.lowercase()}"),
                            )
                        }
                    }
                }

                when (selectedTool) {
                    AiToolTab.STORYBOARD -> item {
                        StoryboardSection(
                            state = storyboardState,
                            onGenerate = onGenerateStoryboard,
                            onApplyToTimeline = onApplyStoryboardToTimeline,
                        )
                    }

                    AiToolTab.CAPTIONS -> item {
                        CaptionsSection(
                            state = captionsState,
                            onGenerate = onGenerateCaptions,
                            onApplyToTimeline = onApplyCaptionsToTimeline,
                        )
                    }

                    AiToolTab.MAGIC_CUT -> item {
                        MagicCutSection(
                            state = magicCutState,
                            onStream = onStreamMagicCut,
                        )
                    }

                    AiToolTab.SCOUT -> item {
                        LocationScoutSection(
                            state = scoutState,
                            onScout = onScoutLocations,
                            onPinLocation = onPinScoutedLocation,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModePillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) IndigoPrimary else StudioSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) IndigoLight else StudioBorder),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) TextMain else TextMuted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) TextMain else TextMuted,
            )
        }
    }
}

@Composable
private fun StoryboardSection(
    state: AiTaskState<AiStoryboardResult>,
    onGenerate: (String) -> Unit,
    onApplyToTimeline: (AiStoryboardResult) -> Unit,
) {
    var prompt by remember { mutableStateOf("30s sleek electric supercar night launch through rain-soaked Tokyo streets") }

    Surface(
        color = StudioSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.MovieFilter, contentDescription = null, tint = IndigoLight)
                Text("Script-to-Video Storyboard", style = MaterialTheme.typography.titleMedium, color = TextMain)
            }
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Describe your video concept") },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_storyboard_input"),
            )
            Button(
                onClick = { onGenerate(prompt) },
                enabled = prompt.isNotBlank() && state !is AiTaskState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_storyboard_generate_button"),
            ) {
                if (state is AiTaskState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextMain, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating Storyboard...")
                } else {
                    Text("Generate Multi-Track Storyboard")
                }
            }

            when (state) {
                is AiTaskState.Error -> AiErrorBanner(state.message)
                is AiTaskState.Success -> {
                    val res = state.data
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(res.conceptSummary, style = MaterialTheme.typography.bodyMedium, color = TextMain)
                            Text("Recommended LUT: ${res.recommendedLut}", style = MaterialTheme.typography.labelSmall, color = VioletAccent)
                            res.scenes.forEach { scene ->
                                Surface(
                                    color = ObsidianBg,
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, StudioBorder),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = "${scene.sceneNumber}. ${scene.title}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = IndigoLight,
                                            )
                                            Text(
                                                text = "${scene.durationSeconds}s • ${scene.trackType.uppercase()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldSuccess,
                                            )
                                        }
                                        Text(scene.cameraNote, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                        Text("VO: \"${scene.voiceover}\"", style = MaterialTheme.typography.bodySmall, color = TextMain)
                                    }
                                }
                            }
                            Button(
                                onClick = { onApplyToTimeline(res) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = ObsidianBg),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_storyboard_button"),
                            ) {
                                Text("Apply Scenes to Active Timeline", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun CaptionsSection(
    state: AiTaskState<List<AiCaptionLine>>,
    onGenerate: (String, String) -> Unit,
    onApplyToTimeline: (List<AiCaptionLine>) -> Unit,
) {
    var transcript by remember {
        mutableStateOf("Welcome to SuSa studio. Today we are cutting a 4K sequence in the cloud with zero proxy lag.")
    }
    var language by remember { mutableStateOf("Spanish") }
    val languages = listOf("English", "Spanish", "Hindi", "Japanese", "French")

    Surface(
        color = StudioSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.ClosedCaption, contentDescription = null, tint = CyanAccent)
                Text("AI Auto-Captions & Subtitles", style = MaterialTheme.typography.titleMedium, color = TextMain)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(languages) { lang ->
                    FilterChip(
                        selected = language == lang,
                        onClick = { language = lang },
                        label = { Text(lang) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary),
                    )
                }
            }
            OutlinedTextField(
                value = transcript,
                onValueChange = { transcript = it },
                label = { Text("Dialogue / Transcript") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onGenerate(transcript, language) },
                enabled = transcript.isNotBlank() && state !is AiTaskState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state is AiTaskState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextMain, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Translating & Timing...")
                } else {
                    Text("Generate $language Subtitles")
                }
            }

            when (state) {
                is AiTaskState.Error -> AiErrorBanner(state.message)
                is AiTaskState.Success -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.data.forEach { line ->
                            Surface(
                                color = StudioSurfaceElevated,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        text = "${line.startTime}-${line.endTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent,
                                    )
                                    Text(
                                        text = line.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMain,
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = { onApplyToTimeline(state.data) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = ObsidianBg),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Insert onto CC Track", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun MagicCutSection(
    state: AiTaskState<String>,
    onStream: (String) -> Unit,
) {
    var notes by remember { mutableStateOf("14-minute talking-head tech review with long pauses, handheld B-roll, and street background noise") }

    Surface(
        color = StudioSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.ContentCut, contentDescription = null, tint = EmeraldSuccess)
                Text("Smart Magic Cut & Voice Isolation", style = MaterialTheme.typography.titleMedium, color = TextMain)
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Raw footage & pacing brief") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onStream(notes) },
                enabled = notes.isNotBlank() && state !is AiTaskState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state is AiTaskState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextMain, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing Footage...")
                } else {
                    Text("Stream Magic Cut Plan")
                }
            }

            when (state) {
                is AiTaskState.Error -> AiErrorBanner(state.message)
                is AiTaskState.Streaming -> StreamingPlanCard(state.text)
                is AiTaskState.Success -> StreamingPlanCard(state.data)
                else -> Unit
            }
        }
    }
}

@Composable
private fun StreamingPlanCard(text: String) {
    Surface(
        color = StudioSurfaceElevated,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMain,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
private fun LocationScoutSection(
    state: AiTaskState<List<AiLocationSuggestion>>,
    onScout: (String) -> Unit,
    onPinLocation: (AiLocationSuggestion) -> Unit,
) {
    var mood by remember { mutableStateOf("Rainy cyberpunk neon alleyways and futuristic skyline viewpoints") }

    Surface(
        color = StudioSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = VioletAccent)
                Text("AI Shoot Location Scout", style = MaterialTheme.typography.titleMedium, color = TextMain)
            }
            OutlinedTextField(
                value = mood,
                onValueChange = { mood = it },
                label = { Text("Scene mood, architecture, or lighting style") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onScout(mood) },
                enabled = mood.isNotBlank() && state !is AiTaskState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state is AiTaskState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextMain, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scouting Coordinates...")
                } else {
                    Text("Scout Locations with Gemini")
                }
            }

            when (state) {
                is AiTaskState.Error -> AiErrorBanner(state.message)
                is AiTaskState.Success -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.data.forEach { loc ->
                            Surface(
                                color = StudioSurfaceElevated,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(loc.name, style = MaterialTheme.typography.titleSmall, color = TextMain)
                                        Text(loc.category, style = MaterialTheme.typography.labelSmall, color = CyanAccent)
                                    }
                                    Text(
                                        text = "${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IndigoLight,
                                    )
                                    Text(loc.scoutingNotes, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    OutlinedButton(
                                        onClick = { onPinLocation(loc) },
                                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                                    ) {
                                        Icon(Icons.Filled.Place, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pin to Shoot Map", color = EmeraldSuccess)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun AiErrorBanner(message: String) {
    Surface(
        color = RoseError.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, RoseError.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = RoseError,
            modifier = Modifier.padding(12.dp),
        )
    }
}
