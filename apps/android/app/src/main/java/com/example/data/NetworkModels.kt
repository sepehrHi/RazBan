package com.joghdstudio.razban.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class NetworkMode {
    NORMAL, TIMEOUT, EMERGENCY
}

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String,
    @Json(name = "display_name") val displayName: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "token") val token: String?,
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "username") val username: String?,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class CreateChatRequest(
    @Json(name = "chat_type") val chatType: String,
    @Json(name = "target_user_id") val targetUserId: String
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "last_message") val lastMessage: String? = null,
    @Json(name = "last_message_time") val lastMessageTime: String? = null,
    @Json(name = "unread_count") val unreadCount: Int? = 0,
    @Json(name = "chat_type") val chatType: String? = "private",
    @Json(name = "status") val status: String? = "offline",
    @Json(name = "last_seen") val lastSeen: String? = null,
    @Json(name = "partner_status") val partnerStatus: String? = "offline",
    @Json(name = "partner_last_seen") val partnerLastSeen: String? = null
)

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "id") val id: Int,
    @Json(name = "username") val username: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "status") val status: String? = "offline",
    @Json(name = "last_seen") val lastSeen: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatParticipant(
    @Json(name = "id") val id: Int,
    @Json(name = "username") val username: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "status") val status: String? = "offline",
    @Json(name = "last_seen") val lastSeen: String? = null
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @Json(name = "chat_id") val chatId: String,
    @Json(name = "message_type") val messageType: String,
    @Json(name = "content") val content: String,
    @Json(name = "network_mode") val networkMode: String? = null,
    @Json(name = "token") val token: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "chat_id") val chatId: String,
    @Json(name = "sender_username") val senderUsername: String,
    @Json(name = "content") val content: String,
    @Json(name = "message_type") val messageType: String,
    @Json(name = "timestamp") val timestamp: Long? = null,
    @Json(name = "media_url") val mediaUrl: String? = null,
    @Json(name = "network_mode") val networkMode: String? = null,
    @Json(name = "reply_to_id") val replyToId: String? = null,
    @Json(name = "is_edited") val isEdited: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class PresenceData(
    @Json(name = "status") val status: String? = "offline",
    @Json(name = "last_seen") val lastSeen: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatMessagesResponse(
    @Json(name = "presence") val presence: PresenceData? = null,
    @Json(name = "messages") val messages: List<MessageResponse>? = null
)

data class FetchMessagesResult(
    val messages: List<MessageResponse>,
    val presence: PresenceData? = null
)

@JsonClass(generateAdapter = true)
data class MessagesListResponse(
    @Json(name = "messages") val messages: List<MessageResponse>? = null
)

@JsonClass(generateAdapter = true)
data class ChatsListResponse(
    @Json(name = "chats") val chats: List<ChatResponse>? = null
)

@JsonClass(generateAdapter = true)
data class UserSearchResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "username") val username: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "status") val status: String? = "offline",
    @Json(name = "last_seen") val lastSeen: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchUsersEnvelope(
    @Json(name = "success") val success: Boolean,
    @Json(name = "users") val users: List<UserSearchResponse>? = null
)

@JsonClass(generateAdapter = true)
data class CreateChatResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "chat_id") val chatId: Int? = null,
    @Json(name = "error") val error: String? = null
)


