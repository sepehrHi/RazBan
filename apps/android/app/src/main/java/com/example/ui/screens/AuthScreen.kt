package com.joghdstudio.razban.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joghdstudio.razban.data.AppLanguage
import com.joghdstudio.razban.data.LocalDictionary
import com.joghdstudio.razban.data.LoginRequest
import com.joghdstudio.razban.data.NetworkClient
import com.joghdstudio.razban.data.RegisterRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: (token: String, displayName: String, username: String) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    appLanguage: AppLanguage,
    onToggleLanguage: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dict = LocalDictionary.current

    var isLoginTab by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    var isEmergencyMode by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    var hasServers by remember { mutableStateOf(NetworkClient.hasPersonalServers(context)) }
    var workerInput by remember { mutableStateOf("") }
    var relayInput by remember { mutableStateOf("") }
    var serverError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language selection toggle
                Surface(
                    onClick = onToggleLanguage,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = if (appLanguage == AppLanguage.FA) "EN" else "FA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("theme_toggle_auth")
                ) {
                    Icon(
                        imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Theme Toggle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "آر",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = dict.appName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = dict.appSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            if (!hasServers) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_config_card"),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "سرور شخصی (Personal Server)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "لطفاً برای اتصال به شبکه و استفاده از برنامه، آدرس سرور شخصی خود را وارد کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = workerInput,
                            onValueChange = { 
                                workerInput = it
                                serverError = null 
                            },
                            label = { Text("Worker URL") },
                            placeholder = { Text("https://example.workers.dev") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Worker") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("worker_url_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = relayInput,
                            onValueChange = { 
                                relayInput = it
                                serverError = null 
                            },
                            label = { Text("Relay URL") },
                            placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Relay") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("relay_url_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        serverError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val trimmedWorker = workerInput.trim()
                                val trimmedRelay = relayInput.trim()

                                if (trimmedWorker.isEmpty() || trimmedRelay.isEmpty()) {
                                    serverError = "لطفاً هر دو فیلد را تکمیل کنید."
                                    return@Button
                                }

                                val urlPattern = "^(https?://).+".toRegex()
                                if (!trimmedWorker.matches(urlPattern) || !trimmedRelay.matches(urlPattern)) {
                                    serverError = "آدرس‌ها باید معتبر بوده و با http یا https شروع شوند."
                                    return@Button
                                }

                                NetworkClient.savePersonalServers(context, trimmedWorker, trimmedRelay)
                                hasServers = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_server_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ذخیره تنظیمات سرور",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_card"),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TabRow(
                        selectedTabIndex = if (isLoginTab) 0 else 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .background(Color.Transparent),
                        divider = {}
                    ) {
                        Tab(
                            selected = isLoginTab,
                            onClick = {
                                isLoginTab = true
                                errorMessage = null
                            },
                            text = { Text(dict.login, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        )
                        Tab(
                            selected = !isLoginTab,
                            onClick = {
                                isLoginTab = false
                                errorMessage = null
                            },
                            text = { Text(dict.register, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        )
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(dict.usernameLabel) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("username_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    AnimatedVisibility(visible = !isLoginTab) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text(dict.displayNameLabel) },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = "Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("display_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(dict.passwordLabel) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Password Visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("password_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    AnimatedVisibility(visible = isEmergencyMode) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFD50000).copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFD50000).copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Emergency Warning",
                                    tint = Color(0xFFD50000),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "حالت اضطراری فعال است. فرآیند ورود/ثبت‌نام از طریق سرور ریلی کمکی برای دور زدن محدودیت‌ها انجام خواهد شد.",
                                    color = Color(0xFFD50000),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }

                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank() || (!isLoginTab && displayName.isBlank())) {
                                errorMessage = dict.requiredFieldsError
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null

                            coroutineScope.launch {
                                if (isLoginTab) {
                                    val response = if (isEmergencyMode) {
                                        NetworkClient.emergencyLogin(LoginRequest(username, password))
                                    } else {
                                        NetworkClient.login(LoginRequest(username, password))
                                    }
                                    isLoading = false
                                    if (response.token != null) {
                                        NetworkClient.saveAuth(
                                            context,
                                            response.token,
                                            response.displayName ?: username,
                                            response.username ?: username
                                        )
                                        onAuthSuccess(
                                            response.token,
                                            response.displayName ?: username,
                                            response.username ?: username
                                        )
                                    } else {
                                        errorMessage = response.error ?: dict.loginFailed
                                    }
                                } else {
                                    val response = if (isEmergencyMode) {
                                        NetworkClient.emergencyRegister(
                                            RegisterRequest(username, password, displayName)
                                        )
                                    } else {
                                        NetworkClient.register(
                                            RegisterRequest(username, password, displayName)
                                        )
                                    }
                                    isLoading = false
                                    if (response.token != null) {
                                        NetworkClient.saveAuth(
                                            context,
                                            response.token,
                                            response.displayName ?: displayName,
                                            response.username ?: username
                                        )
                                        onAuthSuccess(
                                            response.token,
                                            response.displayName ?: displayName,
                                            response.username ?: username
                                        )
                                    } else {
                                        errorMessage = response.error ?: dict.registerFailed
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_auth_button"),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEmergencyMode) Color(0xFFD50000) else MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isLoginTab) dict.submitLogin else dict.submitRegister,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEmergencyMode = !isEmergencyMode }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Emergency Mode",
                            tint = if (isEmergencyMode) Color(0xFFD50000) else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ورود اضطراری 🆘",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isEmergencyMode) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isEmergencyMode) Color(0xFFD50000) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isEmergencyMode,
                            onCheckedChange = { isEmergencyMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFD50000),
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }

            if (hasServers) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        NetworkClient.clearPersonalServers(context)
                        hasServers = false
                        workerInput = ""
                        relayInput = ""
                    },
                    modifier = Modifier.testTag("reset_server_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Server")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reset Server",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
