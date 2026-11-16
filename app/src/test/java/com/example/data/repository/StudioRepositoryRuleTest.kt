package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createProject_andAddClip_succeedsForAuthenticatedOwner() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore, auth)

        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProject(
                title = "SuSa Launch Cut",
                resolution = "4K 60fps",
                aspectRatio = "16:9",
                lutPreset = "Obsidian Cinema",
            )
        }
        assertTrue(projectId.isNotBlank())

        val clipId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addClip(
                projectId = projectId,
                trackType = "video",
                label = "Intro Aerial",
                startMs = 0L,
                durationMs = 8000L,
                speedMultiplier = 1.5,
            )
        }
        assertTrue(clipId.isNotBlank())

        val projects = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProjects().first { list -> list.any { it.id == projectId } }
        }
        assertTrue(projects.any { it.id == projectId && it.title == "SuSa Launch Cut" })
    }

    @Test
    fun renderJob_andShootLocation_lifecyclePersistsProperly() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore, auth)

        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProject(
                title = "Kyoto Dawn Reel",
                resolution = "9:16 Reels",
                aspectRatio = "9:16",
                lutPreset = "Cyber Indigo",
            )
        }

        val jobId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.enqueueRenderJob(projectId, "Kyoto Dawn Reel", "Reels 9:16 60fps")
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateRenderJobProgress(jobId, "completed", 100L)
        }

        val jobs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeRenderJobs().first { list -> list.any { it.id == jobId && it.status == "completed" } }
        }
        assertTrue(jobs.any { it.id == jobId && it.progressPercent == 100L })

        val locId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveShootLocation(
                name = "Fushimi Inari Gate",
                category = "Exterior",
                latitude = 34.9671,
                longitude = 135.7727,
                scoutingNotes = "Golden hour 06:00",
                projectId = projectId,
            )
        }
        assertTrue(locId.isNotBlank())
    }

    @Test
    fun crossUserRead_isDeniedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StudioRepository(firestore, auth)
        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createProject("Private Edit", "4K 60fps", "16:9", "Obsidian Cinema")
        }

        signInTestUser(BOB_EMAIL)
        val exception = assertThrowsType<FirebaseFirestoreException> {
            runBlocking {
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    firestore.collection("users").document(aliceUid)
                        .collection("projects").document(projectId)
                        .get().await()
                }
            }
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception.code)
    }

    @Test
    fun observeProjects_unauthenticatedUser_failsFastOrDenied() = runBlocking {
        auth.signOut()
        val repository = StudioRepository(firestore, auth)
        val exception = assertThrowsType<IllegalStateException> {
            runBlocking {
                withTimeout(FLOW_TIMEOUT_MS) {
                    repository.observeProjects().first()
                }
            }
        }
        assertTrue(exception.message?.contains("signed in") == true)
    }

    @Test
    fun directUnauthenticatedFirestoreQuery_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val exception = assertThrowsType<FirebaseFirestoreException> {
            runBlocking {
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    firestore.collection("users").document("alice_test")
                        .collection("projects").get().await()
                }
            }
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception.code)
    }

    @Test
    fun aiThread_andMessages_lifecycleAndCrossUserDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StudioRepository(firestore, auth)

        val threadId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createAiThread(
                title = "Personal Assistant Chat",
                modelName = "gemini-3.8-flash",
                systemInstruction = "Be concise.",
                lastMessagePreview = "Hi SuSa AI",
            )
        }
        assertTrue(threadId.isNotBlank())

        val userMsgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addAiMessage(
                threadId = threadId,
                role = "user",
                text = "Explain quantum computing simply.",
                hasImage = false,
                modelName = "gemini-3.8-flash",
            )
        }
        assertTrue(userMsgId.isNotBlank())

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeAiMessages(threadId).first { it.isNotEmpty() }
        }
        assertEquals(1, messages.size)
        assertEquals("user", messages.first().role)

        signInTestUser(BOB_EMAIL)
        val exception = assertThrowsType<FirebaseFirestoreException> {
            runBlocking {
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    firestore.collection("users").document(aliceUid)
                        .collection("aiThreads").document(threadId)
                        .get().await()
                }
            }
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_susa@test.com"
        const val BOB_EMAIL = "bob_susa@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
