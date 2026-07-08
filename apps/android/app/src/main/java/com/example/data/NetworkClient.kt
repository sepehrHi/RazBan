package com.joghdstudio.razban.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkClient {
    private const val TAG = "NetworkClient"

    const val KEY_WORKER_URL = "personal_worker_url"
    const val KEY_RELAY_URL = "personal_relay_url"

    private var personalWorkerUrl: String? = null
    private var personalRelayUrl: String? = null

    fun loadPersonalServers(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        personalWorkerUrl = prefs.getString(KEY_WORKER_URL, null)
        personalRelayUrl = prefs.getString(KEY_RELAY_URL, null)
    }

    fun savePersonalServers(context: Context, workerUrl: String, relayUrl: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_WORKER_URL, workerUrl)
            .putString(KEY_RELAY_URL, relayUrl)
            .apply()
        personalWorkerUrl = workerUrl
        personalRelayUrl = relayUrl
    }

    fun clearPersonalServers(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_WORKER_URL)
            .remove(KEY_RELAY_URL)
            .apply()
        personalWorkerUrl = null
        personalRelayUrl = null
    }

    fun hasPersonalServers(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_WORKER_URL, null).isNullOrBlank() &&
               !prefs.getString(KEY_RELAY_URL, null).isNullOrBlank()
    }

    fun getWorkerUrl(): String {
        return personalWorkerUrl ?: ""
    }

    fun getRelayUrl(): String {
        return personalRelayUrl ?: ""
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // --- SharedPreferences Auth Storage ---
    private const val PREFS_NAME = "razban_messenger_prefs"
    private const val KEY_TOKEN = "security_token"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_USERNAME = "username"

    fun saveAuth(context: Context, token: String, displayName: String, username: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getSavedToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)
    }

    fun getSavedDisplayName(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_DISPLAY_NAME, null)
    }

    fun getSavedUsername(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USERNAME, null)
    }

    fun clearAuth(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    // --- Authentication REST Calls ---
    suspend fun register(request: RegisterRequest): AuthResponse = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(RegisterRequest::class.java)
            val jsonBody = adapter.toJson(request)
            val reqBody = jsonBody.toRequestBody(jsonMediaType)

            val req = Request.Builder()
                .url("${getWorkerUrl()}/api/register")
                .post(reqBody)
                .build()

            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Register Response Code: ${response.code}, Body: $bodyStr")
                if (response.isSuccessful) {
                    val respAdapter = moshi.adapter(AuthResponse::class.java)
                    respAdapter.fromJson(bodyStr) ?: AuthResponse(null, null, null, "پاسخ نامعتبر سرور")
                } else {
                    AuthResponse(null, null, null, parseError(bodyStr) ?: "خطا در ثبت‌نام (کد ${response.code})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Register error", e)
            AuthResponse(null, null, null, "خطای ارتباط: ${e.localizedMessage}")
        }
    }

    suspend fun login(request: LoginRequest): AuthResponse = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(LoginRequest::class.java)
            val jsonBody = adapter.toJson(request)
            val reqBody = jsonBody.toRequestBody(jsonMediaType)

            val req = Request.Builder()
                .url("${getWorkerUrl()}/api/login")
                .post(reqBody)
                .build()

            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Login Response Code: ${response.code}, Body: $bodyStr")
                if (response.isSuccessful) {
                    val respAdapter = moshi.adapter(AuthResponse::class.java)
                    respAdapter.fromJson(bodyStr) ?: AuthResponse(null, null, null, "پاسخ نامعتبر سرور")
                } else {
                    AuthResponse(null, null, null, parseError(bodyStr) ?: "رمز عبور یا یوزرنیم اشتباه است")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            AuthResponse(null, null, null, "خطای ارتباط: ${e.localizedMessage}")
        }
    }

    suspend fun emergencyLogin(request: LoginRequest): AuthResponse = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf(
                "action" to "login",
                "username" to request.username,
                "password" to request.password
            )
            val jsonBody = moshi.adapter(Map::class.java).toJson(payload)
            val reqBody = jsonBody.toRequestBody(jsonMediaType)

            val req = Request.Builder()
                .url(getRelayUrl())
                .post(reqBody)
                .build()

            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Emergency Login Response Code: ${response.code}, Body: $bodyStr")
                if (response.isSuccessful) {
                    val respAdapter = moshi.adapter(AuthResponse::class.java)
                    respAdapter.fromJson(bodyStr) ?: AuthResponse(null, null, null, "پاسخ نامعتبر سرور")
                } else {
                    AuthResponse(null, null, null, parseError(bodyStr) ?: "نام کاربری یا رمز عبور اشتباه است.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Emergency Login error", e)
            AuthResponse(null, null, null, "اتصال برقرار نشد، اتصال اینترنت را بررسی کنید")
        }
    }

    suspend fun emergencyRegister(request: RegisterRequest): AuthResponse = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf(
                "action" to "register",
                "username" to request.username,
                "password" to request.password,
                "display_name" to request.displayName
            )
            val jsonBody = moshi.adapter(Map::class.java).toJson(payload)
            val reqBody = jsonBody.toRequestBody(jsonMediaType)

            val req = Request.Builder()
                .url(getRelayUrl())
                .post(reqBody)
                .build()

            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Emergency Register Response Code: ${response.code}, Body: $bodyStr")
                if (response.isSuccessful || response.code == 201) {
                    val respAdapter = moshi.adapter(AuthResponse::class.java)
                    respAdapter.fromJson(bodyStr) ?: AuthResponse(null, null, null, "پاسخ نامعتبر سرور")
                } else {
                    AuthResponse(null, null, null, parseError(bodyStr) ?: "ثبت‌نام ناموفق بود")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Emergency Register error", e)
            AuthResponse(null, null, null, "اتصال برقرار نشد، اتصال اینترنت را بررسی کنید")
        }
    }

    // --- User Search ---
    suspend fun searchUsers(token: String, query: String, mode: NetworkMode): List<UserSearchResponse> = withContext(Dispatchers.IO) {
        try {
            val url = when (mode) {
                NetworkMode.NORMAL -> {
                    "${getWorkerUrl()}/api/users/search?query=$query"
                }
                NetworkMode.TIMEOUT, NetworkMode.EMERGENCY -> {
                    "${getRelayUrl()}?action=users_search&query=$query&token=$token"
                }
            }

            val reqBuilder = Request.Builder().url(url)
            if (mode == NetworkMode.NORMAL) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }
            val req = reqBuilder.get().build()

            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Search Users ($mode) Code: ${response.code}, Body: $bodyStr")
                if (response.isSuccessful) {
                    moshi.adapter(SearchUsersEnvelope::class.java).fromJson(bodyStr)?.users ?: emptyList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search users error on mode $mode", e)
            emptyList()
        }
    }

    // --- Creating New Chat ---
    suspend fun createChat(token: String, targetUserIdOrUsername: String, mode: NetworkMode): ChatResponse? = withContext(Dispatchers.IO) {
        try {
            var targetUserIdInt = targetUserIdOrUsername.toIntOrNull()
            var partnerDisplayName = ""
            if (targetUserIdInt == null) {
                // It's a username! Let's search for this user first
                Log.d(TAG, "createChat: targetUserIdOrUsername is not an integer. Searching for username: $targetUserIdOrUsername")
                val foundUsers = searchUsers(token, targetUserIdOrUsername, mode)
                val matchingUser = foundUsers.firstOrNull { it.username.equals(targetUserIdOrUsername, ignoreCase = true) }
                    ?: foundUsers.firstOrNull()
                if (matchingUser != null) {
                    targetUserIdInt = matchingUser.id
                    partnerDisplayName = matchingUser.displayName
                    Log.d(TAG, "createChat: Found matching user ID: $targetUserIdInt")
                } else {
                    Log.e(TAG, "createChat: No user found for username: $targetUserIdOrUsername")
                    return@withContext null
                }
            } else {
                // It's an ID. Let's see if we can find a matching user from a quick search to get their displayName
                try {
                    val foundUsers = searchUsers(token, targetUserIdOrUsername, mode)
                    val matchingUser = foundUsers.firstOrNull { it.id == targetUserIdInt }
                    if (matchingUser != null) {
                        partnerDisplayName = matchingUser.displayName
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not resolve partner display name for ID: $targetUserIdInt")
                }
            }

            val url = when (mode) {
                NetworkMode.NORMAL -> "${getWorkerUrl()}/api/chats/create"
                NetworkMode.TIMEOUT, NetworkMode.EMERGENCY -> getRelayUrl()
            }

            val jsonBody = when (mode) {
                NetworkMode.NORMAL -> {
                    """
                    {
                        "chat_type": "private",
                        "target_user_id": $targetUserIdInt
                    }
                    """.trimIndent()
                }
                NetworkMode.TIMEOUT, NetworkMode.EMERGENCY -> {
                    """
                    {
                        "action": "chats_create",
                        "token": "$token",
                        "chat_type": "private",
                        "target_user_id": $targetUserIdInt
                    }
                    """.trimIndent()
                }
            }

            val reqBody = jsonBody.toRequestBody(jsonMediaType)
            val reqBuilder = Request.Builder().url(url)
            if (mode == NetworkMode.NORMAL) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }
            val req = reqBuilder.post(reqBody).build()

            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Create Chat ($mode) Code: ${response.code}, Body: $bodyStr")
                if (response.isSuccessful) {
                    val createResponse = moshi.adapter(CreateChatResponse::class.java).fromJson(bodyStr)
                    if (createResponse != null && createResponse.success && createResponse.chatId != null) {
                        ChatResponse(
                            id = createResponse.chatId.toString(),
                            title = if (partnerDisplayName.isNotEmpty()) partnerDisplayName else "Chat #${createResponse.chatId}",
                            chatType = "private"
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Create chat error on mode $mode", e)
            null
        }
    }

    // --- Fetching Chats ---
    suspend fun fetchChats(token: String, mode: NetworkMode): List<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            val url = when (mode) {
                NetworkMode.NORMAL -> {
                    "${getWorkerUrl()}/api/chats"
                }
                NetworkMode.TIMEOUT, NetworkMode.EMERGENCY -> {
                    "${getRelayUrl()}?action=chats_list&token=$token"
                }
            }

            val reqBuilder = Request.Builder().url(url)
            if (mode == NetworkMode.NORMAL) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }
            val req = reqBuilder.get().build()

            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Fetch Chats ($mode) Code: ${response.code}, Body: $bodyStr")
                if (response.isSuccessful) {
                    val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, ChatResponse::class.java)
                    val adapter = moshi.adapter<List<ChatResponse>>(listType)
                    adapter.fromJson(bodyStr) ?: emptyList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch chats error on mode $mode", e)
            emptyList()
        }
    }

    // --- Dynamic Messaging Operations ---

    // 1. Fetching Messages
    suspend fun fetchMessages(
        chatId: String,
        token: String,
        mode: NetworkMode
    ): FetchMessagesResult = withContext(Dispatchers.IO) {
        try {
            val url = when (mode) {
                NetworkMode.NORMAL -> {
                    "${getWorkerUrl()}/api/messages/get?chat_id=$chatId"
                }
                NetworkMode.TIMEOUT, NetworkMode.EMERGENCY -> {
                    "${getRelayUrl()}?action=messages_get&chat_id=$chatId&token=$token"
                }
            }

            val reqBuilder = Request.Builder().url(url)
            if (mode == NetworkMode.NORMAL) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }
            val req = reqBuilder.get().build()

            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Fetch Messages ($mode) Code: ${response.code}")
                if (response.isSuccessful) {
                    // Try parsing as ChatMessagesResponse first (new structured wrapper)
                    try {
                        val chatMsgResponse = moshi.adapter(ChatMessagesResponse::class.java).fromJson(bodyStr)
                        if (chatMsgResponse != null) {
                            return@withContext FetchMessagesResult(
                                messages = chatMsgResponse.messages ?: emptyList(),
                                presence = chatMsgResponse.presence
                            )
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Failed to parse as ChatMessagesResponse, trying fallbacks", e)
                    }

                    // Fallbacks:
                    // 1. Direct List
                    try {
                        val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, MessageResponse::class.java)
                        val messagesList = moshi.adapter<List<MessageResponse>>(listType).fromJson(bodyStr) ?: emptyList()
                        FetchMessagesResult(messages = messagesList)
                    } catch (ex: Exception) {
                        // 2. Wrapped messages
                        val wrapAdapter = moshi.adapter(MessagesListResponse::class.java)
                        val messagesList = wrapAdapter.fromJson(bodyStr)?.messages ?: emptyList()
                        FetchMessagesResult(messages = messagesList)
                    }
                } else {
                    FetchMessagesResult(messages = emptyList())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch messages error on mode $mode", e)
            FetchMessagesResult(messages = emptyList())
        }
    }

    // 2. Sending Text Messages
    suspend fun sendTextMessage(
        chatId: String,
        token: String,
        content: String,
        mode: NetworkMode,
        replyToId: String? = null
    ): MessageResponse? = withContext(Dispatchers.IO) {
        try {
            val url: String
            val reqBody: RequestBody
            val reqBuilder = Request.Builder()

            val chatIdValue = chatId.toIntOrNull() ?: chatId

            when (mode) {
                NetworkMode.NORMAL -> {
                    url = "${getWorkerUrl()}/api/messages/send"
                    if (replyToId != null) {
                        val multipartBuilder = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("chat_id", chatIdValue.toString())
                            .addFormDataPart("message_type", "text")
                            .addFormDataPart("content", content)
                            .addFormDataPart("network_mode", "normal")
                            .addFormDataPart("reply_to_id", replyToId)
                        reqBody = multipartBuilder.build()
                    } else {
                        val payload = mapOf(
                            "chat_id" to chatIdValue,
                            "message_type" to "text",
                            "content" to content,
                            "network_mode" to "normal"
                        )
                        val jsonBody = moshi.adapter(Map::class.java).toJson(payload)
                        reqBody = jsonBody.toRequestBody(jsonMediaType)
                    }
                    reqBuilder.addHeader("Authorization", "Bearer $token")
                }
                NetworkMode.TIMEOUT -> {
                    url = getRelayUrl()
                    val payload = mutableMapOf<String, Any>(
                        "action" to "messages_send",
                        "chat_id" to chatIdValue,
                        "message_type" to "text",
                        "content" to content,
                        "network_mode" to "timeout",
                        "token" to token
                    )
                    if (replyToId != null) {
                        payload["reply_to_id"] = replyToId
                    }
                    val jsonBody = moshi.adapter(Map::class.java).toJson(payload)
                    reqBody = jsonBody.toRequestBody(jsonMediaType)
                }
                NetworkMode.EMERGENCY -> {
                    url = getRelayUrl()
                    val payload = mutableMapOf<String, Any>(
                        "action" to "messages_send",
                        "chat_id" to chatIdValue,
                        "message_type" to "text",
                        "content" to content,
                        "network_mode" to "emergency",
                        "token" to token
                    )
                    if (replyToId != null) {
                        payload["reply_to_id"] = replyToId
                    }
                    val jsonBody = moshi.adapter(Map::class.java).toJson(payload)
                    reqBody = jsonBody.toRequestBody(jsonMediaType)
                }
            }

            val req = reqBuilder.url(url).post(reqBody).build()

            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Send Text ($mode) Code: ${response.code}, Body: $bodyStr")
                if (response.isSuccessful) {
                    val mapAdapter = moshi.adapter(Map::class.java)
                    val map = try { mapAdapter.fromJson(bodyStr) } catch (e: Exception) { null }
                    val success = map?.get("success") as? Boolean ?: false

                    if (success) {
                        val messageObj = map?.get("message")
                        if (messageObj != null) {
                            val msgJson = moshi.adapter(Any::class.java).toJson(messageObj)
                            moshi.adapter(MessageResponse::class.java).fromJson(msgJson)
                        } else {
                            try {
                                moshi.adapter(MessageResponse::class.java).fromJson(bodyStr)
                            } catch (e: Exception) {
                                MessageResponse(
                                    id = "msg_${System.currentTimeMillis()}",
                                    chatId = chatId,
                                    senderUsername = "",
                                    content = content,
                                    messageType = "text",
                                    timestamp = System.currentTimeMillis(),
                                    networkMode = mode.name.lowercase(),
                                    replyToId = replyToId
                                )
                            }
                        }
                    } else {
                        try {
                            moshi.adapter(MessageResponse::class.java).fromJson(bodyStr)
                        } catch (e: Exception) {
                            null
                        }
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Send text error on mode $mode", e)
            null
        }
    }

    // 2.1. Editing Text Messages
    suspend fun editMessage(
        messageId: String,
        content: String,
        token: String,
        mode: NetworkMode
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url: String
            val reqBody: RequestBody
            val reqBuilder = Request.Builder()

            when (mode) {
                NetworkMode.NORMAL -> {
                    url = "${getWorkerUrl()}/api/messages/edit"
                    val payload = mapOf(
                        "message_id" to messageId,
                        "content" to content
                    )
                    val jsonBody = moshi.adapter(Map::class.java).toJson(payload)
                    reqBody = jsonBody.toRequestBody(jsonMediaType)
                    reqBuilder.addHeader("Authorization", "Bearer $token")
                }
                NetworkMode.TIMEOUT, NetworkMode.EMERGENCY -> {
                    url = getRelayUrl()
                    val payload = mapOf(
                        "action" to "messages_edit",
                        "token" to token,
                        "message_id" to messageId,
                        "content" to content
                    )
                    val jsonBody = moshi.adapter(Map::class.java).toJson(payload)
                    reqBody = jsonBody.toRequestBody(jsonMediaType)
                }
            }

            val req = reqBuilder.url(url).post(reqBody).build()
            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Edit Message ($mode) Code: ${response.code}, Body: $bodyStr")
                if (response.isSuccessful) {
                    val map = try { moshi.adapter(Map::class.java).fromJson(bodyStr) } catch (e: Exception) { null }
                    map?.get("success") as? Boolean ?: true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Edit message error on mode $mode", e)
            false
        }
    }

    // 2.2. Deleting Messages
    suspend fun deleteMessage(
        messageId: String,
        deleteType: String, // "for_me" or "for_everyone"
        token: String,
        mode: NetworkMode
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url: String
            val reqBody: RequestBody
            val reqBuilder = Request.Builder()

            when (mode) {
                NetworkMode.NORMAL -> {
                    url = "${getWorkerUrl()}/api/messages/delete"
                    val payload = mapOf(
                        "message_id" to messageId,
                        "delete_type" to deleteType
                    )
                    val jsonBody = moshi.adapter(Map::class.java).toJson(payload)
                    reqBody = jsonBody.toRequestBody(jsonMediaType)
                    reqBuilder.addHeader("Authorization", "Bearer $token")
                }
                NetworkMode.TIMEOUT, NetworkMode.EMERGENCY -> {
                    url = getRelayUrl()
                    val payload = mapOf(
                        "action" to "messages_delete",
                        "token" to token,
                        "message_id" to messageId,
                        "delete_type" to deleteType
                    )
                    val jsonBody = moshi.adapter(Map::class.java).toJson(payload)
                    reqBody = jsonBody.toRequestBody(jsonMediaType)
                }
            }

            val req = reqBuilder.url(url).post(reqBody).build()
            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Delete Message ($mode) Code: ${response.code}, Body: $bodyStr")
                if (response.isSuccessful) {
                    val map = try { moshi.adapter(Map::class.java).fromJson(bodyStr) } catch (e: Exception) { null }
                    map?.get("success") as? Boolean ?: true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete message error on mode $mode", e)
            false
        }
    }

    // 3. Sending Media Files
    suspend fun sendMediaMessage(
        chatId: String,
        token: String,
        messageType: String, // "voice", "video", "photo", "file"
        file: File,
        mode: NetworkMode
    ): MessageResponse? = withContext(Dispatchers.IO) {
        try {
            if (mode == NetworkMode.EMERGENCY) {
                Log.w(TAG, "Media message blocked in EMERGENCY mode")
                return@withContext null
            }

            val fileBody = file.asRequestBody("application/octet-stream".toMediaType())
            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("message_type", messageType)
                .addFormDataPart("file", file.name, fileBody)

            val url: String
            val reqBuilder = Request.Builder()

            if (mode == NetworkMode.NORMAL) {
                url = "${getWorkerUrl()}/api/messages/send"
                multipartBuilder.addFormDataPart("network_mode", "normal")
                reqBuilder.addHeader("Authorization", "Bearer $token")
            } else {
                // Timeout mode - Restricts sizes up to 20MB in UI.
                url = getRelayUrl()
                multipartBuilder.addFormDataPart("token", token)
            }

            val reqBody = multipartBuilder.build()
            val req = reqBuilder.url(url).post(reqBody).build()

            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Send Media ($mode) Code: ${response.code}, Body: $bodyStr")
                if (response.isSuccessful) {
                    moshi.adapter(MessageResponse::class.java).fromJson(bodyStr)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Send media error on mode $mode", e)
            null
        }
    }

    private fun parseError(body: String): String? {
        return try {
            val json = moshi.adapter(Map::class.java).fromJson(body)
            json?.get("error")?.toString()
        } catch (e: Exception) {
            null
        }
    }

    // 4. Background Heartbeat
    suspend fun sendHeartbeat(
        token: String,
        status: String, // "online" or "offline"
        mode: NetworkMode
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url: String
            val reqBody: RequestBody
            val reqBuilder = Request.Builder()

            when (mode) {
                NetworkMode.NORMAL -> {
                    url = "${getWorkerUrl()}/api/users/heartbeat"
                    val payload = mapOf(
                        "status" to status
                    )
                    val jsonBody = moshi.adapter(Map::class.java).toJson(payload)
                    reqBody = jsonBody.toRequestBody(jsonMediaType)
                    reqBuilder.addHeader("Authorization", "Bearer $token")
                }
                NetworkMode.TIMEOUT, NetworkMode.EMERGENCY -> {
                    url = getRelayUrl()
                    val payload = mapOf(
                        "action" to "users_heartbeat",
                        "token" to token,
                        "status" to status
                    )
                    val jsonBody = moshi.adapter(Map::class.java).toJson(payload)
                    reqBody = jsonBody.toRequestBody(jsonMediaType)
                }
            }

            val req = reqBuilder.url(url).post(reqBody).build()
            client.newCall(req).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Heartbeat ($mode, status: $status) Code: ${response.code}, Body: $bodyStr")
                response.isSuccessful
            }
        } catch (e: Exception) {
            // Heartbeats fail silently as requested
            Log.e(TAG, "Heartbeat error ($status) on mode $mode", e)
            false
        }
    }
}
