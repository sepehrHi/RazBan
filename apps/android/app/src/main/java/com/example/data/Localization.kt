package com.joghdstudio.razban.data

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(val code: String, val layoutDirection: LayoutDirection) {
    EN("en", LayoutDirection.Ltr),
    FA("fa", LayoutDirection.Rtl)
}

interface Dictionary {
    val appName: String
    val appSubtitle: String
    val login: String
    val register: String
    val usernameLabel: String
    val passwordLabel: String
    val displayNameLabel: String
    val showPassword: String
    val hidePassword: String
    val requiredFieldsError: String
    val loginFailed: String
    val registerFailed: String
    val connectionError: String
    val submitLogin: String
    val submitRegister: String
    val routingModeTitle: String
    val routingNormal: String
    val routingTimeout: String
    val routingEmergency: String
    val normalModeDesc: String
    val timeoutModeDesc: String
    val emergencyModeDesc: String
    val activeChats: String
    val newChatTitle: String
    val newChatDesc: String
    val newChatPlaceholder: String
    val usernameEmptyError: String
    val createButton: String
    val cancelButton: String
    val chatStatusActive: String
    val inputBarPlaceholder: String
    val emergencyTextOnlyIndicator: String
    val fileLimitIndicator: String
    val sendButtonDesc: String
    val attachButtonDesc: String
    val micButtonDesc: String
    val stopMicButtonDesc: String
    val recordingVoice: String
    val selectAttachmentTitle: String
    val uploadLimitWarning: String
    val sendPhoto: String
    val sendVideo: String
    val sendAudio: String
    val sendDocument: String
    val attachmentPhotoLabel: String
    val attachmentVideoLabel: String
    val playVideoLabel: String
    val videoSimulatedLabel: String
    val downloadLabel: String
    val currentUserLabel: String
    val logoutDesc: String
    val refreshDesc: String
    val findUserLabel: String
    val findUserPlaceholder: String
    val findUserButton: String
    val createChatSuccess: String
    val createChatFailed: String
    val microphonePermissionRequired: String
    val searchResultsTitle: String
    val noChatsEmptyState: String
    val emergencyNotificationTitle: String
    val emergencyNotificationMessage: String
    val comingSoonBadge: String
    val underDevBadge: String
    val featureUnderDevAlert: String
}

object EnglishDictionary : Dictionary {
    override val appName = "Razban Messenger"
    override val appSubtitle = "Secure intelligent messenger with resilient network routing"
    override val login = "Login"
    override val register = "Register"
    override val usernameLabel = "Username (Latin)"
    override val passwordLabel = "Password"
    override val displayNameLabel = "Display Name"
    override val showPassword = "Show Password"
    override val hidePassword = "Hide Password"
    override val requiredFieldsError = "Please fill in all fields."
    override val loginFailed = "Login failed"
    override val registerFailed = "Registration failed"
    override val connectionError = "Connection error"
    override val submitLogin = "Sign In"
    override val submitRegister = "Create Account"
    override val routingModeTitle = "Network Routing Configuration"
    override val routingNormal = "Normal"
    override val routingTimeout = "Timeout"
    override val routingEmergency = "Emergency"
    override val normalModeDesc = "🟢 Directly connected to Cloudflare Workers - Full Bandwidth"
    override val timeoutModeDesc = "🟡 Routed via Google Services - Anti-blocking & Timeout Bypass"
    override val emergencyModeDesc = "🔴 ONLY text messaging is allowed (Emergency relay active)"
    override val activeChats = "Active Chats"
    override val newChatTitle = "Start a New Chat"
    override val newChatDesc = "Enter target user's username to start messaging:"
    override val newChatPlaceholder = "e.g. sohrab_99"
    override val usernameEmptyError = "Username cannot be empty."
    override val createButton = "Create"
    override val cancelButton = "Cancel"
    override val chatStatusActive = "Active"
    override val inputBarPlaceholder = "Write a message..."
    override val emergencyTextOnlyIndicator = "Only text messages are allowed in emergency mode"
    override val fileLimitIndicator = "Timeout Mode Active • File attachment size limit: 20MB"
    override val sendButtonDesc = "Send"
    override val attachButtonDesc = "Attach File"
    override val micButtonDesc = "Record voice"
    override val stopMicButtonDesc = "Stop recording"
    override val recordingVoice = "Recording voice..."
    override val selectAttachmentTitle = "Select Attachment"
    override val uploadLimitWarning = "Maximum allowed upload size: 20MB"
    override val sendPhoto = "Send Photo 📸"
    override val sendVideo = "Send Video 🎥"
    override val sendAudio = "Send Voice Message 🎙️"
    override val sendDocument = "Send Document / Archive 📄"
    override val attachmentPhotoLabel = "📸 Photo Attachment"
    override val attachmentVideoLabel = "🎥 Video Attachment"
    override val playVideoLabel = "Play Video"
    override val videoSimulatedLabel = "Simulated Video Player"
    override val downloadLabel = "Download"
    override val currentUserLabel = "User"
    override val logoutDesc = "Logout"
    override val refreshDesc = "Refresh"
    override val findUserLabel = "Find User (Real-time)"
    override val findUserPlaceholder = "Enter exact username (e.g. sohrab_99)"
    override val findUserButton = "Find"
    override val createChatSuccess = "Chat opened successfully"
    override val createChatFailed = "Failed to create chat with this user"
    override val microphonePermissionRequired = "Microphone record permission is required to send voice messages."
    override val searchResultsTitle = "Search Results"
    override val noChatsEmptyState = "No active conversations. Search for a user above to start chatting!"
    override val emergencyNotificationTitle = "🚨 Emergency Routing Mode"
    override val emergencyNotificationMessage = "This mode is designed for national internet outages to keep communication alive. It is currently under active development."
    override val comingSoonBadge = "Coming Soon"
    override val underDevBadge = "Under Construction"
    override val featureUnderDevAlert = "This feature is currently under active development. Stay tuned for future releases!"
}

object PersianDictionary : Dictionary {
    override val appName = "رازبان مسنجر"
    override val appSubtitle = "پیام‌رسان هوشمند امن با مسیریابی ضد اختلال"
    override val login = "ورود"
    override val register = "ثبت‌نام"
    override val usernameLabel = "یوزرنیم (لاتین)"
    override val passwordLabel = "رمز عبور"
    override val displayNameLabel = "اسم نمایشی (فارسی)"
    override val showPassword = "نمایش رمز"
    override val hidePassword = "مخفی کردن رمز"
    override val requiredFieldsError = "لطفاً تمام فیلدها را کامل کنید."
    override val loginFailed = "رمز عبور یا یوزرنیم اشتباه است"
    override val registerFailed = "ثبت‌نام ناموفق بود"
    override val connectionError = "خطای ارتباط با سرور"
    override val submitLogin = "ورود به حساب"
    override val submitRegister = "ساخت حساب جدید"
    override val routingModeTitle = "وضعیت مسیریابی شبکه (پویا)"
    override val routingNormal = "عادی"
    override val routingTimeout = "تایماوت"
    override val routingEmergency = "اضطراری"
    override val normalModeDesc = "🟢 متصل مستقیم به Cloudflare Workers - پهنای باند کامل"
    override val timeoutModeDesc = "🟡 رله شده از Google Services - ضد فیلترینگ و تایم‌اوت"
    override val emergencyModeDesc = "🔴 فقط ارسال متن مجاز است (رله اضطراری فعال)"
    override val activeChats = "گفتگوهای فعال"
    override val newChatTitle = "ایجاد گفتگوی جدید"
    override val newChatDesc = "برای شروع گفتگو، یوزرنیم کاربر مورد نظر را وارد کنید:"
    override val newChatPlaceholder = "مثلا: sohrab_99"
    override val usernameEmptyError = "یوزرنیم نمی‌تواند خالی باشد."
    override val createButton = "ایجاد"
    override val cancelButton = "انصراف"
    override val chatStatusActive = "پویا • وضعیت فعال"
    override val inputBarPlaceholder = "پیام خود را بنویسید..."
    override val emergencyTextOnlyIndicator = "فقط ارسال متن در حالت اضطراری مجاز است"
    override val fileLimitIndicator = "حالت تایم‌اوت فعال است • محدودیت حجم فایل تا ۲۰ مگابایت"
    override val sendButtonDesc = "ارسال"
    override val attachButtonDesc = "پیوست فایل"
    override val micButtonDesc = "ضبط صدا"
    override val stopMicButtonDesc = "توقف ضبط"
    override val recordingVoice = "در حال ضبط ویس..."
    override val selectAttachmentTitle = "انتخاب فایل ضمیمه"
    override val uploadLimitWarning = "حداکثر اندازه مجاز فایل: ۲۰ مگابایت"
    override val sendPhoto = "ارسال عکس 📸"
    override val sendVideo = "ارسال ویدیو 🎥"
    override val sendAudio = "ارسال فایل صوتی ویس 🎙️"
    override val sendDocument = "ارسال سند و فایل عمومی 📄"
    override val attachmentPhotoLabel = "📸 تصویر ضمیمه"
    override val attachmentVideoLabel = "🎥 ویدیو ضمیمه (شبیه‌ساز)"
    override val playVideoLabel = "پخش ویدیو"
    override val videoSimulatedLabel = "پخش‌کننده ویدیو شبیه‌ساز"
    override val downloadLabel = "دانلود"
    override val currentUserLabel = "کاربر"
    override val logoutDesc = "خروج"
    override val refreshDesc = "بروزرسانی"
    override val findUserLabel = "پیدا کردن کاربر (واقعی)"
    override val findUserPlaceholder = "یوزرنیم دقیق را وارد کنید (مثال: sohrab_99)"
    override val findUserButton = "پیدا کن"
    override val createChatSuccess = "گفتگو با موفقیت باز شد"
    override val createChatFailed = "خطا در ایجاد گفتگو با این کاربر"
    override val microphonePermissionRequired = "برای ارسال پیام صوتی، دسترسی به میکروفون الزامی است."
    override val searchResultsTitle = "نتیجه جستجو"
    override val noChatsEmptyState = "هیچ گفتگوی فعالی وجود ندارد. با جستجو و پیدا کردن کاربر در بالا، چت خود را آغاز کنید!"
    override val emergencyNotificationTitle = "🚨 وضعیت رله اضطراری فعال شد"
    override val emergencyNotificationMessage = "این قابلیت تاب‌آور برای مواقع قطعی کامل اینترنت طراحی شده و در حال حاضر در دست توسعه (بزودی) می‌باشد."
    override val comingSoonBadge = "به‌زودی"
    override val underDevBadge = "در دست ساخت"
    override val featureUnderDevAlert = "این بخش در حال توسعه است و به‌زودی در نسخه‌های بعدی فعال خواهد شد!"
}

val LocalDictionary = staticCompositionLocalOf<Dictionary> { PersianDictionary }
