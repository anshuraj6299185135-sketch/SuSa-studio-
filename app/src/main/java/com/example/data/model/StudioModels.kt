package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StudioProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val resolution: String = "4K 60fps",
    val aspectRatio: String = "16:9",
    val durationSeconds: Long = 60L,
    val status: String = "editing",
    val lutPreset: String = "Obsidian Cinema",
    val shootLocationName: String = "",
    val shootLat: Double = 0.0,
    val shootLng: Double = 0.0,
    val collaboratorCount: Long = 1L,
    val proxyEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TimelineClip(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackType: String = "video",
    val label: String = "",
    val startMs: Long = 0L,
    val durationMs: Long = 5000L,
    val speedMultiplier: Double = 1.0,
    val colorHex: String = "#6366F1",
    val mediaUri: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ReviewComment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val timestampMs: Long = 0L,
    val body: String = "",
    val resolved: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class RenderJob(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val profile: String = "YouTube 4K H.265",
    val status: String = "queued",
    val progressPercent: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ShootLocation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Exterior",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val scoutingNotes: String = "",
    val projectId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SecurityAuditLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val eventType: String = "",
    val deviceLabel: String = "",
    val detail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

data class AiStoryboardScene(
    val sceneNumber: Int,
    val title: String,
    val trackType: String,
    val durationSeconds: Int,
    val cameraNote: String,
    val voiceover: String,
)

data class AiStoryboardResult(
    val conceptSummary: String,
    val recommendedLut: String,
    val scenes: List<AiStoryboardScene>,
)

data class AiCaptionLine(
    val startTime: String,
    val endTime: String,
    val startMs: Long,
    val durationMs: Long,
    val text: String,
)

data class AiLocationSuggestion(
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val scoutingNotes: String,
)

@IgnoreExtraProperties
data class AiChatThread(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "New Chat",
    val modelName: String = "gemini-3.8-flash",
    val systemInstruction: String = "",
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AiChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val hasImage: Boolean = false,
    val modelName: String = "gemini-3.8-flash",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

