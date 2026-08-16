package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.bottomnav.AppTab
import com.example.ui.bottomnav.FixedBottomNav
import com.example.ui.bottomnav.WorkspaceActionsBottomSheet
import com.example.ui.export.GithubExportBottomSheet
import com.example.ui.chat.ChatScreen
import com.example.ui.code.CodeScreen
import com.example.ui.sidebar.GlobalSidebar
import com.example.ui.settings.GlobalSettingsScreen
import com.example.ui.settings.ThreadSettingsScreen
import com.example.ui.settings.LogKeeperScreen
import com.example.utils.LogKeeper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniRootApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("omniroot_prefs", android.content.Context.MODE_PRIVATE) }
    
    var currentTab by remember { mutableStateOf(AppTab.CHAT) }
    var showWorkspaceActions by remember { mutableStateOf(false) }
    var showGithubExport by remember { mutableStateOf(false) }
    var showTokenPanel by remember { mutableStateOf(false) }
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        FixedBottomNav(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it },
                            onMoreClick = { showWorkspaceActions = true }
                        )
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
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    onNavigateToThreadSettings = { navController.navigate("thread_settings") }
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
                        onZipExportClick = {
                            showWorkspaceActions = false
                            scope.launch {
                                val context = navController.context
                                val dir = com.example.engine.fs.LocalFileManager.getWorkspaceDir()
                                val cacheDir = context.cacheDir
                                val zipFile = java.io.File(cacheDir, "workspace_${dir.name}.zip")
                                val result = com.example.engine.fs.LocalFileManager.zipDirectory(dir, zipFile)
                                if (result.isSuccess) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        zipFile
                                    )
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Export Workspace"))
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to create ZIP", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onThreadSettingsClick = {
                            showWorkspaceActions = false
                            navController.navigate("thread_settings")
                        },
                        onTokenPanelClick = {
                            showTokenPanel = true
                        }
                    )
                }
                if (showGithubExport) {
                    GithubExportBottomSheet(
                        onDismiss = { showGithubExport = false }
                    )
                }
                
                if (showTokenPanel) {
                    com.example.ui.chat.AiTokenPanelBottomSheet(
                        onDismiss = { showTokenPanel = false }
                    )
                }
            }
            
            composable("thread_settings") {
                ThreadSettingsScreen(
                    workspaceId = chatSessionId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("log_keeper") {
                LogKeeperScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                GlobalSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateTo = { route -> 
                        navController.navigate(route) 
                        // Note: actual nested routes for settings aren't fully implemented in this phase
                    }
                )
            }
            composable("settings/omniroot") {
                com.example.ui.settings.omniroot.AiManagerPanelScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onAddKeyClick = { providerId ->
                        navController.navigate("settings/omniroot/add_key/$providerId")
                    }
                )
            }
            composable("settings/omniroot/add_key/{providerId}") { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: return@composable
                com.example.ui.settings.omniroot.DirectToKeyWebViewScreen(
                    providerId = providerId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("settings/audio") {
                com.example.ui.settings.AudioSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("settings/{subRoute}") { backStackEntry ->
                val subRoute = backStackEntry.arguments?.getString("subRoute") ?: "Unknown"
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(subRoute.replaceFirstChar { it.uppercase() }) },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        when (subRoute) {
                            "skills" -> com.example.ui.settings.SkillsSettingsContent()
                            "tools" -> com.example.ui.settings.ToolsSettingsContent()
                            "mcp" -> com.example.ui.settings.MCPSettingsContent()
                            "plugins" -> com.example.ui.settings.PluginsSettingsContent()
                            "memory_modules" -> com.example.ui.settings.MemoryModulesSettingsContent()
                            "github", "firebase", "gdrive" -> com.example.ui.settings.IntegrationsSettingsContent()
                            "permissions" -> com.example.ui.settings.PermissionsSettingsContent()
                            "font" -> com.example.ui.settings.FontSettingsContent()
                            "backup" -> com.example.ui.settings.BackupSettingsContent()
                            "editor" -> com.example.ui.settings.EditorSettingsContent()
                            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Settings content for $subRoute (Pending implementation)") }
                        }
                    }
                }
            }
        }
    }
}
