package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.MainActivity
import com.example.ui.bottomnav.AppTab
import com.example.ui.bottomnav.FixedBottomNav
import com.example.ui.bottomnav.WorkspaceActionsBottomSheet
import com.example.ui.chat.ChatScreen
import com.example.ui.code.CodeScreen
import com.example.ui.export.GithubExportBottomSheet
import com.example.ui.settings.AudioSettingsScreen
import com.example.ui.settings.GlobalSettingsScreen
import com.example.ui.settings.LogKeeperScreen
import com.example.ui.settings.ThreadSettingsScreen
import com.example.ui.settings.omniroot.AiManagerPanelScreen
import com.example.ui.settings.omniroot.DirectToKeyWebViewScreen
import com.example.ui.sidebar.GlobalSidebar
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun OmniRootApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("omniroot_prefs", android.content.Context.MODE_PRIVATE) }
    
    var currentTab by remember { mutableStateOf(AppTab.CHAT) }
    var showWorkspaceActions by remember { mutableStateOf(false) }
    var showGithubExport by remember { mutableStateOf(false) }
    var chatSessionId by remember { 
        mutableStateOf(
            run {
                val savedId = prefs.getString("active_chat_id", null)
                val existing = com.example.engine.fs.LocalFileManager.getWorkspaces()
                if (savedId != null && existing.any { it.name == savedId }) {
                    savedId
                } else if (existing.isNotEmpty()) {
                    existing.first().name
                } else {
                    val newId = java.util.UUID.randomUUID().toString()
                    com.example.engine.fs.LocalFileManager.setWorkspaceName(newId, "Chat 1")
                    prefs.edit().putString("active_chat_id", newId).apply()
                    newId
                }
            }
        ) 
    }
    
    val navController = rememberNavController()
    var showNewChatDialog by remember { mutableStateOf(false) }

    var triggerCameraLaunch by remember { mutableStateOf(false) }

    // React to incoming Deep Links and Intents from Widget & Shortcuts
    val incomingUri by MainActivity.currentIntentData
    val incomingAction by MainActivity.currentIntentAction

    LaunchedEffect(incomingUri, incomingAction) {
        val uri = incomingUri
        val action = incomingAction
        if (uri != null || action != null) {
            val uriStr = uri?.toString() ?: ""
            if (uriStr.contains("chat/new") || action == "open_chat") {
                val isTemp = uri?.getBooleanQueryParameter("temp", false) ?: false
                if (isTemp) {
                    val newTempId = "temp_${System.currentTimeMillis()}"
                    com.example.engine.fs.LocalFileManager.setWorkspaceName(newTempId, "🔥 Quick Chat")
                    chatSessionId = newTempId
                }
                currentTab = AppTab.CHAT
                navController.navigate("main") { popUpTo("main") { inclusive = true } }
            } else if (uriStr.contains("chat/camera") || action == "open_camera") {
                val isTemp = uri?.getBooleanQueryParameter("temp", false) ?: false
                if (isTemp) {
                    val newTempId = "temp_${System.currentTimeMillis()}"
                    com.example.engine.fs.LocalFileManager.setWorkspaceName(newTempId, "📷 Camera Chat")
                    chatSessionId = newTempId
                }
                triggerCameraLaunch = true
                currentTab = AppTab.CHAT
                navController.navigate("main") { popUpTo("main") { inclusive = true } }
            } else if (uriStr.contains("chat/voice") || action == "voice_mode") {
                currentTab = AppTab.CHAT
                navController.navigate("main") { popUpTo("main") { inclusive = true } }
            } else if (uriStr.contains("code/workspace") || action == "open_code") {
                currentTab = AppTab.CODE
                navController.navigate("main") { popUpTo("main") { inclusive = true } }
            }
            // Clear once handled
            MainActivity.currentIntentData.value = null
            MainActivity.currentIntentAction.value = null
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDrawerGesturesEnabled = (currentRoute == "main")

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isDrawerGesturesEnabled,
        drawerContent = {
            GlobalSidebar(
                onClose = { scope.launch { drawerState.close() } },
                onNewChat = { 
                    showNewChatDialog = true
                },
                onNavigateToArtifacts = {
                    scope.launch { drawerState.close() }
                    navController.navigate("artifacts")
                },
                onNavigateToSettings = { 
                    scope.launch { drawerState.close() }
                    navController.navigate("settings")
                },
                currentChatId = chatSessionId,
                onChatSelected = { newSessionId -> 
                    chatSessionId = newSessionId
                    prefs.edit().putString("active_chat_id", newSessionId).apply()
                }
            )
        }
    ) {
        if (showNewChatDialog) {
            com.example.ui.chat.NewChatDialog(
                onDismiss = { showNewChatDialog = false },
                onCreate = { threadName, appType, model, integrations, instructions ->
                    val newSessionId = java.util.UUID.randomUUID().toString()
                    com.example.engine.fs.LocalFileManager.setWorkspaceName(newSessionId, threadName)
                    scope.launch {
                        val db = com.example.engine.db.AppDatabase.getDatabase(context)
                        db.workspaceConfigDao().saveConfig(
                            com.example.engine.db.WorkspaceConfigEntity(
                                workspaceId = newSessionId,
                                threadName = threadName,
                                appType = appType,
                                model = model,
                                integrations = integrations,
                                instructions = instructions
                            )
                        )
                    }
                    chatSessionId = newSessionId
                    prefs.edit().putString("active_chat_id", newSessionId).apply()
                    showNewChatDialog = false
                    scope.launch { drawerState.close() }
                }
            )
        }
        
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                val isImeOpen = WindowInsets.isImeVisible
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.statusBars,
                    bottomBar = {
                        if (!isImeOpen) {
                            FixedBottomNav(
                                currentTab = currentTab,
                                onTabSelected = { currentTab = it },
                                onMoreClick = { showWorkspaceActions = true }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        when (currentTab) {
                            AppTab.CHAT -> key(chatSessionId) {
                                remember(chatSessionId) {
                                    com.example.engine.fs.LocalFileManager.switchWorkspace(chatSessionId)
                                    true
                                }
                                ChatScreen(
                                    sessionId = chatSessionId,
                                    initialCameraTrigger = triggerCameraLaunch,
                                    onCameraTriggerConsumed = { triggerCameraLaunch = false },
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    onNavigateToThreadSettings = { navController.navigate("thread_settings") },
                                    onSessionPromoted = { promotedId ->
                                        chatSessionId = promotedId
                                        prefs.edit().putString("active_chat_id", promotedId).apply()
                                    }
                                )
                            }
                            AppTab.CODE -> CodeScreen(
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                        
                        FloatingActionButton(
                            onClick = { navController.navigate("log_keeper") },
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = "Export Logs")
                        }
                    }
                }
                
                if (showWorkspaceActions) {
                    WorkspaceActionsBottomSheet(
                        onDismiss = { showWorkspaceActions = false },
                        onExportClick = {
                            showWorkspaceActions = false
                            showGithubExport = true
                        },
                        onThreadSettingsClick = {
                            showWorkspaceActions = false
                            navController.navigate("thread_settings")
                        }
                    )
                }
                
                if (showGithubExport) {
                    GithubExportBottomSheet(
                        onDismiss = { showGithubExport = false }
                    )
                }
            }

            composable("artifacts") {
                com.example.ui.artifacts.ArtifactsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEditInChatCode = { artifact ->
                        scope.launch {
                            val wsId = com.example.engine.fs.ArtifactWorkspaceManager.openArtifactInWorkspace(context, artifact)
                            chatSessionId = wsId
                            prefs.edit().putString("active_chat_id", wsId).apply()
                            navController.popBackStack()
                            currentTab = AppTab.CHAT
                            android.widget.Toast.makeText(context, "Editing '${artifact.title}' with AI in Chat & Code", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            
            composable("thread_settings") {
                ThreadSettingsScreen(
                    workspaceId = chatSessionId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable("settings") {
                val context = androidx.compose.ui.platform.LocalContext.current
                GlobalSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateTo = { destination ->
                        when (destination) {
                            "settings/audio", "audio_settings" -> navController.navigate("audio_settings")
                            "settings/omniroot", "ai_manager" -> navController.navigate("ai_manager")
                            "settings/artifacts", "artifacts" -> navController.navigate("artifacts")
                            "log_keeper" -> navController.navigate("log_keeper")
                            else -> {
                                try {
                                    navController.navigate(destination)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Module configuration coming soon", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }

            composable("audio_settings") {
                AudioSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("log_keeper") {
                LogKeeperScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("ai_manager") {
                AiManagerPanelScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onAddKeyClick = { providerId ->
                        navController.navigate("direct_to_key/$providerId")
                    }
                )
            }

            composable("direct_to_key/{providerId}") { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
                DirectToKeyWebViewScreen(
                    providerId = providerId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
