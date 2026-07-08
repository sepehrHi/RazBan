package com.joghdstudio.razban

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import com.joghdstudio.razban.data.AppLanguage
import com.joghdstudio.razban.data.EnglishDictionary
import com.joghdstudio.razban.data.LocalDictionary
import com.joghdstudio.razban.data.NetworkClient
import com.joghdstudio.razban.data.NetworkMode
import com.joghdstudio.razban.data.PersianDictionary
import com.joghdstudio.razban.ui.screens.AuthScreen
import com.joghdstudio.razban.ui.screens.ChatRoomScreen
import com.joghdstudio.razban.ui.screens.MainScreen
import com.joghdstudio.razban.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var activeToken: String = ""
    private var activeNetworkMode: NetworkMode = NetworkMode.NORMAL

    override fun onDestroy() {
        if (activeToken.isNotEmpty()) {
            val tok = activeToken
            val mode = activeNetworkMode
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                NetworkClient.sendHeartbeat(tok, "offline", mode)
            }
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkTheme by remember { mutableStateOf(true) }
            var appLanguage by remember { mutableStateOf(AppLanguage.FA) }

            MyApplicationTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalDictionary provides if (appLanguage == AppLanguage.FA) PersianDictionary else EnglishDictionary,
                    LocalLayoutDirection provides appLanguage.layoutDirection
                ) {
                    val coroutineScope = rememberCoroutineScope()
                    var currentScreen by remember { mutableStateOf("auth") }
                    
                    var token by remember { mutableStateOf("") }
                    var displayName by remember { mutableStateOf("") }
                    var username by remember { mutableStateOf("") }

                    var currentNetworkMode by remember { mutableStateOf(NetworkMode.NORMAL) }

                    SideEffect {
                        activeToken = token
                        activeNetworkMode = currentNetworkMode
                    }

                    LaunchedEffect(token, currentNetworkMode) {
                        if (token.isNotEmpty()) {
                            while (true) {
                                NetworkClient.sendHeartbeat(token, "online", currentNetworkMode)
                                kotlinx.coroutines.delay(45000)
                            }
                        }
                    }

                    var activeChatId by remember { mutableStateOf("") }
                    var activeChatTitle by remember { mutableStateOf("") }
                    var activeChatType by remember { mutableStateOf("private") }

                    LaunchedEffect(Unit) {
                        NetworkClient.loadPersonalServers(this@MainActivity)
                        val savedToken = NetworkClient.getSavedToken(this@MainActivity)
                        val savedDisplayName = NetworkClient.getSavedDisplayName(this@MainActivity)
                        val savedUsername = NetworkClient.getSavedUsername(this@MainActivity)
                        if (savedToken != null && savedDisplayName != null && savedUsername != null) {
                            token = savedToken
                            displayName = savedDisplayName
                            username = savedUsername
                            currentScreen = "main"
                        }
                    }

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        when (currentScreen) {
                            "auth" -> {
                                AuthScreen(
                                    onAuthSuccess = { userToken, name, uName ->
                                        token = userToken
                                        displayName = name
                                        username = uName
                                        currentScreen = "main"
                                    },
                                    darkTheme = darkTheme,
                                    onToggleTheme = { darkTheme = !darkTheme },
                                    appLanguage = appLanguage,
                                    onToggleLanguage = {
                                        appLanguage = if (appLanguage == AppLanguage.FA) AppLanguage.EN else AppLanguage.FA
                                    }
                                )
                            }
                            "main" -> {
                                MainScreen(
                                    token = token,
                                    displayName = displayName,
                                    username = username,
                                    currentMode = currentNetworkMode,
                                    onModeChange = { newMode -> currentNetworkMode = newMode },
                                    onChatSelected = { id, title, type ->
                                        activeChatId = id
                                        activeChatTitle = title
                                        activeChatType = type
                                        currentScreen = "chatroom"
                                    },
                                    onLogout = {
                                        val oldToken = token
                                        val oldMode = currentNetworkMode
                                        coroutineScope.launch {
                                            NetworkClient.sendHeartbeat(oldToken, "offline", oldMode)
                                        }
                                        NetworkClient.clearAuth(this@MainActivity)
                                        token = ""
                                        displayName = ""
                                        username = ""
                                        currentScreen = "auth"
                                    },
                                    darkTheme = darkTheme,
                                    onToggleTheme = { darkTheme = !darkTheme },
                                    appLanguage = appLanguage,
                                    onToggleLanguage = {
                                        appLanguage = if (appLanguage == AppLanguage.FA) AppLanguage.EN else AppLanguage.FA
                                    }
                                )
                            }
                            "chatroom" -> {
                                ChatRoomScreen(
                                    chatId = activeChatId,
                                    chatTitle = activeChatTitle,
                                    chatType = activeChatType,
                                    token = token,
                                    myUsername = username,
                                    currentMode = currentNetworkMode,
                                    onBack = { currentScreen = "main" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
