package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiCaptionLine
import com.example.data.model.AiChatMessage
import com.example.data.model.AiChatThread
import com.example.data.model.AiLocationSuggestion
import com.example.data.model.AiStoryboardResult
import com.example.data.model.RenderJob
import com.example.data.model.ReviewComment
import com.example.data.model.SecurityAuditLog
import com.example.data.model.ShootLocation
import com.example.data.model.StudioProject
import com.example.data.model.TimelineClip
import com.example.data.repository.StudioAiRepository
import com.example.data.repository.StudioRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiTaskState<out T> {
    data object Idle : AiTaskState<Nothing>
    data object Loading : AiTaskState<Nothing>
    data class Streaming(val text: String) : AiTaskState<Nothing>
    data class Success<T>(val data: T) : AiTaskState<T>
    data class Error(val message: String) : AiTaskState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(
    private val repository: StudioRepository,
    private val aiRepository: StudioAiRepository,
    private val currentUserId: String,
    private val currentUserDisplayName: String,
) : ViewModel() {

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId: StateFlow<String?> = _selectedProjectId.asStateFlow()

    private val _selectedMediaUri = MutableStateFlow<String?>(null)
    val selectedMediaUri: StateFlow<String?> = _selectedMediaUri.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // --- Real-time Firestore Streams ---

    val projectsState: StateFlow<UiState<List<StudioProject>>> = repository.observeProjects()
        .asUiState("projects")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val renderJobsState: StateFlow<UiState<List<RenderJob>>> = repository.observeRenderJobs()
        .asUiState("renderJobs")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val locationsState: StateFlow<UiState<List<ShootLocation>>> = repository.observeShootLocations()
        .asUiState("locations")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val auditLogsState: StateFlow<UiState<List<SecurityAuditLog>>> = repository.observeAuditLogs()
        .asUiState("auditLogs")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val clipsState: StateFlow<UiState<List<TimelineClip>>> = _selectedProjectId
        .flatMapLatest { projectId ->
            if (projectId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeClips(projectId).asUiState("clips")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Success(emptyList()))

    val commentsState: StateFlow<UiState<List<ReviewComment>>> = _selectedProjectId
        .flatMapLatest { projectId ->
            if (projectId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeComments(projectId).asUiState("comments")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Success(emptyList()))

    // --- AI Lab States ---

    private val _storyboardState = MutableStateFlow<AiTaskState<AiStoryboardResult>>(AiTaskState.Idle)
    val storyboardState: StateFlow<AiTaskState<AiStoryboardResult>> = _storyboardState.asStateFlow()

    private val _captionsState = MutableStateFlow<AiTaskState<List<AiCaptionLine>>>(AiTaskState.Idle)
    val captionsState: StateFlow<AiTaskState<List<AiCaptionLine>>> = _captionsState.asStateFlow()

    private val _magicCutState = MutableStateFlow<AiTaskState<String>>(AiTaskState.Idle)
    val magicCutState: StateFlow<AiTaskState<String>> = _magicCutState.asStateFlow()

    private val _scoutLocationsState = MutableStateFlow<AiTaskState<List<AiLocationSuggestion>>>(AiTaskState.Idle)
    val scoutLocationsState: StateFlow<AiTaskState<List<AiLocationSuggestion>>> = _scoutLocationsState.asStateFlow()

    // --- Personal AI Assistant (ChatGPT-style) States ---

    private val _selectedThreadId = MutableStateFlow<String?>(null)
    val selectedThreadId: StateFlow<String?> = _selectedThreadId.asStateFlow()

    private val _selectedChatModel = MutableStateFlow("gemini-3.8-flash")
    val selectedChatModel: StateFlow<String> = _selectedChatModel.asStateFlow()

    private val _customInstructions = MutableStateFlow("")
    val customInstructions: StateFlow<String> = _customInstructions.asStateFlow()

    private val _chatStreamingText = MutableStateFlow<String?>(null)
    val chatStreamingText: StateFlow<String?> = _chatStreamingText.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private var activeChatJob: Job? = null

    val aiThreadsState: StateFlow<UiState<List<AiChatThread>>> = repository.observeAiThreads()
        .asUiState("aiThreads")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val aiMessagesState: StateFlow<UiState<List<AiChatMessage>>> = _selectedThreadId
        .flatMapLatest { threadId ->
            if (threadId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAiMessages(threadId).asUiState("aiMessages")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Success(emptyList()))

    // --- Selection & Media Actions ---

    fun selectProject(projectId: String) {
        _selectedProjectId.value = projectId
    }

    fun setSelectedMediaUri(uri: String?) {
        _selectedMediaUri.value = uri
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    // --- Project & Timeline Actions ---

    fun createProject(
        title: String,
        resolution: String,
        aspectRatio: String,
        lutPreset: String,
        proxyEnabled: Boolean,
        onCreated: (String) -> Unit = {},
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val id = repository.createProject(
                    title = title,
                    resolution = resolution,
                    aspectRatio = aspectRatio,
                    lutPreset = lutPreset,
                    proxyEnabled = proxyEnabled,
                )
                _selectedProjectId.value = id
                _statusBanner.value = "Project \"${title.trim()}\" synced to SuSa Cloud Vault."
                onCreated(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to create project: ${e.localizedMessage}"
            }
        }
    }

    fun createStarterCut(onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = repository.createProject(
                    title = "Tokyo Neon Commercial",
                    resolution = "4K 60fps",
                    aspectRatio = "16:9",
                    lutPreset = "Cyber Indigo",
                    proxyEnabled = true,
                    shootLocationName = "Shibuya Crossing, Tokyo",
                    shootLat = 35.6595,
                    shootLng = 139.7004,
                )
                repository.addClip(id, "video", "A-Roll Drone Establishing", 0L, 8000L, 1.5, "#6366F1", "", "Anamorphic 24mm • Speed Ramp 1.5x")
                repository.addClip(id, "video", "Street Motion Gimbal Cut", 8000L, 7000L, 1.0, "#818CF8", "", "Whip-pan transition in")
                repository.addClip(id, "vfx", "Cyber Indigo LUT + Light Leak", 2000L, 10000L, 1.0, "#C084FC", "", "3D title lower-third overlay")
                repository.addClip(id, "audio", "Synth Pulse Master Stem", 0L, 15000L, 1.0, "#10B981", "", "Voice isolation active • -14 LUFS")
                repository.addClip(id, "caption", "Future edits happen in the cloud.", 1500L, 4500L, 1.0, "#06B6D4", "", "Auto-synced EN subtitle")
                repository.saveShootLocation(
                    name = "Shibuya Crossing, Tokyo",
                    category = "Exterior",
                    latitude = 35.6595,
                    longitude = 139.7004,
                    scoutingNotes = "Blue hour neon reflections, 24mm f/1.8",
                    projectId = id,
                )
                _selectedProjectId.value = id
                _statusBanner.value = "Starter multi-track cut initialized."
                onCreated(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Could not create starter cut: ${e.localizedMessage}"
            }
        }
    }

    fun updateProjectSettings(
        project: StudioProject,
        lutPreset: String = project.lutPreset,
        proxyEnabled: Boolean = project.proxyEnabled,
    ) {
        viewModelScope.launch {
            try {
                repository.updateProjectSettings(
                    projectId = project.id,
                    lutPreset = lutPreset,
                    proxyEnabled = proxyEnabled,
                    status = project.status,
                    durationSeconds = project.durationSeconds,
                )
                _statusBanner.value = "Updated timeline preset: $lutPreset"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Update failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                repository.deleteProject(projectId)
                if (_selectedProjectId.value == projectId) {
                    _selectedProjectId.value = null
                }
                _statusBanner.value = "Project moved out of Cloud Vault."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Delete failed: ${e.localizedMessage}"
            }
        }
    }

    fun addClip(
        projectId: String,
        trackType: String,
        label: String,
        startMs: Long,
        durationMs: Long,
        speedMultiplier: Double,
        mediaUri: String = "",
        notes: String = "",
    ) {
        if (label.isBlank()) return
        val accent = when (trackType) {
            "audio" -> "#10B981"
            "vfx" -> "#C084FC"
            "caption" -> "#06B6D4"
            else -> "#6366F1"
        }
        viewModelScope.launch {
            try {
                repository.addClip(
                    projectId = projectId,
                    trackType = trackType,
                    label = label,
                    startMs = startMs,
                    durationMs = durationMs,
                    speedMultiplier = speedMultiplier,
                    colorHex = accent,
                    mediaUri = mediaUri,
                    notes = notes,
                )
                _statusBanner.value = "Added $trackType layer \"$label\""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to add clip: ${e.localizedMessage}"
            }
        }
    }

    fun updateClip(projectId: String, clip: TimelineClip, durationMs: Long, speedMultiplier: Double, notes: String) {
        viewModelScope.launch {
            try {
                repository.updateClip(projectId, clip.id, durationMs, speedMultiplier, notes)
                _statusBanner.value = "Clip \"${clip.label}\" updated."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to update clip: ${e.localizedMessage}"
            }
        }
    }

    fun deleteClip(projectId: String, clipId: String) {
        viewModelScope.launch {
            try {
                repository.deleteClip(projectId, clipId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to remove clip: ${e.localizedMessage}"
            }
        }
    }

    fun addReviewComment(projectId: String, timestampMs: Long, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            try {
                repository.addComment(
                    projectId = projectId,
                    authorName = currentUserDisplayName.ifBlank { "Studio Editor" },
                    timestampMs = timestampMs,
                    body = body,
                )
                _statusBanner.value = "Timestamped review note posted."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to post note: ${e.localizedMessage}"
            }
        }
    }

    fun toggleCommentResolved(projectId: String, comment: ReviewComment) {
        viewModelScope.launch {
            try {
                repository.toggleCommentResolved(projectId, comment.id, !comment.resolved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to update note: ${e.localizedMessage}"
            }
        }
    }

    // --- Cloud & GPU Render Queue ---

    fun dispatchCloudRender(project: StudioProject, profile: String) {
        viewModelScope.launch {
            try {
                val jobId = repository.enqueueRenderJob(project.id, project.title, profile)
                _statusBanner.value = "Cloud GPU render queued ($profile)"
                // Advance job state cleanly across Cloud GPU nodes
                delay(1200L)
                repository.updateRenderJobProgress(jobId, "rendering", 35L)
                delay(1400L)
                repository.updateRenderJobProgress(jobId, "rendering", 78L)
                delay(1400L)
                repository.updateRenderJobProgress(jobId, "completed", 100L)
                repository.updateProjectSettings(
                    projectId = project.id,
                    lutPreset = project.lutPreset,
                    proxyEnabled = project.proxyEnabled,
                    status = "ready",
                    durationSeconds = project.durationSeconds,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Render dispatch failed: ${e.localizedMessage}"
            }
        }
    }

    // --- Google Maps Shoot Location Actions ---

    fun saveShootLocation(
        name: String,
        category: String,
        latitude: Double,
        longitude: Double,
        scoutingNotes: String,
        attachProjectId: String? = null,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                repository.saveShootLocation(
                    name = name,
                    category = category,
                    latitude = latitude,
                    longitude = longitude,
                    scoutingNotes = scoutingNotes,
                    projectId = attachProjectId.orEmpty(),
                )
                if (!attachProjectId.isNullOrBlank()) {
                    repository.attachLocationToProject(attachProjectId, name, latitude, longitude)
                }
                _statusBanner.value = "Pinned \"$name\" to Shoot Map."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to pin location: ${e.localizedMessage}"
            }
        }
    }

    fun attachLocationToProject(projectId: String, location: ShootLocation) {
        viewModelScope.launch {
            try {
                repository.attachLocationToProject(
                    projectId = projectId,
                    locationName = location.name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                )
                _statusBanner.value = "Linked \"${location.name}\" to active project."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to link location: ${e.localizedMessage}"
            }
        }
    }

    fun deleteShootLocation(locationId: String) {
        viewModelScope.launch {
            try {
                repository.deleteShootLocation(locationId)
                _statusBanner.value = "Removed shoot pin."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to remove pin: ${e.localizedMessage}"
            }
        }
    }

    // --- Firebase AI Logic Actions ---

    fun generateStoryboard(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _storyboardState.value = AiTaskState.Loading
            try {
                val result = aiRepository.generateStoryboard(prompt)
                _storyboardState.value = AiTaskState.Success(result)
                repository.logAuditEvent("AI_STORYBOARD", "Generated ${result.scenes.size}-scene AI storyboard")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _storyboardState.value = AiTaskState.Error(e.localizedMessage ?: "Storyboard failed.")
            }
        }
    }

    fun applyStoryboardToActiveProject(result: AiStoryboardResult) {
        val projectId = _selectedProjectId.value
        viewModelScope.launch {
            try {
                val targetId = if (projectId.isNullOrBlank()) {
                    repository.createProject(
                        title = "AI Studio Cut",
                        resolution = "4K 60fps",
                        aspectRatio = "16:9",
                        lutPreset = result.recommendedLut,
                    )
                } else {
                    projectId
                }
                var cursorMs = 0L
                result.scenes.forEach { scene ->
                    val durMs = scene.durationSeconds * 1000L
                    val color = when (scene.trackType) {
                        "audio" -> "#10B981"
                        "vfx" -> "#C084FC"
                        else -> "#6366F1"
                    }
                    repository.addClip(
                        projectId = targetId,
                        trackType = scene.trackType,
                        label = scene.title,
                        startMs = cursorMs,
                        durationMs = durMs,
                        speedMultiplier = 1.0,
                        colorHex = color,
                        notes = "${scene.cameraNote} | VO: ${scene.voiceover}",
                    )
                    cursorMs += durMs
                }
                _selectedProjectId.value = targetId
                _statusBanner.value = "Inserted ${result.scenes.size} AI scenes onto timeline."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to apply scenes: ${e.localizedMessage}"
            }
        }
    }

    fun generateAutoCaptions(script: String, language: String) {
        if (script.isBlank()) return
        viewModelScope.launch {
            _captionsState.value = AiTaskState.Loading
            try {
                val lines = aiRepository.generateCaptions(script, language)
                _captionsState.value = AiTaskState.Success(lines)
                repository.logAuditEvent("AI_CAPTIONS", "Generated ${lines.size} $language subtitles")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _captionsState.value = AiTaskState.Error(e.localizedMessage ?: "Caption generation failed.")
            }
        }
    }

    fun applyCaptionsToActiveProject(lines: List<AiCaptionLine>) {
        val projectId = _selectedProjectId.value
        if (projectId.isNullOrBlank()) {
            _statusBanner.value = "Select or create a project first to attach captions."
            return
        }
        viewModelScope.launch {
            try {
                lines.forEach { line ->
                    repository.addClip(
                        projectId = projectId,
                        trackType = "caption",
                        label = line.text,
                        startMs = line.startMs,
                        durationMs = line.durationMs,
                        speedMultiplier = 1.0,
                        colorHex = "#06B6D4",
                        notes = "${line.startTime} → ${line.endTime}",
                    )
                }
                _statusBanner.value = "Added ${lines.size} AI subtitle blocks to CC track."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to insert captions: ${e.localizedMessage}"
            }
        }
    }

    fun streamMagicCutPlan(rawNotes: String) {
        if (rawNotes.isBlank()) return
        viewModelScope.launch {
            _magicCutState.value = AiTaskState.Loading
            var finalText = ""
            try {
                aiRepository.generateMagicCutPlanStream(rawNotes).collect { partial ->
                    finalText = partial
                    _magicCutState.value = AiTaskState.Streaming(partial)
                }
                if (finalText.isNotBlank()) {
                    _magicCutState.value = AiTaskState.Success(finalText)
                    repository.logAuditEvent("AI_MAGIC_CUT", "Generated smart magic cut & voice isolation plan")
                } else {
                    _magicCutState.value = AiTaskState.Error("AI returned an empty edit plan.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _magicCutState.value = AiTaskState.Error(e.localizedMessage ?: "Magic Cut failed.")
            }
        }
    }

    fun scoutShootLocations(moodPrompt: String) {
        if (moodPrompt.isBlank()) return
        viewModelScope.launch {
            _scoutLocationsState.value = AiTaskState.Loading
            try {
                val results = aiRepository.scoutShootLocations(moodPrompt)
                _scoutLocationsState.value = AiTaskState.Success(results)
                repository.logAuditEvent("AI_LOCATION_SCOUT", "Scouted ${results.size} shoot coordinates")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _scoutLocationsState.value = AiTaskState.Error(e.localizedMessage ?: "Location scout failed.")
            }
        }
    }

    fun logSignInEvent() {
        viewModelScope.launch {
            repository.logAuditEvent("SESSION_ACTIVE", "Verified Google Sign-In session (${currentUserId.take(8)}…)")
        }
    }

    // --- Personal AI Assistant Actions ---

    fun selectAiThread(thread: AiChatThread) {
        activeChatJob?.cancel()
        _isChatGenerating.value = false
        _chatStreamingText.value = null
        _chatError.value = null
        _selectedThreadId.value = thread.id
        _selectedChatModel.value = thread.modelName.ifBlank { "gemini-3.8-flash" }
        _customInstructions.value = thread.systemInstruction
    }

    fun startNewAiThread() {
        activeChatJob?.cancel()
        _isChatGenerating.value = false
        _chatStreamingText.value = null
        _chatError.value = null
        _selectedThreadId.value = null
    }

    fun setChatModel(modelName: String) {
        val safeModel = if (modelName == "gemini-3.5-flash-lite") modelName else "gemini-3.8-flash"
        _selectedChatModel.value = safeModel
        val activeId = _selectedThreadId.value
        if (!activeId.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    repository.updateAiThread(threadId = activeId, modelName = safeModel)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update thread model", e)
                }
            }
        }
    }

    fun updatePersonalInstructions(instructions: String) {
        val trimmed = instructions.trim().take(1000)
        _customInstructions.value = trimmed
        val activeId = _selectedThreadId.value
        if (!activeId.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    repository.updateAiThread(threadId = activeId, systemInstruction = trimmed)
                    _statusBanner.value = "Personal AI instructions saved."
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _statusBanner.value = "Failed to save instructions: ${e.localizedMessage}"
                }
            }
        } else {
            _statusBanner.value = "Personal AI instructions active for new chats."
        }
    }

    fun renameAiThread(threadId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            try {
                repository.updateAiThread(threadId = threadId, title = newTitle)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to rename chat: ${e.localizedMessage}"
            }
        }
    }

    fun deleteAiThread(threadId: String) {
        viewModelScope.launch {
            try {
                if (_selectedThreadId.value == threadId) {
                    activeChatJob?.cancel()
                    _isChatGenerating.value = false
                    _chatStreamingText.value = null
                    _selectedThreadId.value = null
                }
                repository.deleteAiThread(threadId)
                _statusBanner.value = "Conversation deleted."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to delete conversation: ${e.localizedMessage}"
            }
        }
    }

    fun dismissChatError() {
        _chatError.value = null
    }

    fun stopAiGeneration() {
        val partial = _chatStreamingText.value
        val activeId = _selectedThreadId.value
        activeChatJob?.cancel()
        activeChatJob = null
        _isChatGenerating.value = false
        _chatStreamingText.value = null
        if (!partial.isNullOrBlank() && !activeId.isNullOrBlank()) {
            val currentMessages = (aiMessagesState.value as? UiState.Success)?.data.orEmpty()
            val model = _selectedChatModel.value
            viewModelScope.launch {
                try {
                    repository.addAiMessage(activeId, "model", partial, false, model)
                    repository.updateAiThread(
                        threadId = activeId,
                        lastMessagePreview = partial.take(200),
                        messageCount = (currentMessages.size + 1).toLong(),
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Could not persist stopped partial reply", e)
                }
            }
        }
    }

    fun sendPersonalAiMessage(
        prompt: String,
        imageBitmap: Bitmap? = null,
    ) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank() && imageBitmap == null) return
        if (_isChatGenerating.value) return

        val effectivePrompt = cleanPrompt.ifBlank { "Analyze this image and describe what you see." }
        val modelName = _selectedChatModel.value
        val instructions = _customInstructions.value
        val existingMessages = (aiMessagesState.value as? UiState.Success)?.data.orEmpty()

        activeChatJob = viewModelScope.launch {
            _isChatGenerating.value = true
            _chatError.value = null
            _chatStreamingText.value = ""

            try {
                val threadId = _selectedThreadId.value ?: repository.createAiThread(
                    title = effectivePrompt.replace('\n', ' ').take(48),
                    modelName = modelName,
                    systemInstruction = instructions,
                    lastMessagePreview = effectivePrompt.take(200),
                ).also { newId ->
                    _selectedThreadId.value = newId
                    repository.logAuditEvent("AI_CHAT_NEW", "Started Personal AI thread")
                }

                repository.addAiMessage(
                    threadId = threadId,
                    role = "user",
                    text = effectivePrompt,
                    hasImage = imageBitmap != null,
                    modelName = modelName,
                )

                var finalReply = ""
                aiRepository.streamPersonalChatResponse(
                    history = existingMessages,
                    userPrompt = effectivePrompt,
                    imageBitmap = imageBitmap,
                    modelName = modelName,
                    systemInstruction = instructions,
                    userName = currentUserDisplayName,
                ).collect { accumulated ->
                    finalReply = accumulated
                    _chatStreamingText.value = accumulated
                }

                if (finalReply.isNotBlank()) {
                    repository.addAiMessage(
                        threadId = threadId,
                        role = "model",
                        text = finalReply,
                        hasImage = false,
                        modelName = modelName,
                    )
                    repository.updateAiThread(
                        threadId = threadId,
                        lastMessagePreview = finalReply.replace('\n', ' ').take(200),
                        messageCount = (existingMessages.size + 2).toLong(),
                    )
                } else {
                    _chatError.value = "SuSa AI returned an empty response."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Personal AI message failed", e)
                _chatError.value = e.localizedMessage ?: "Failed to generate response."
            } finally {
                _isChatGenerating.value = false
                _chatStreamingText.value = null
            }
        }
    }

    fun regenerateLastAiResponse() {
        if (_isChatGenerating.value) return
        val threadId = _selectedThreadId.value ?: return
        val messages = (aiMessagesState.value as? UiState.Success)?.data.orEmpty()
        val lastUserIndex = messages.indexOfLast { it.role == "user" }
        if (lastUserIndex < 0) return

        val lastUserMessage = messages[lastUserIndex]
        val trailingModelMessages = messages.drop(lastUserIndex + 1).filter { it.role == "model" }
        val historyBeforeTurn = messages.take(lastUserIndex)
        val modelName = _selectedChatModel.value
        val instructions = _customInstructions.value

        activeChatJob = viewModelScope.launch {
            _isChatGenerating.value = true
            _chatError.value = null
            _chatStreamingText.value = ""
            try {
                trailingModelMessages.forEach { oldModelMsg ->
                    repository.deleteAiMessage(threadId, oldModelMsg.id)
                }
                var finalReply = ""
                aiRepository.streamPersonalChatResponse(
                    history = historyBeforeTurn,
                    userPrompt = lastUserMessage.text,
                    imageBitmap = null,
                    modelName = modelName,
                    systemInstruction = instructions,
                    userName = currentUserDisplayName,
                ).collect { accumulated ->
                    finalReply = accumulated
                    _chatStreamingText.value = accumulated
                }

                if (finalReply.isNotBlank()) {
                    repository.addAiMessage(
                        threadId = threadId,
                        role = "model",
                        text = finalReply,
                        hasImage = false,
                        modelName = modelName,
                    )
                    repository.updateAiThread(
                        threadId = threadId,
                        lastMessagePreview = finalReply.replace('\n', ' ').take(200),
                        messageCount = (historyBeforeTurn.size + 2).toLong(),
                    )
                } else {
                    _chatError.value = "SuSa AI returned an empty response."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Regenerate AI response failed", e)
                _chatError.value = e.localizedMessage ?: "Regeneration failed."
            } finally {
                _isChatGenerating.value = false
                _chatStreamingText.value = null
            }
        }
    }

    private fun <T> Flow<T>.asUiState(label: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }

    private companion object {
        const val TAG = "StudioVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
