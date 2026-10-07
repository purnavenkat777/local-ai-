package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DiyaScreen
import com.example.ui.DiyaViewModel
import com.example.ui.DiyaViewModelFactory
import com.example.ui.components.DiyaDrawer
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ConversationsScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.ModelsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: DiyaViewModel by viewModels {
        val app = application as DiyaApplication
        DiyaViewModelFactory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DiyaApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DiyaApp(viewModel: DiyaViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Handle back button when navigating secondary screens
    if (uiState.currentScreen != DiyaScreen.CHAT) {
        BackHandler {
            viewModel.navigateTo(DiyaScreen.CHAT)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DiyaDrawer(
                currentScreen = uiState.currentScreen,
                onSelectScreen = { screen ->
                    viewModel.navigateTo(screen)
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (uiState.currentScreen) {
                DiyaScreen.CHAT -> {
                    ChatScreen(
                        state = uiState,
                        onSendMessage = { text -> viewModel.sendMessage(text) },
                        onStopGeneration = { viewModel.stopGeneration() },
                        onRetryHealth = { viewModel.checkConnectionHealth() },
                        onOpenNav = { scope.launch { drawerState.open() } },
                        onNewChat = { viewModel.createNewChat() },
                        onConfirmTool = { confirmed -> viewModel.confirmToolAction(confirmed) },
                        onOpenSettings = { viewModel.navigateTo(DiyaScreen.SETTINGS) }
                    )
                }
                DiyaScreen.CONVERSATIONS -> {
                    ConversationsScreen(
                        conversations = conversations,
                        selectedId = uiState.selectedConversationId,
                        onSelectConversation = { id -> viewModel.selectConversation(id) },
                        onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                        onNewChat = { viewModel.createNewChat() },
                        onClose = { viewModel.navigateTo(DiyaScreen.CHAT) }
                    )
                }
                DiyaScreen.MEMORY -> {
                    MemoryScreen(
                        memories = memories,
                        onAddMemory = { k, v, c, imp -> viewModel.addMemory(k, v, c, imp) },
                        onUpdateMemory = { mem -> viewModel.updateMemory(mem) },
                        onDeleteMemory = { id -> viewModel.deleteMemory(id) },
                        onClose = { viewModel.navigateTo(DiyaScreen.CHAT) }
                    )
                }
                DiyaScreen.MODELS -> {
                    ModelsScreen(
                        discoveredModels = uiState.discoveredModels,
                        activeModelName = uiState.runtimeConfig.activeModelName,
                        onSelectModel = { model -> viewModel.selectModel(model) },
                        onRescanModels = { viewModel.checkConnectionHealth() },
                        onClose = { viewModel.navigateTo(DiyaScreen.CHAT) }
                    )
                }
                DiyaScreen.SETTINGS -> {
                    SettingsScreen(
                        config = uiState.runtimeConfig,
                        status = uiState.connectionStatus,
                        isRunningDiagnostics = uiState.isRunningDiagnostics,
                        onSaveConfig = { cfg -> viewModel.updateConfig(cfg) },
                        onClearConversations = { viewModel.clearAllConversations() },
                        onClearMemories = { viewModel.clearAllMemories() },
                        onRunDiagnostics = { viewModel.runDiagnostics() },
                        onClose = { viewModel.navigateTo(DiyaScreen.CHAT) }
                    )
                }
            }
        }
    }
}
