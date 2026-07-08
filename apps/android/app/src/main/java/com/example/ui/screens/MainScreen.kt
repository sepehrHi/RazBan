package com.joghdstudio.razban.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joghdstudio.razban.data.AppLanguage
import com.joghdstudio.razban.data.ChatResponse
import com.joghdstudio.razban.data.LocalDictionary
import com.joghdstudio.razban.data.NetworkClient
import com.joghdstudio.razban.data.NetworkMode
import com.joghdstudio.razban.data.UserSearchResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    token: String,
    displayName: String,
    username: String,
    currentMode: NetworkMode,
    onModeChange: (NetworkMode) -> Unit,
    onChatSelected: (chatId: String, title: String, type: String) -> Unit,
    onLogout: () -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    appLanguage: AppLanguage,
    onToggleLanguage: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dict = LocalDictionary.current
    val context = LocalContext.current
    var chatList by remember { mutableStateOf<List<ChatResponse>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showCreateChatDialog by remember { mutableStateOf(false) }
    var newChatUsername by remember { mutableStateOf("") }
    var createChatError by remember { mutableStateOf<String?>(null) }
    var isCreatingChat by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserSearchResponse>>(emptyList()) }
    var showSearchResultCard by remember { mutableStateOf(false) }
    var isSearchingUser by remember { mutableStateOf(false) }
    var showEmergencyNotification by remember { mutableStateOf(false) }

    LaunchedEffect(currentMode) {
        if (currentMode == NetworkMode.EMERGENCY) {
            showEmergencyNotification = true
        }
    }

    fun refreshChats(silent: Boolean = false) {
        if (!silent) {
            isRefreshing = true
        }
        coroutineScope.launch {
            try {
                val fetched = NetworkClient.fetchChats(token, currentMode)
                chatList = fetched.filter { it.id != "razban_ai" && it.chatType != "ai" && !it.lastMessage.isNullOrEmpty() }
            } catch (e: Exception) {
                // Ignore silent/background errors
            } finally {
                if (!silent) {
                    isRefreshing = false
                }
            }
        }
    }

    LaunchedEffect(currentMode) {
        refreshChats()
        while (true) {
            kotlinx.coroutines.delay(7000)
            refreshChats(silent = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column(modifier = Modifier.padding(end = 16.dp)) {
                        Text(
                            text = dict.appName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${dict.currentUserLabel}: $displayName (@$username)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    // Quick Language Switcher Button
                    IconButton(onClick = onToggleLanguage) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = if (appLanguage == AppLanguage.FA) "EN" else "FA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // Theme Toggle Button
                    IconButton(onClick = onToggleTheme, modifier = Modifier.testTag("theme_toggle")) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    // Refresh Button
                    IconButton(onClick = { refreshChats() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = dict.refreshDesc)
                    }
                    // Logout Button
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("logout_button")) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = dict.logoutDesc,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showCreateChatDialog = true
                    newChatUsername = ""
                    createChatError = null
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_chat_fab")
            ) {
                Icon(imageVector = Icons.Default.Chat, contentDescription = dict.newChatTitle)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Network mode panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dict.routingModeTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Normal mode button
                        val normalSelected = currentMode == NetworkMode.NORMAL
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (normalSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable { onModeChange(NetworkMode.NORMAL) }
                                .testTag("mode_normal_tab")
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (normalSelected) MaterialTheme.colorScheme.primary else Color(0xFF00FFCC).copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dict.routingNormal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (normalSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // Timeout mode button
                        val timeoutSelected = currentMode == NetworkMode.TIMEOUT
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (timeoutSelected) Color(0xFFFFD600).copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable { onModeChange(NetworkMode.TIMEOUT) }
                                .testTag("mode_timeout_tab")
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (timeoutSelected) Color(0xFFFFD600) else Color(0xFFFFD600).copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dict.routingTimeout,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (timeoutSelected) Color(0xFFFFD600) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // Emergency mode button
                        val emergencySelected = currentMode == NetworkMode.EMERGENCY
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (emergencySelected) Color(0xFFD50000).copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    onModeChange(NetworkMode.EMERGENCY)
                                    showEmergencyNotification = true
                                }
                                .testTag("mode_emergency_tab")
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (emergencySelected) Color(0xFFD50000) else Color(0xFFD50000).copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dict.routingEmergency,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (emergencySelected) Color(0xFFD50000) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = when (currentMode) {
                                    NetworkMode.NORMAL -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    NetworkMode.TIMEOUT -> Color(0xFFFFD600).copy(alpha = 0.08f)
                                    NetworkMode.EMERGENCY -> Color(0xFFD50000).copy(alpha = 0.08f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 6.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = when (currentMode) {
                                NetworkMode.NORMAL -> dict.normalModeDesc
                                NetworkMode.TIMEOUT -> dict.timeoutModeDesc
                                NetworkMode.EMERGENCY -> dict.emergencyModeDesc
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (currentMode) {
                                NetworkMode.NORMAL -> MaterialTheme.colorScheme.primary
                                NetworkMode.TIMEOUT -> Color(0xFFFFD600)
                                NetworkMode.EMERGENCY -> Color(0xFFD50000)
                            }
                        )
                    }
                }
            }

            // Real-time "Find User" Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = dict.findUserLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(dict.findUserPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("user_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                isSearchingUser = true
                                searchResults = emptyList()
                                showSearchResultCard = false
                                coroutineScope.launch {
                                    val results = NetworkClient.searchUsers(token, searchQuery.trim(), currentMode)
                                    searchResults = results
                                    showSearchResultCard = true
                                    isSearchingUser = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        if (isSearchingUser) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(dict.findUserButton)
                        }
                    }
                }
            }

            // Animated Search Result Card
            AnimatedVisibility(visible = showSearchResultCard) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    if (searchResults.isEmpty()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (appLanguage == AppLanguage.FA) "کاربری یافت نشد" else "No matching users found",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = dict.searchResultsTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp)
                            )
                            searchResults.forEachIndexed { index, user ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            val isOnline = user.status == "online"
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .align(Alignment.BottomEnd)
                                                    .background(
                                                        color = if (isOnline) Color.Green else Color.Red,
                                                        shape = CircleShape
                                                    )
                                                    .then(
                                                        if (!isOnline) {
                                                            Modifier.border(
                                                                width = 1.dp,
                                                                color = Color.White,
                                                                shape = CircleShape
                                                            )
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = user.displayName,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "@${user.username}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    
                                    var isUserCreatingChat by remember { mutableStateOf(false) }
                                    
                                    Button(
                                        onClick = {
                                            isUserCreatingChat = true
                                            coroutineScope.launch {
                                                val response = NetworkClient.createChat(token, user.id.toString(), currentMode)
                                                isUserCreatingChat = false
                                                if (response != null) {
                                                    showSearchResultCard = false
                                                    searchQuery = ""
                                                    onChatSelected(response.id, user.displayName, response.chatType ?: "private")
                                                } else {
                                                    createChatError = dict.createChatFailed
                                                }
                                            }
                                        },
                                        enabled = !isUserCreatingChat,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isUserCreatingChat) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text(dict.createButton)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = dict.activeChats,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { refreshChats() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (chatList.isEmpty()) {
                        // High-fidelity Empty State layout
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = dict.noChatsEmptyState,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("chat_list")
                        ) {
                            items(chatList) { chat ->
                                ChatItem(
                                    chat = chat,
                                    onClick = {
                                        onChatSelected(chat.id, chat.title, chat.chatType ?: "private")
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateChatDialog) {
        AlertDialog(
            onDismissRequest = { showCreateChatDialog = false },
            title = { Text(dict.newChatTitle) },
            text = {
                Column {
                    Text(
                        dict.newChatDesc,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = newChatUsername,
                        onValueChange = { newChatUsername = it },
                        placeholder = { Text(dict.newChatPlaceholder) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_chat_username_field")
                    )
                    createChatError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChatUsername.isBlank()) {
                            createChatError = dict.usernameEmptyError
                            return@Button
                        }
                        isCreatingChat = true
                        createChatError = null

                        coroutineScope.launch {
                            val response = NetworkClient.createChat(token, newChatUsername.trim(), currentMode)
                            isCreatingChat = false
                            if (response != null) {
                                showCreateChatDialog = false
                                onChatSelected(response.id, response.title, response.chatType ?: "private")
                            } else {
                                createChatError = dict.createChatFailed
                            }
                        }
                    },
                    enabled = !isCreatingChat
                ) {
                    if (isCreatingChat) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(dict.createButton)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateChatDialog = false }) {
                    Text(dict.cancelButton)
                }
            }
        )
    }

    EmergencyNotification(
        visible = showEmergencyNotification,
        onDismiss = { showEmergencyNotification = false },
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 16.dp)
    )
}
}

@Composable
fun ChatItem(
    chat: ChatResponse,
    onClick: () -> Unit
) {
    val avatarColor = if (chat.unreadCount != null && chat.unreadCount > 0) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(avatarColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (chat.chatType == "group") Icons.Default.Group else Icons.Default.Person,
                contentDescription = chat.title,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )

            if (chat.chatType != "group") {
                val isOnline = chat.partnerStatus == "online"
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .background(
                            color = if (isOnline) Color.Green else Color.Red,
                            shape = CircleShape
                        )
                        .then(
                            if (!isOnline) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chat.lastMessageTime ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage ?: "",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (chat.unreadCount != null && chat.unreadCount > 0) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.unreadCount.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
