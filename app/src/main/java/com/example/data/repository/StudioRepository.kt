package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.model.AiChatMessage
import com.example.data.model.AiChatThread
import com.example.data.model.RenderJob
import com.example.data.model.ReviewComment
import com.example.data.model.SecurityAuditLog
import com.example.data.model.ShootLocation
import com.example.data.model.StudioProject
import com.example.data.model.TimelineClip
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudioRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Studio Projects ---

    fun observeProjects(): Flow<List<StudioProject>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("projects")
        emitAll(
            colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudioProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    suspend fun createProject(
        title: String,
        resolution: String,
        aspectRatio: String,
        lutPreset: String,
        proxyEnabled: Boolean = true,
        shootLocationName: String = "",
        shootLat: Double = 0.0,
        shootLng: Double = 0.0,
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().take(120),
            "resolution" to resolution,
            "aspectRatio" to aspectRatio,
            "durationSeconds" to 30L,
            "status" to "editing",
            "lutPreset" to lutPreset,
            "shootLocationName" to shootLocationName.take(140),
            "shootLat" to shootLat,
            "shootLng" to shootLng,
            "collaboratorCount" to 1L,
            "proxyEnabled" to proxyEnabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            docRef.set(payload).await()
            logAuditEventInternal(uid, "PROJECT_CREATED", "Created project \"${title.trim().take(60)}\" ($resolution)")
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProjectSettings(
        projectId: String,
        lutPreset: String,
        proxyEnabled: Boolean,
        status: String,
        durationSeconds: Long,
    ) {
        val uid = requireUserId()
        require(projectId.isNotBlank()) { "Project ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        val updates = mapOf(
            "lutPreset" to lutPreset,
            "proxyEnabled" to proxyEnabled,
            "status" to status,
            "durationSeconds" to durationSeconds,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun attachLocationToProject(
        projectId: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
    ) {
        val uid = requireUserId()
        require(projectId.isNotBlank()) { "Project ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        val updates = mapOf(
            "shootLocationName" to locationName.take(140),
            "shootLat" to latitude,
            "shootLng" to longitude,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteProject(projectId: String) {
        val uid = requireUserId()
        require(projectId.isNotBlank()) { "Project ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Timeline Clips ---

    fun observeClips(projectId: String): Flow<List<TimelineClip>> = flow {
        val uid = requireUserId()
        require(projectId.isNotBlank()) { "Project ID must not be blank" }
        val colRef = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("clips")
        emitAll(
            colRef.orderBy("startMs", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TimelineClip::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    suspend fun addClip(
        projectId: String,
        trackType: String,
        label: String,
        startMs: Long,
        durationMs: Long,
        speedMultiplier: Double = 1.0,
        colorHex: String = "#6366F1",
        mediaUri: String = "",
        notes: String = "",
    ): String {
        val uid = requireUserId()
        require(projectId.isNotBlank()) { "Project ID must not be blank" }
        val docRef = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("clips").document()
        val payload = mapOf(
            "userId" to uid,
            "trackType" to trackType,
            "label" to label.trim().take(200),
            "startMs" to startMs.coerceAtLeast(0L),
            "durationMs" to durationMs.coerceAtLeast(100L),
            "speedMultiplier" to speedMultiplier.coerceIn(0.25, 8.0),
            "colorHex" to colorHex,
            "mediaUri" to mediaUri.take(1024),
            "notes" to notes.take(1000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateClip(
        projectId: String,
        clipId: String,
        durationMs: Long,
        speedMultiplier: Double,
        notes: String,
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("clips").document(clipId)
        try {
            docRef.update(
                mapOf(
                    "durationMs" to durationMs.coerceAtLeast(100L),
                    "speedMultiplier" to speedMultiplier.coerceIn(0.25, 8.0),
                    "notes" to notes.take(1000),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteClip(projectId: String, clipId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("clips").document(clipId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Review Comments ---

    fun observeComments(projectId: String): Flow<List<ReviewComment>> = flow {
        val uid = requireUserId()
        require(projectId.isNotBlank()) { "Project ID must not be blank" }
        val colRef = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("comments")
        emitAll(
            colRef.orderBy("timestampMs", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ReviewComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    suspend fun addComment(
        projectId: String,
        authorName: String,
        timestampMs: Long,
        body: String,
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("comments").document()
        val payload = mapOf(
            "userId" to uid,
            "authorName" to authorName.ifBlank { "Studio Editor" }.take(80),
            "timestampMs" to timestampMs.coerceAtLeast(0L),
            "body" to body.trim().take(600),
            "resolved" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleCommentResolved(projectId: String, commentId: String, resolved: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("comments").document(commentId)
        try {
            docRef.update(
                mapOf(
                    "resolved" to resolved,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Cloud Render Jobs ---

    fun observeRenderJobs(): Flow<List<RenderJob>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("renderJobs")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RenderJob::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    suspend fun enqueueRenderJob(
        projectId: String,
        projectTitle: String,
        profile: String,
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("renderJobs").document()
        val payload = mapOf(
            "userId" to uid,
            "projectId" to projectId,
            "projectTitle" to projectTitle.take(120),
            "profile" to profile.take(80),
            "status" to "queued",
            "progressPercent" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            docRef.set(payload).await()
            logAuditEventInternal(uid, "RENDER_DISPATCHED", "Queued $profile render for \"$projectTitle\"")
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateRenderJobProgress(
        jobId: String,
        status: String,
        progressPercent: Long,
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("renderJobs").document(jobId)
        try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "progressPercent" to progressPercent.coerceIn(0L, 100L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Geotagged Shoot Locations ---

    fun observeShootLocations(): Flow<List<ShootLocation>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("locations")
        emitAll(
            colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ShootLocation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    suspend fun saveShootLocation(
        name: String,
        category: String,
        latitude: Double,
        longitude: Double,
        scoutingNotes: String,
        projectId: String = "",
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("locations").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(140),
            "category" to category,
            "latitude" to latitude.coerceIn(-90.0, 90.0),
            "longitude" to longitude.coerceIn(-180.0, 180.0),
            "scoutingNotes" to scoutingNotes.take(1000),
            "projectId" to projectId.take(128),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            docRef.set(payload).await()
            logAuditEventInternal(uid, "LOCATION_PINNED", "Pinned $category location \"${name.trim().take(60)}\"")
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteShootLocation(locationId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("locations").document(locationId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Security Audit Logs ---

    fun observeAuditLogs(): Flow<List<SecurityAuditLog>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("auditLogs")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(25)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SecurityAuditLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    suspend fun logAuditEvent(eventType: String, detail: String) {
        val uid = requireUserId()
        logAuditEventInternal(uid, eventType, detail)
    }

    private suspend fun logAuditEventInternal(uid: String, eventType: String, detail: String) {
        val docRef = db.collection("users").document(uid).collection("auditLogs").document()
        val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}".take(100)
        val payload = mapOf(
            "userId" to uid,
            "eventType" to eventType.take(60),
            "deviceLabel" to deviceLabel,
            "detail" to detail.take(400),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String,
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    // --- Personal AI Assistant Threads & Messages ---

    fun observeAiThreads(): Flow<List<AiChatThread>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("aiThreads")
        emitAll(
            colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AiChatThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    fun observeAiMessages(threadId: String): Flow<List<AiChatMessage>> = flow {
        val uid = requireUserId()
        require(threadId.isNotBlank()) { "Thread ID must not be blank" }
        val colRef = db.collection("users").document(uid)
            .collection("aiThreads").document(threadId)
            .collection("messages")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AiChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    suspend fun createAiThread(
        title: String,
        modelName: String,
        systemInstruction: String,
        lastMessagePreview: String = "",
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("aiThreads").document()
        val safeModel = if (modelName == "gemini-3.5-flash-lite") modelName else "gemini-3.8-flash"
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().ifBlank { "New Chat" }.take(120),
            "modelName" to safeModel,
            "systemInstruction" to systemInstruction.trim().take(1000),
            "lastMessagePreview" to lastMessagePreview.trim().take(300),
            "messageCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateAiThread(
        threadId: String,
        title: String? = null,
        modelName: String? = null,
        systemInstruction: String? = null,
        lastMessagePreview: String? = null,
        messageCount: Long? = null,
    ) {
        val uid = requireUserId()
        require(threadId.isNotBlank()) { "Thread ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("aiThreads").document(threadId)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (title != null) updates["title"] = title.trim().ifBlank { "New Chat" }.take(120)
        if (modelName != null) {
            updates["modelName"] = if (modelName == "gemini-3.5-flash-lite") modelName else "gemini-3.8-flash"
        }
        if (systemInstruction != null) updates["systemInstruction"] = systemInstruction.trim().take(1000)
        if (lastMessagePreview != null) updates["lastMessagePreview"] = lastMessagePreview.trim().take(300)
        if (messageCount != null) updates["messageCount"] = messageCount.coerceIn(0L, 10000L)
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAiThread(threadId: String) {
        val uid = requireUserId()
        require(threadId.isNotBlank()) { "Thread ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("aiThreads").document(threadId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun addAiMessage(
        threadId: String,
        role: String,
        text: String,
        hasImage: Boolean,
        modelName: String,
    ): String {
        val uid = requireUserId()
        require(threadId.isNotBlank()) { "Thread ID must not be blank" }
        val docRef = db.collection("users").document(uid)
            .collection("aiThreads").document(threadId)
            .collection("messages").document()
        val safeRole = if (role == "model") "model" else "user"
        val safeText = text.trim().ifBlank { " " }.take(50000)
        val payload = mapOf(
            "userId" to uid,
            "role" to safeRole,
            "text" to safeText,
            "hasImage" to hasImage,
            "modelName" to modelName.take(60),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAiMessage(threadId: String, messageId: String) {
        val uid = requireUserId()
        require(threadId.isNotBlank() && messageId.isNotBlank()) { "Thread and Message IDs must not be blank" }
        val docRef = db.collection("users").document(uid)
            .collection("aiThreads").document(threadId)
            .collection("messages").document(messageId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
