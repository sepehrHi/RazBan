package com.joghdstudio.razban.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.joghdstudio.razban.data.LocalDictionary
import com.joghdstudio.razban.data.MessageResponse
import com.joghdstudio.razban.data.NetworkClient
import com.joghdstudio.razban.data.NetworkMode
import com.joghdstudio.razban.data.AndroidAudioRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatId: String,
    chatTitle: String,
    chatType: String,
    token: String,
    myUsername: String,
    currentMode: NetworkMode,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val dict = LocalDictionary.current

    val audioRecorder = remember { AndroidAudioRecorder(context) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    var messages by remember { mutableStateOf<List<MessageResponse>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoadingMessages by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableStateOf(0) }

    var showAttachmentDialog by remember { mutableStateOf(false) }

    var replyToMessage by remember { mutableStateOf<MessageResponse?>(null) }
    var editingMessage by remember { mutableStateOf<MessageResponse?>(null) }

    var chatStatus by remember { mutableStateOf("offline") }
    var chatLastSeen by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chatId) {
        while (true) {
            try {
                val chats = NetworkClient.fetchChats(token, currentMode)
                val currentChat = chats.firstOrNull { it.id == chatId }
                if (currentChat != null) {
                    chatStatus = currentChat.status ?: "offline"
                    chatLastSeen = currentChat.lastSeen
                }
            } catch (e: Exception) {
                // Silently ignore
            }
            delay(15000)
        }
    }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingDurationSeconds = 0
            while (isRecordingVoice) {
                delay(1000)
                recordingDurationSeconds++
            }
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = File(context.cacheDir, "voice_record_${System.currentTimeMillis()}.mp4")
            recordingFile = file
            audioRecorder.start(file)
            isRecordingVoice = true
        } else {
            Toast.makeText(context, dict.microphonePermissionRequired, Toast.LENGTH_SHORT).show()
        }
    }

    fun loadMessages(silent: Boolean = false) {
        if (!silent) {
            isLoadingMessages = true
        }
        coroutineScope.launch {
            try {
                val fetchedResult = NetworkClient.fetchMessages(chatId, token, currentMode)
                val fetched = fetchedResult.messages
                val hasNewMessages = fetched.size != messages.size
                messages = fetched
                
                // Update presence info!
                val presence = fetchedResult.presence
                if (presence != null) {
                    chatStatus = presence.status ?: "offline"
                    chatLastSeen = presence.lastSeen
                }

                if (!silent && messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                } else if (silent && hasNewMessages && messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            } catch (e: Exception) {
                // Ignore background silent errors
            } finally {
                if (!silent) {
                    isLoadingMessages = false
                }
            }
        }
    }

    LaunchedEffect(chatId, currentMode) {
        loadMessages()
        while (true) {
            kotlinx.coroutines.delay(5000)
            loadMessages(silent = true)
        }
    }

    fun handleSendText() {
        if (inputText.isBlank()) return
        val textToSend = inputText
        val replyId = replyToMessage?.id
        replyToMessage = null
        inputText = ""
        isSending = true

        val myNewMessage = MessageResponse(
            id = "temp_${System.currentTimeMillis()}",
            chatId = chatId,
            senderUsername = myUsername,
            content = textToSend,
            messageType = "text",
            timestamp = System.currentTimeMillis(),
            networkMode = currentMode.name.lowercase(),
            replyToId = replyId
        )

        messages = messages + myNewMessage
        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }

        coroutineScope.launch {
            val sentMsg = NetworkClient.sendTextMessage(chatId, token, textToSend, currentMode, replyToId = replyId)
            isSending = false
            if (sentMsg != null) {
                messages = messages.map { if (it.id == myNewMessage.id) sentMsg else it }
            } else {
                Toast.makeText(context, "ارسال به سرور با خطا مواجه شد؛ به صورت محلی ذخیره شد.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleSendMedia(mediaType: String) {
        // Real stream and real upload simulation
        val dummyFile = File(context.cacheDir, "razban_dummy_${System.currentTimeMillis()}.$mediaType")
        try {
            dummyFile.writeText("Razban file stream payload.")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        showAttachmentDialog = false
        isSending = true

        val targetType = when (mediaType) {
            "jpg" -> "photo"
            "mp4" -> "video"
            "mp3", "m4a", "wav" -> "voice"
            else -> "file"
        }

        coroutineScope.launch {
            val result = NetworkClient.sendMediaMessage(
                chatId = chatId,
                token = token,
                messageType = targetType,
                file = dummyFile,
                mode = currentMode
            )
            isSending = false
            if (result != null) {
                messages = messages + result
                listState.animateScrollToItem(messages.size - 1)
            } else {
                Toast.makeText(context, "خطا در آپلود فایل", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = chatTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (chatType == "group") {
                                Text(
                                    text = dict.chatStatusActive,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            } else {
                                val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
                                val isOnline = chatStatus == "online"
                                val statusLabel = if (isOnline) {
                                    if (isRtl) "آنلاین" else "Online"
                                } else {
                                    if (chatLastSeen != null) {
                                        if (isRtl) "آفلاین (آخرین بازدید: $chatLastSeen)" else "Offline (Last seen: $chatLastSeen)"
                                    } else {
                                        if (isRtl) "آفلاین" else "Offline"
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (isOnline) Color.Green else Color.Red,
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = statusLabel,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .background(
                                color = when (currentMode) {
                                    NetworkMode.NORMAL -> Color(0xFF00C853).copy(alpha = 0.15f)
                                    NetworkMode.TIMEOUT -> Color(0xFFFFD600).copy(alpha = 0.15f)
                                    NetworkMode.EMERGENCY -> Color(0xFFD50000).copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (currentMode) {
                                NetworkMode.NORMAL -> "🟢 " + dict.routingNormal
                                NetworkMode.TIMEOUT -> "🟡 " + dict.routingTimeout
                                NetworkMode.EMERGENCY -> "🔴 " + dict.routingEmergency
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (currentMode) {
                                NetworkMode.NORMAL -> Color(0xFF00C853)
                                NetworkMode.TIMEOUT -> Color(0xFFFFD600)
                                NetworkMode.EMERGENCY -> Color(0xFFD50000)
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoadingMessages && messages.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .testTag("message_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
                    ) {
                        items(messages) { message ->
                            val isMe = message.senderUsername == myUsername
                            val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
                            MessageBubble(
                                message = message,
                                isMe = isMe,
                                allMessages = messages,
                                onCopy = { msg ->
                                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("message", msg.content)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, if (isRtl) "کپی شد" else "Copied", Toast.LENGTH_SHORT).show()
                                },
                                onReply = { msg ->
                                    replyToMessage = msg
                                    editingMessage = null
                                },
                                onEdit = { msg ->
                                    editingMessage = msg
                                    replyToMessage = null
                                    inputText = msg.content
                                },
                                onDeleteForMe = { msg ->
                                    val messageId = msg.id ?: ""
                                    coroutineScope.launch {
                                        val success = NetworkClient.deleteMessage(messageId, "for_me", token, currentMode)
                                        if (success) {
                                            messages = messages.filter { it.id != msg.id }
                                            Toast.makeText(context, if (isRtl) "از لیست شما حذف شد" else "Deleted for you", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, if (isRtl) "خطا در حذف پیام" else "Error deleting message", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDeleteForEveryone = { msg ->
                                    val messageId = msg.id ?: ""
                                    coroutineScope.launch {
                                        val success = NetworkClient.deleteMessage(messageId, "for_everyone", token, currentMode)
                                        if (success) {
                                            messages = messages.filter { it.id != msg.id }
                                            Toast.makeText(context, if (isRtl) "برای همه حذف شد" else "Deleted for everyone", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, if (isRtl) "خطا در حذف پیام" else "Error deleting message", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (currentMode == NetworkMode.EMERGENCY) {
                Text(
                    text = dict.emergencyTextOnlyIndicator,
                    color = Color(0xFFD50000),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD50000).copy(alpha = 0.08f))
                        .padding(vertical = 6.dp)
                )
            } else if (currentMode == NetworkMode.TIMEOUT) {
                Text(
                    text = dict.fileLimitIndicator,
                    color = Color(0xFFFFD600),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFD600).copy(alpha = 0.08f))
                        .padding(vertical = 6.dp)
                )
            }

            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
                val isEmergency = currentMode == NetworkMode.EMERGENCY

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Preview Bar for Reply or Edit
                    if (replyToMessage != null || editingMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (replyToMessage != null) Icons.Default.Reply else Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (replyToMessage != null) {
                                        if (isRtl) "پاسخ به ${replyToMessage!!.senderUsername}" else "Reply to ${replyToMessage!!.senderUsername}"
                                    } else {
                                        if (isRtl) "در حال ویرایش پیام..." else "Editing message..."
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (replyToMessage != null) replyToMessage!!.content else editingMessage!!.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (editingMessage != null) {
                                        inputText = ""
                                    }
                                    replyToMessage = null
                                    editingMessage = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    }

                    // Input Bar Row (Swapped: Send/Mic is first, Attachment is last)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        // 1. Send / Mic Button (First in code, so RIGHT in RTL / LEFT in LTR)
                        if (inputText.isNotBlank() || editingMessage != null) {
                            IconButton(
                                onClick = {
                                    val editMsg = editingMessage
                                    if (editMsg != null) {
                                        val updatedContent = inputText
                                        val messageId = editMsg.id ?: ""
                                        editingMessage = null
                                        inputText = ""
                                        coroutineScope.launch {
                                            val success = NetworkClient.editMessage(messageId, updatedContent, token, currentMode)
                                            if (success) {
                                                messages = messages.map {
                                                    if (it.id == messageId) {
                                                        it.copy(content = updatedContent, isEdited = true)
                                                    } else {
                                                        it
                                                    }
                                                }
                                                Toast.makeText(context, if (isRtl) "پیام ویرایش شد" else "Message edited", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, if (isRtl) "خطا در ویرایش پیام" else "Error editing message", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        handleSendText()
                                    }
                                },
                                modifier = Modifier
                                    .testTag("send_button")
                                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                                    contentDescription = if (editingMessage != null) "Save" else dict.sendButtonDesc,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, dict.featureUnderDevAlert, Toast.LENGTH_LONG).show()
                                },
                                enabled = !isEmergency,
                                modifier = Modifier
                                    .background(
                                        color = if (isEmergency) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        else if (isRecordingVoice) Color(0xFFD50000).copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecordingVoice) dict.stopMicButtonDesc else dict.micButtonDesc,
                                    tint = if (isEmergency) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    else if (isRecordingVoice) Color(0xFFD50000)
                                    else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 2. TextField Box (Middle)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (isRecordingVoice) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PulsingRecordIndicator()
                                    Text(
                                        text = "${dict.recordingVoice} ${recordingDurationSeconds / 60}:${String.format("%02d", recordingDurationSeconds % 60)}",
                                        fontSize = 13.sp,
                                        color = Color(0xFFD50000),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                TextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            dict.inputBarPlaceholder,
                                            fontSize = 14.sp
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("message_input_field"),
                                    maxLines = 4,
                                    singleLine = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 3. Attachment Button (Last in code, so LEFT in RTL / RIGHT in LTR)
                        IconButton(
                            onClick = { showAttachmentDialog = true },
                            enabled = !isEmergency,
                            modifier = Modifier
                                .testTag("attachment_button")
                                .background(
                                    color = if (isEmergency) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = dict.attachButtonDesc,
                                tint = if (isEmergency) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAttachmentDialog) {
        AlertDialog(
            onDismissRequest = { showAttachmentDialog = false },
            title = { Text(dict.selectAttachmentTitle) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = dict.uploadLimitWarning,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 1. Send Photo (Coming Soon)
                    Button(
                        onClick = {
                            Toast.makeText(context, dict.featureUnderDevAlert, Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📸", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dict.sendPhoto, fontWeight = FontWeight.SemiBold)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = dict.comingSoonBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // 2. Send Video (Coming Soon)
                    Button(
                        onClick = {
                            Toast.makeText(context, dict.featureUnderDevAlert, Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎥", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dict.sendVideo, fontWeight = FontWeight.SemiBold)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = dict.comingSoonBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // 3. Send Audio / Voice (Under Construction)
                    Button(
                        onClick = {
                            Toast.makeText(context, dict.featureUnderDevAlert, Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎙️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dict.sendAudio, fontWeight = FontWeight.SemiBold)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    text = dict.underDevBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // 4. Send Document (Active!)
                    Button(
                        onClick = { handleSendMedia("pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📄", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(dict.sendDocument, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachmentDialog = false }) {
                    Text(dict.cancelButton)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageResponse,
    isMe: Boolean,
    allMessages: List<MessageResponse> = emptyList(),
    onCopy: (MessageResponse) -> Unit,
    onReply: (MessageResponse) -> Unit,
    onEdit: (MessageResponse) -> Unit,
    onDeleteForMe: (MessageResponse) -> Unit,
    onDeleteForEveryone: (MessageResponse) -> Unit
) {
    val dict = LocalDictionary.current
    var showMenu by remember { mutableStateOf(false) }
    val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    val containerColor = if (isMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val borderStroke = if (isMe) {
        null
    } else {
        BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = containerColor,
            border = borderStroke,
            shadowElevation = if (isMe) 2.dp else 0.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onLongClick = { showMenu = true },
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (!isMe) {
                    Text(
                        text = message.senderUsername,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (message.replyToId != null) {
                    val repliedMsg = allMessages.firstOrNull { it.id == message.replyToId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .background(
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .background(
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                            else MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = repliedMsg?.senderUsername ?: "Reply",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = repliedMsg?.content ?: (if (isRtl) "پیام قبلی" else "Previous message"),
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = (if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                when (message.messageType) {
                    "photo" -> {
                        Column {
                            AsyncImage(
                                model = message.mediaUrl ?: "https://picsum.photos/400/300",
                                contentDescription = "Media attachment",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dict.attachmentPhotoLabel,
                                fontSize = 12.sp,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    "video" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = dict.playVideoLabel,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = dict.attachmentVideoLabel,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                    "voice" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                repeat(10) { index ->
                                    val height = remember { (12..32).random().dp }
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(height)
                                            .background(
                                                if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "0:07 🎙️",
                                fontSize = 11.sp,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    "file" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.Black.copy(alpha = 0.05f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = "File",
                                tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "razban_doc_archive.pdf",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "3.4 MB",
                                    fontSize = 10.sp,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = dict.downloadLabel,
                                tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    else -> {
                        Column {
                            Text(
                                text = message.content,
                                fontSize = 15.sp,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                            if (message.isEdited == true) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isRtl) "ویرایش شده" else "edited",
                                    fontSize = 10.sp,
                                    color = (if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.5f),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (isRtl) "کپی" else "Copy") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onCopy(message)
                }
            )
            DropdownMenuItem(
                text = { Text(if (isRtl) "ریپلای" else "Reply") },
                leadingIcon = { Icon(Icons.Default.Reply, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onReply(message)
                }
            )
            if (isMe && message.messageType == "text") {
                DropdownMenuItem(
                    text = { Text(if (isRtl) "ویرایش" else "Edit") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onEdit(message)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(if (isRtl) "پاک کردن برای من" else "Delete for me") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onDeleteForMe(message)
                }
            )
            if (isMe) {
                DropdownMenuItem(
                    text = { Text(if (isRtl) "پاک کردن برای هردو" else "Delete for everyone") },
                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onDeleteForEveryone(message)
                    }
                )
            }
        }
    }
}

@Composable
fun PulsingRecordIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(Color(0xFFD50000).copy(alpha = alpha))
    )
}
