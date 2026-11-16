package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AiChatMessage
import com.example.data.model.AiChatThread
import com.example.ui.components.ChatMarkdownText
import com.example.ui.components.copyTextToClipboard
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoseError
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderStrong
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHigh
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class StarterPrompt(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val icon: ImageVector,
)

private val DefaultStarterPrompts = listOf(
    StarterPrompt(
        title = "Plan my day",
        subtitle = "Prioritize tasks & schedule",
        prompt = "Help me build a focused, realistic plan for today with top 3 priorities and deep-work blocks.",
        icon = Icons.Filled.Lightbulb,
    ),
    StarterPrompt(
        title = "Write clean code",
        subtitle = "Debug, refactor, explain",
        prompt = "Write a clean Kotlin coroutine helper with exponential backoff retry and explain how it works.",
        icon = Icons.Filled.Code,
    ),
    StarterPrompt(
        title = "Explain simply",
        subtitle = "Break down tough topics",
        prompt = "Explain how large language models generate text token by token in clear, everyday terms.",
        icon = Icons.Filled.School,
    ),
    StarterPrompt(
        title = "Video hook & script",
        subtitle = "Creative studio brainstorm",
        prompt = "Write 3 attention-grabbing 15-second video hooks and shot ideas for a tech product showcase.",
        icon = Icons.Filled.MovieCreation,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalAiChatScreen(
    userName: String,
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
    onDismissError: () -> Unit,
    onCopyFeedback: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }
    var threadToRename by remember { mutableStateOf<AiChatThread?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = decodeScaledBitmap(context, uri)
                attachedBitmap = bmp
            }
        }
    }

    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    val threads = (threadsState as? UiState.Success)?.data.orEmpty()
    val activeThread = threads.firstOrNull { it.id == selectedThreadId }

    LaunchedEffect(messages.size, streamingText?.length) {
        val totalItems = messages.size + (if (streamingText != null) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .imePadding()
    ) {
        // Chat Header Bar (History, Model Switcher, New Chat, Instructions)
        Surface(
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorder),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = { showHistorySheet = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("ai_chat_history_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "Chat history",
                        tint = TextMain,
                    )
                }

                Box {
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, StudioBorderStrong),
                        modifier = Modifier
                            .clickable { showModelMenu = true }
                            .testTag("ai_model_selector"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = VioletAccent,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (selectedModel == "gemini-3.5-flash-lite") {
                                    "SuSa AI • 3.5 Lite"
                                } else {
                                    "SuSa AI • 3.8 Flash"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = TextMain,
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false },
                        containerColor = StudioSurfaceElevated,
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Gemini 3.8 Flash (Default)", color = TextMain, fontWeight = FontWeight.SemiBold)
                                    Text("Best balance for reasoning, code & vision", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            },
                            onClick = {
                                onSelectModel("gemini-3.8-flash")
                                showModelMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Gemini 3.5 Flash-Lite", color = TextMain, fontWeight = FontWeight.SemiBold)
                                    Text("Ultra-fast everyday replies", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            },
                            onClick = {
                                onSelectModel("gemini-3.5-flash-lite")
                                showModelMenu = false
                            },
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showInstructionsDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("ai_instructions_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Personal instructions",
                            tint = if (customInstructions.isNotBlank()) EmeraldSuccess else TextMuted,
                        )
                    }
                    IconButton(
                        onClick = onNewChat,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("ai_new_chat_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "New chat",
                            tint = IndigoLight,
                        )
                    }
                }
            }
        }

        // Active Thread Subtitle Strip (if viewing saved thread)
        AnimatedVisibility(visible = activeThread != null) {
            if (activeThread != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioSurfaceElevated.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = activeThread.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (customInstructions.isNotBlank()) {
                        Text(
                            text = "Custom Persona Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                        )
                    }
                }
            }
        }

        // Message Feed or Welcome Empty State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (messagesState is UiState.Loading && selectedThreadId != null) {
                CircularProgressIndicator(
                    color = IndigoPrimary,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else if (messages.isEmpty() && streamingText == null) {
                EmptyChatWelcome(
                    userName = userName,
                    hasCustomInstructions = customInstructions.isNotBlank(),
                    onPickStarter = { prompt ->
                        onSendMessage(prompt, null)
                    },
                    onOpenInstructions = { showInstructionsDialog = true },
                )
            } else {
                val lastModelIndex = messages.indexOfLast { it.role == "model" }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ai_chat_message_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(messages.indices.toList(), key = { idx -> messages[idx].id.ifBlank { idx } }) { idx ->
                        val msg = messages[idx]
                        if (msg.role == "user") {
                            UserMessageBubble(message = msg)
                        } else {
                            AssistantMessageBlock(
                                text = msg.text,
                                isStreaming = false,
                                isLastReply = idx == lastModelIndex && !isGenerating,
                                onCopy = {
                                    copyTextToClipboard(context, "SuSa AI reply", msg.text)
                                    onCopyFeedback("Copied response to clipboard")
                                },
                                onRegenerate = onRegenerateLast,
                            )
                        }
                    }

                    if (streamingText != null) {
                        item(key = "streaming_assistant") {
                            AssistantMessageBlock(
                                text = streamingText.ifBlank { "Thinking…" },
                                isStreaming = true,
                                isLastReply = false,
                                onCopy = {},
                                onRegenerate = {},
                            )
                        }
                    }
                }
            }
        }

        // Inline Error Banner (Transparent AI error visibility)
        if (!chatError.isNullOrBlank()) {
            Surface(
                color = RoseError.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, RoseError.copy(alpha = 0.45f)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = chatError,
                        style = MaterialTheme.typography.bodySmall,
                        color = RoseError,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismissError, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss error", tint = RoseError, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Bottom ChatGPT-style Composer Bar
        Surface(
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorder),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val previewBitmap = attachedBitmap
                if (previewBitmap != null) {
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Attached image preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(MaterialTheme.shapes.small),
                            )
                            Text(
                                text = "Image attached for visual Q&A",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMain,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { attachedBitmap = null },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove image", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("ai_attach_image_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Attach photo",
                            tint = if (isGenerating) TextSubtle else IndigoLight,
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Ask anything…",
                                color = TextSubtle,
                            )
                        },
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = StudioSurfaceElevated,
                            unfocusedContainerColor = StudioSurfaceElevated,
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = StudioBorderStrong,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("ai_chat_input"),
                    )

                    if (isGenerating) {
                        Surface(
                            color = RoseError,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onStopGeneration() }
                                .testTag("ai_stop_button"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Stop,
                                    contentDescription = "Stop generating",
                                    tint = TextMain,
                                )
                            }
                        }
                    } else {
                        val canSend = inputText.isNotBlank() || attachedBitmap != null
                        Surface(
                            color = if (canSend) IndigoPrimary else StudioSurfaceHigh,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(enabled = canSend) {
                                    val promptToSend = inputText
                                    val bmpToSend = attachedBitmap
                                    inputText = ""
                                    attachedBitmap = null
                                    onSendMessage(promptToSend, bmpToSend)
                                }
                                .testTag("ai_send_button"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send message",
                                    tint = if (canSend) TextMain else TextSubtle,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Chat History Drawer Bottom Sheet
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = StudioSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Conversations", style = MaterialTheme.typography.titleLarge, color = TextMain)
                    Button(
                        onClick = {
                            onNewChat()
                            showHistorySheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Chat")
                    }
                }

                if (threads.isEmpty()) {
                    Text(
                        text = "No saved conversations yet. Start asking anything below!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(threads, key = { it.id }) { thread ->
                            val isSelected = thread.id == selectedThreadId
                            Surface(
                                color = if (isSelected) IndigoPrimary.copy(alpha = 0.16f) else StudioSurfaceElevated,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) IndigoPrimary else StudioBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectThread(thread)
                                        showHistorySheet = false
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = thread.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (thread.lastMessagePreview.isNotBlank()) {
                                            Text(
                                                text = thread.lastMessagePreview,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { threadToRename = thread },
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Rename chat", tint = TextMuted, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteThread(thread.id) },
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete chat", tint = RoseError, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Personal Instructions Dialog
    if (showInstructionsDialog) {
        var draft by remember(customInstructions) { mutableStateOf(customInstructions) }
        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            containerColor = StudioSurface,
            title = { Text("Personal AI Instructions", color = TextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Customize how SuSa AI responds to you (role, tone, preferred language, or context).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Custom instructions") },
                        placeholder = { Text("e.g., Be concise, prefer Kotlin examples, remember I edit 4K YouTube videos.") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveInstructions(draft)
                        showInstructionsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstructionsDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
        )
    }

    // Rename Thread Dialog
    val renaming = threadToRename
    if (renaming != null) {
        var newTitle by remember(renaming.id) { mutableStateOf(renaming.title) }
        AlertDialog(
            onDismissRequest = { threadToRename = null },
            containerColor = StudioSurface,
            title = { Text("Rename Conversation", color = TextMain) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameThread(renaming.id, newTitle)
                        threadToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { threadToRename = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
        )
    }
}

@Composable
private fun EmptyChatWelcome(
    userName: String,
    hasCustomInstructions: Boolean,
    onPickStarter: (String) -> Unit,
    onOpenInstructions: () -> Unit,
) {
    val firstName = userName.trim().substringBefore(" ").ifBlank { "Creator" }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(IndigoPrimary, VioletAccent, CyanAccent)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "SuSa Personal AI",
                    tint = TextMain,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "What can I help with, $firstName?",
                style = MaterialTheme.typography.headlineSmall,
                color = TextMain,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your personal AI assistant for questions, writing, code, image Q&A, and studio ideas.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            if (hasCustomInstructions) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenInstructions,
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Personal Persona Active", color = EmeraldSuccess)
                }
            }
        }

        items(DefaultStarterPrompts.chunked(2)) { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { starter ->
                    Surface(
                        color = StudioSurface,
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPickStarter(starter.prompt) }
                            .testTag("starter_${starter.title.lowercase().replace(' ', '_')}"),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = starter.icon,
                                contentDescription = null,
                                tint = IndigoLight,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = starter.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextMain,
                            )
                            Text(
                                text = starter.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(message: AiChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            color = IndigoPrimary.copy(alpha = 0.24f),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.45f)),
            modifier = Modifier.widthIn(max = 310.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (message.hasImage) {
                    Text(
                        text = "[Photo Attached]",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMain,
                )
            }
        }
    }
}

@Composable
private fun AssistantMessageBlock(
    text: String,
    isStreaming: Boolean,
    isLastReply: Boolean,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(IndigoPrimary, VioletAccent))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "SuSa AI",
                    tint = TextMain,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = "SuSa AI",
                style = MaterialTheme.typography.titleSmall,
                color = IndigoLight,
            )
            if (isStreaming) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.8.dp,
                    color = EmeraldSuccess,
                )
            }
        }

        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ChatMarkdownText(
                markdown = text,
                modifier = Modifier.padding(14.dp),
            )
        }

        if (!isStreaming) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy response",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (isLastReply) {
                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Regenerate response",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private suspend fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val maxDim = 1280
        val w = raw.width
        val h = raw.height
        if (w <= maxDim && h <= maxDim) {
            raw
        } else {
            val scale = maxDim.toFloat() / maxOf(w, h).toFloat()
            Bitmap.createScaledBitmap(
                raw,
                (w * scale).toInt().coerceAtLeast(1),
                (h * scale).toInt().coerceAtLeast(1),
                true,
            )
        }
    } catch (_: Exception) {
        null
    }
}
