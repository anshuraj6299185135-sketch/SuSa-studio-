package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AiCaptionLine
import com.example.data.model.AiChatMessage
import com.example.data.model.AiLocationSuggestion
import com.example.data.model.AiStoryboardResult
import com.example.data.model.AiStoryboardScene
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class StudioAiRepository {

    private val storyboardSchema = Schema.obj(
        properties = mapOf(
            "conceptSummary" to Schema.string("Brief 1-2 sentence directorial summary of the video cut"),
            "recommendedLut" to Schema.enumeration(
                values = listOf("Obsidian Cinema", "Cyber Indigo", "Teal & Amber", "Noir Monochrome", "Natural Rec.709"),
                description = "Recommended SuSa color LUT preset"
            ),
            "scenes" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "sceneNumber" to Schema.integer("1-based scene index"),
                        "title" to Schema.string("Short clip label, max 40 chars"),
                        "trackType" to Schema.enumeration(listOf("video", "vfx", "audio")),
                        "durationSeconds" to Schema.integer("Clip duration in seconds, between 3 and 30"),
                        "cameraNote" to Schema.string("Framing, lens, transition, or speed ramp note"),
                        "voiceover" to Schema.string("Voiceover or caption line for this scene"),
                    )
                ),
                description = "4 to 6 structured timeline scenes"
            )
        )
    )

    private val captionSchema = Schema.obj(
        properties = mapOf(
            "captions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "startTime" to Schema.string("Formatted start timecode like 00:00"),
                        "endTime" to Schema.string("Formatted end timecode like 00:04"),
                        "startMs" to Schema.integer("Start offset in milliseconds"),
                        "durationMs" to Schema.integer("Duration in milliseconds"),
                        "text" to Schema.string("Translated or cleaned subtitle line"),
                    )
                )
            )
        )
    )

    private val locationSchema = Schema.obj(
        properties = mapOf(
            "locations" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string("Real-world filming spot or landmark name"),
                        "category" to Schema.enumeration(listOf("Exterior", "Studio", "Aerial")),
                        "latitude" to Schema.double("Accurate WGS84 latitude"),
                        "longitude" to Schema.double("Accurate WGS84 longitude"),
                        "scoutingNotes" to Schema.string("Best lighting window, lens advice, and crowd/permit tip"),
                    )
                ),
                description = "3 recommended filming locations with real coordinates"
            )
        )
    )

    private val storyboardModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = storyboardSchema
            }
        )
    }

    private val captionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = captionSchema
            }
        )
    }

    private val locationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = locationSchema
            }
        )
    }

    private val streamingModel by lazy {
        Firebase.ai.generativeModel(modelName = MODEL_NAME)
    }

    suspend fun generateStoryboard(prompt: String): AiStoryboardResult {
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                storyboardModel.generateContent(
                    "You are SuSa studio's Senior Video Director AI. Turn this video brief into a concrete 4-to-5-scene multi-track editing storyboard with camera framing, speed ramps, and voiceover:\n\n$prompt"
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty storyboard.")
            val root = JSONObject(jsonText)
            val arr = root.getJSONArray("scenes")
            val scenes = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                AiStoryboardScene(
                    sceneNumber = obj.optInt("sceneNumber", i + 1),
                    title = obj.getString("title"),
                    trackType = obj.optString("trackType", "video"),
                    durationSeconds = obj.optInt("durationSeconds", 6).coerceIn(3, 30),
                    cameraNote = obj.getString("cameraNote"),
                    voiceover = obj.getString("voiceover"),
                )
            }
            AiStoryboardResult(
                conceptSummary = root.getString("conceptSummary"),
                recommendedLut = root.optString("recommendedLut", "Obsidian Cinema"),
                scenes = scenes,
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Storyboard generation timed out", e)
            throw IllegalStateException("AI storyboard timed out after 60 seconds. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Storyboard generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Storyboard generation failed.", e)
        }
    }

    suspend fun generateCaptions(script: String, language: String): List<AiCaptionLine> {
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                captionModel.generateContent(
                    "Generate timed subtitle blocks in $language for a short-form/cinema video edit from the following dialogue or transcript. Space timestamps cleanly starting at 0ms:\n\n$script"
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned empty captions.")
            val root = JSONObject(jsonText)
            val arr = root.getJSONArray("captions")
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                AiCaptionLine(
                    startTime = obj.getString("startTime"),
                    endTime = obj.getString("endTime"),
                    startMs = obj.optLong("startMs", i * 3500L),
                    durationMs = obj.optLong("durationMs", 3200L).coerceAtLeast(500L),
                    text = obj.getString("text"),
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Caption generation timed out", e)
            throw IllegalStateException("AI captions timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Caption generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Caption generation failed.", e)
        }
    }

    fun generateMagicCutPlanStream(rawNotes: String): Flow<String> = flow {
        val buffer = StringBuilder()
        try {
            withTimeout(TIMEOUT_MS) {
                streamingModel.generateContentStream(
                    "You are SuSa studio's Smart Magic Cut & Voice Isolation AI engineer. Analyze this footage description and produce a concise, actionable edit plan covering: (1) Dead-space & silence cuts, (2) Speed-ramp timestamps, (3) Voice isolation & LUFS loudness target, and (4) Color matching LUT advice.\n\nFootage brief: $rawNotes"
                ).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        buffer.append(piece)
                        emit(buffer.toString())
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Magic Cut stream timed out", e)
            throw IllegalStateException("AI Magic Cut timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Magic Cut stream failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Magic Cut streaming failed.", e)
        }
    }

    suspend fun scoutShootLocations(moodPrompt: String): List<AiLocationSuggestion> {
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                locationModel.generateContent(
                    "Suggest 3 real-world filming locations matching this cinematic visual brief, with accurate WGS84 latitude/longitude and practical cinematography scouting notes:\n\n$moodPrompt"
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned empty location suggestions.")
            val root = JSONObject(jsonText)
            val arr = root.getJSONArray("locations")
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                AiLocationSuggestion(
                    name = obj.getString("name"),
                    category = obj.optString("category", "Exterior"),
                    latitude = obj.getDouble("latitude").coerceIn(-90.0, 90.0),
                    longitude = obj.getDouble("longitude").coerceIn(-180.0, 180.0),
                    scoutingNotes = obj.getString("scoutingNotes"),
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Location scout timed out", e)
            throw IllegalStateException("AI Location Scout timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Location scout failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Location scouting failed.", e)
        }
    }

    fun streamPersonalChatResponse(
        history: List<AiChatMessage>,
        userPrompt: String,
        imageBitmap: Bitmap?,
        modelName: String,
        systemInstruction: String,
        userName: String,
    ): Flow<String> = flow {
        val selectedModelName = if (modelName == MODEL_LITE) MODEL_LITE else MODEL_NAME
        val basePersona = buildString {
            append("You are SuSa AI, a helpful, intelligent personal AI assistant (similar to ChatGPT) built for ")
            append(userName.ifBlank { "the user" })
            append(" inside SuSa studio. Answer any topic clearly—everyday questions, writing, coding, learning, planning, analysis, math, or video production—using clean markdown formatting (headings, bullet lists, and fenced code blocks when sharing code). Be accurate, concise, and helpful.")
            if (systemInstruction.isNotBlank()) {
                append("\n\nUser's Personal Custom Instructions:\n")
                append(systemInstruction.trim())
            }
        }

        val chatModel = Firebase.ai.generativeModel(
            modelName = selectedModelName,
            systemInstruction = content { text(basePersona) }
        )

        val sdkHistory = history
            .filter { it.text.isNotBlank() }
            .takeLast(30)
            .map { msg ->
                val role = if (msg.role == "model") "model" else "user"
                content(role = role) { text(msg.text) }
            }

        val chat = chatModel.startChat(history = sdkHistory)
        val buffer = StringBuilder()

        try {
            withTimeout(TIMEOUT_MS) {
                val promptContent = content("user") {
                    if (imageBitmap != null) {
                        image(imageBitmap)
                    }
                    text(userPrompt.ifBlank { "Analyze this image and describe what you see." })
                }
                chat.sendMessageStream(promptContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        buffer.append(piece)
                        emit(buffer.toString())
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Personal AI chat timed out", e)
            throw IllegalStateException("Response timed out after 60 seconds. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Personal AI chat failed", e)
            throw IllegalStateException(e.localizedMessage ?: "AI chat generation failed.", e)
        }
    }

    private companion object {
        const val TAG = "StudioAiRepo"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val MODEL_LITE = "gemini-3.5-flash-lite"
        const val TIMEOUT_MS = 60_000L
    }
}
