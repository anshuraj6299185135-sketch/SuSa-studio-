package com.example

import android.content.Context
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AiChatMessage
import com.example.data.model.StudioProject
import com.example.ui.components.VideoPlayer
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.PersonalAiChatScreen
import com.example.ui.screens.TimelineEditorScreen
import com.example.ui.theme.SuSaStudioTheme
import com.example.ui.viewmodel.UiState
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder()
          .setApplicationId("1:123456789012:android:abcdef123456")
          .setApiKey("test-api-key")
          .setProjectId("demo-no-project")
          .build()
      )
    }
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SuSa studio", appName)
  }

  @Test
  fun `auth screen renders FlowRow pills and Google sign-in without runtime crash`() {
    composeTestRule.setContent {
      SuSaStudioTheme {
        AuthScreen(onAuthSuccess = {})
      }
    }
    composeTestRule.onNodeWithText("Edit, Scout & Render in the Cloud.").assertIsDisplayed()
    composeTestRule.onNodeWithText("Gemini AI Studio").assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
  }

  @Test
  fun `personal ai chat screen renders starter prompts and composer`() {
    composeTestRule.setContent {
      SuSaStudioTheme {
        PersonalAiChatScreen(
          userName = "Anshu",
          threadsState = UiState.Success(emptyList()),
          selectedThreadId = null,
          messagesState = UiState.Success(emptyList<AiChatMessage>()),
          streamingText = null,
          isGenerating = false,
          chatError = null,
          selectedModel = "gemini-3.8-flash",
          customInstructions = "",
          onSelectThread = {},
          onNewChat = {},
          onRenameThread = { _, _ -> },
          onDeleteThread = {},
          onSelectModel = {},
          onSaveInstructions = {},
          onSendMessage = { _, _ -> },
          onStopGeneration = {},
          onRegenerateLast = {},
          onDismissError = {},
          onCopyFeedback = {},
        )
      }
    }
    composeTestRule.onNodeWithText("What can I help with, Anshu?").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ai_chat_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ai_send_button").assertIsDisplayed()
  }

  @Test
  fun `video player renders empty placeholder when url is blank`() {
    composeTestRule.setContent {
      SuSaStudioTheme {
        VideoPlayer(
          videoUrl = null,
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("video_player_empty").assertIsDisplayed()
    composeTestRule.onNodeWithText("No cloud or local video selected").assertIsDisplayed()
  }

  @Test
  fun `video player renders Media3 controls and cycles speed for cloud url`() {
    composeTestRule.setContent {
      SuSaStudioTheme {
        VideoPlayer(
          videoUrl = "https://storage.googleapis.com/media-session/sintel/trailer.mp4",
          title = "Sintel Cloud Cut",
          subtitle = "4K 60fps • Obsidian Cinema",
          onFullscreenToggle = {},
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("media3_video_player").assertIsDisplayed()
    composeTestRule.onNodeWithText("CLOUD").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sintel Cloud Cut").assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_player_play_pause").assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_player_scrubber").assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_player_mute_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_player_fullscreen_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("video_player_speed_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("1.5x").assertIsDisplayed()
  }

  @Test
  fun `timeline editor displays Cloud URL button and opens stream dialog`() {
    val sampleProject = StudioProject(
      id = "proj_1",
      title = "Cyberpunk Cut",
      resolution = "4K 60fps",
      aspectRatio = "16:9",
      lutPreset = "Obsidian Cinema",
      proxyEnabled = true,
      status = "editing",
    )
    composeTestRule.setContent {
      SuSaStudioTheme {
        TimelineEditorScreen(
          projectsState = UiState.Success(listOf(sampleProject)),
          selectedProjectId = "proj_1",
          clipsState = UiState.Success(emptyList()),
          commentsState = UiState.Success(emptyList()),
          selectedMediaUri = null,
          onSelectProject = {},
          onMediaPicked = {},
          onUpdateProjectSettings = { _, _, _ -> },
          onAddClip = { _, _, _, _, _, _, _, _ -> },
          onUpdateClip = { _, _, _, _, _ -> },
          onDeleteClip = { _, _ -> },
          onAddReviewComment = { _, _, _ -> },
          onToggleCommentResolved = { _, _ -> },
          onDispatchRender = { _, _ -> },
          onCreateStarterCut = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("load_cloud_video_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cloud_video_url_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_load_cloud_video").assertIsDisplayed()
  }
}

