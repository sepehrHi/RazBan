package com.joghdstudio.razban.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joghdstudio.razban.data.LocalDictionary
import kotlinx.coroutines.delay

@Composable
fun EmergencyNotification(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dict = LocalDictionary.current

    // Auto-dismiss and progress state
    var progress by remember(visible) { mutableStateOf(1f) }

    LaunchedEffect(visible) {
        if (visible) {
            progress = 1f
            val duration = 5000L
            val step = 50L
            val stepsCount = duration / step
            for (i in 1..stepsCount) {
                delay(step)
                progress = 1f - (i.toFloat() / stepsCount)
            }
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .border(
                    BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFD50000),
                                Color(0xFFFF6D00),
                                Color(0xFFD50000)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Pulsing/Glowing warning icon container
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .background(
                                color = Color(0xFFD50000).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Emergency Warning",
                            tint = Color(0xFFD50000),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Text Content
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = dict.emergencyNotificationTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD50000),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dict.emergencyNotificationMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp,
                            fontSize = 12.sp
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Smooth decreasing progress bar at the bottom
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = Color(0xFFD50000),
                    trackColor = Color(0xFFD50000).copy(alpha = 0.15f)
                )
            }
        }
    }
}
