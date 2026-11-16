package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.StudioAiRepository
import com.example.data.repository.StudioRepository
import com.example.security.configureAppCheck
import com.example.ui.screens.AccountSecurityScreen
import com.example.ui.screens.AiAutomationScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ShootMapScreen
import com.example.ui.screens.StudioHomeScreen
import com.example.ui.screens.TimelineEditorScreen
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.SuSaStudioTheme
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

enum class StudioTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    STUDIO("studio", "Studio", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    TIMELINE("timeline", "Timeline", Icons.Filled.MovieCreation, Icons.Outlined.MovieCreation),
    AI_LAB("ai_lab", "SuSa AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    SHOOT_MAP("shoot_map", "Shoot Map", Icons.Filled.Place, Icons.Outlined.Place),
    SECURITY("security", "Security", Icons.Filled.Security, Icons.Outlined.Security),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            SuSaStudioTheme {
                SuSaStudioRoot()
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun SuSaStudioRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                // AuthStateListener updates currentUser reactively
            }
        )
    } else {
        key(user.uid) {
            AuthenticatedStudioShell(user = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedStudioShell(user: FirebaseUser) {
    val uid = user.uid
    val displayName = user.displayName.orEmpty().ifBlank { user.email?.substringBefore("@").orEmpty() }

    val viewModel: StudioViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudioViewModel(
                    repository = StudioRepository(db = db),
                    aiRepository = StudioAiRepository(),
                    currentUserId = uid,
                    currentUserDisplayName = displayName,
                )
            }
        }
    )

    var activeTab by rememberSaveable { mutableStateOf(StudioTab.STUDIO) }
    var aiHubMode by rememberSaveable { mutableStateOf("chat") }
    val snackbarHostState = remember { SnackbarHostState() }
    val uiScope = rememberCoroutineScope()

    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val renderJobsState by viewModel.renderJobsState.collectAsStateWithLifecycle()
    val locationsState by viewModel.locationsState.collectAsStateWithLifecycle()
    val auditLogsState by viewModel.auditLogsState.collectAsStateWithLifecycle()
    val clipsState by viewModel.clipsState.collectAsStateWithLifecycle()
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val selectedMediaUri by viewModel.selectedMediaUri.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val storyboardState by viewModel.storyboardState.collectAsStateWithLifecycle()
    val captionsState by viewModel.captionsState.collectAsStateWithLifecycle()
    val magicCutState by viewModel.magicCutState.collectAsStateWithLifecycle()
    val scoutState by viewModel.scoutLocationsState.collectAsStateWithLifecycle()

    val aiThreadsState by viewModel.aiThreadsState.collectAsStateWithLifecycle()
    val selectedThreadId by viewModel.selectedThreadId.collectAsStateWithLifecycle()
    val aiMessagesState by viewModel.aiMessagesState.collectAsStateWithLifecycle()
    val chatStreamingText by viewModel.chatStreamingText.collectAsStateWithLifecycle()
    val isChatGenerating by viewModel.isChatGenerating.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val selectedChatModel by viewModel.selectedChatModel.collectAsStateWithLifecycle()
    val customInstructions by viewModel.customInstructions.collectAsStateWithLifecycle()

    LaunchedEffect(uid) {
        viewModel.logSignInEvent()
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    if (activeTab != StudioTab.STUDIO) {
        BackHandler {
            activeTab = StudioTab.STUDIO
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = StudioSurface,
                        titleContentColor = TextMain,
                    ),
                    title = {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = TextMain, fontWeight = FontWeight.Bold)) {
                                    append("SuSa ")
                                }
                                withStyle(SpanStyle(color = IndigoPrimary, fontWeight = FontWeight.Bold)) {
                                    append("studio")
                                }
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    actions = {
                        if (activeTab != StudioTab.AI_LAB || aiHubMode != "chat") {
                            IconButton(
                                onClick = {
                                    aiHubMode = "chat"
                                    activeTab = StudioTab.AI_LAB
                                },
                                modifier = Modifier.testTag("topbar_personal_ai_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "Open Personal AI",
                                    tint = IndigoLight,
                                )
                            }
                        }
                        Text(
                            text = activeTab.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = IndigoLight,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = StudioSurface) {
                        StudioTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = IndigoLight,
                                    selectedTextColor = IndigoLight,
                                    indicatorColor = IndigoPrimary.copy(alpha = 0.2f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                                modifier = Modifier.testTag("nav_${tab.route}"),
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = StudioSurface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = IndigoLight,
                                    selectedTextColor = IndigoLight,
                                    indicatorColor = IndigoPrimary.copy(alpha = 0.2f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                                modifier = Modifier.testTag("rail_${tab.route}"),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (activeTab) {
                        StudioTab.STUDIO -> StudioHomeScreen(
                            userName = displayName,
                            projectsState = projectsState,
                            renderJobsState = renderJobsState,
                            selectedProjectId = selectedProjectId,
                            onOpenProject = { project ->
                                viewModel.selectProject(project.id)
                                activeTab = StudioTab.TIMELINE
                            },
                            onCreateProject = { title, res, ratio, lut, proxy ->
                                viewModel.createProject(title, res, ratio, lut, proxy) {
                                    activeTab = StudioTab.TIMELINE
                                }
                            },
                            onCreateStarterCut = {
                                viewModel.createStarterCut {
                                    activeTab = StudioTab.TIMELINE
                                }
                            },
                            onDispatchRender = { project, profile ->
                                viewModel.dispatchCloudRender(project, profile)
                            },
                            onDeleteProject = { id ->
                                viewModel.deleteProject(id)
                            },
                            onOpenAiLab = {
                                aiHubMode = "chat"
                                activeTab = StudioTab.AI_LAB
                            },
                        )

                        StudioTab.TIMELINE -> TimelineEditorScreen(
                            projectsState = projectsState,
                            selectedProjectId = selectedProjectId,
                            clipsState = clipsState,
                            commentsState = commentsState,
                            selectedMediaUri = selectedMediaUri,
                            onSelectProject = viewModel::selectProject,
                            onMediaPicked = viewModel::setSelectedMediaUri,
                            onUpdateProjectSettings = { project, lut, proxy ->
                                viewModel.updateProjectSettings(project, lut, proxy)
                            },
                            onAddClip = viewModel::addClip,
                            onUpdateClip = viewModel::updateClip,
                            onDeleteClip = viewModel::deleteClip,
                            onAddReviewComment = viewModel::addReviewComment,
                            onToggleCommentResolved = viewModel::toggleCommentResolved,
                            onDispatchRender = viewModel::dispatchCloudRender,
                            onCreateStarterCut = {
                                viewModel.createStarterCut()
                            },
                        )

                        StudioTab.AI_LAB -> AiAutomationScreen(
                            userName = displayName,
                            activeMode = aiHubMode,
                            onModeChange = { aiHubMode = it },
                            threadsState = aiThreadsState,
                            selectedThreadId = selectedThreadId,
                            messagesState = aiMessagesState,
                            streamingText = chatStreamingText,
                            isGenerating = isChatGenerating,
                            chatError = chatError,
                            selectedModel = selectedChatModel,
                            customInstructions = customInstructions,
                            onSelectThread = viewModel::selectAiThread,
                            onNewChat = viewModel::startNewAiThread,
                            onRenameThread = viewModel::renameAiThread,
                            onDeleteThread = viewModel::deleteAiThread,
                            onSelectModel = viewModel::setChatModel,
                            onSaveInstructions = viewModel::updatePersonalInstructions,
                            onSendMessage = viewModel::sendPersonalAiMessage,
                            onStopGeneration = viewModel::stopAiGeneration,
                            onRegenerateLast = viewModel::regenerateLastAiResponse,
                            onDismissChatError = viewModel::dismissChatError,
                            onCopyFeedback = { msg ->
                                uiScope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                            storyboardState = storyboardState,
                            captionsState = captionsState,
                            magicCutState = magicCutState,
                            scoutState = scoutState,
                            onGenerateStoryboard = viewModel::generateStoryboard,
                            onApplyStoryboardToTimeline = { result ->
                                viewModel.applyStoryboardToActiveProject(result)
                                activeTab = StudioTab.TIMELINE
                            },
                            onGenerateCaptions = viewModel::generateAutoCaptions,
                            onApplyCaptionsToTimeline = { lines ->
                                viewModel.applyCaptionsToActiveProject(lines)
                                activeTab = StudioTab.TIMELINE
                            },
                            onStreamMagicCut = viewModel::streamMagicCutPlan,
                            onScoutLocations = viewModel::scoutShootLocations,
                            onPinScoutedLocation = { loc ->
                                viewModel.saveShootLocation(
                                    name = loc.name,
                                    category = loc.category,
                                    latitude = loc.latitude,
                                    longitude = loc.longitude,
                                    scoutingNotes = loc.scoutingNotes,
                                    attachProjectId = selectedProjectId,
                                )
                                activeTab = StudioTab.SHOOT_MAP
                            },
                        )

                        StudioTab.SHOOT_MAP -> ShootMapScreen(
                            locationsState = locationsState,
                            projectsState = projectsState,
                            selectedProjectId = selectedProjectId,
                            onSaveLocation = viewModel::saveShootLocation,
                            onAttachLocationToProject = viewModel::attachLocationToProject,
                            onDeleteLocation = viewModel::deleteShootLocation,
                        )

                        StudioTab.SECURITY -> AccountSecurityScreen(
                            user = user,
                            auditLogsState = auditLogsState,
                            onSignOutComplete = {
                                activeTab = StudioTab.STUDIO
                            },
                        )
                    }
                }
            }
        }
    }
}
