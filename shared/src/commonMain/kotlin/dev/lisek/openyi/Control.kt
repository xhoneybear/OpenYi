package dev.lisek.openyi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Close
import com.composables.icons.materialsymbols.roundedfilled.Photo_camera
import com.composables.icons.materialsymbols.roundedfilled.Sd_card_alert
import com.composables.icons.materialsymbols.roundedfilled.Sensors
import com.composables.icons.materialsymbols.roundedfilled.Settings
import com.composables.icons.materialsymbols.roundedfilled.Timer_10_alt_1
import com.composables.icons.materialsymbols.roundedfilled.Timer_3_alt_1
import com.composables.icons.materialsymbols.roundedfilled.Timer_5
import com.composables.icons.materialsymbols.roundedfilled.Timer_off
import com.composables.icons.materialsymbols.roundedfilled.Videocam
import com.composables.icons.materialsymbols.roundedfilled.Warning

@Composable
fun Control(onSettingsClick: () -> Unit) {
    var playerState by remember { mutableStateOf(RtspPlayerState.Disconnected) }
    var timer by remember { mutableStateOf(TimerDuration.OFF) }
    var recordMode by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }

    val cameraClient = remember { CameraClient.shared }
    val connectionState by cameraClient.connectionState.collectAsState()
    val isRecording by cameraClient.isRecording.collectAsState()
    val sdCardMessage by cameraClient.sdCardAlert.collectAsState()

    LaunchedEffect(isRecording) {
        if (!isRecording) {
            playerState = RtspPlayerState.Disconnected
        }
    }

    // Connect on initial entry only if disconnected (reusing existing connection from Intro)
    LaunchedEffect(Unit) {
        cameraClient.connectAndInitialize(forceReconnect = false)
    }

    Scaffold(
        topBar = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .windowInsetsPadding(
                        WindowInsets
                            .safeContent
                            .only(WindowInsetsSides.Top)
                    )
            ) {
                Row(Modifier.size(48.dp)) {}
                Text(
                    text = "OpenYi",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(
                    onClick = { onSettingsClick() },
                ) {
                    Icon(
                        imageVector = MaterialSymbols.RoundedFilled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .windowInsetsPadding(
                        WindowInsets
                            .safeContent
                            .only(WindowInsetsSides.Bottom)
                    )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .clickable { recordMode = !recordMode }
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.RoundedFilled.Photo_camera,
                            contentDescription = "Photo Mode",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .background(
                                    if (recordMode) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    CircleShape
                                )
                                .padding(8.dp)
                        )
                        Icon(
                            imageVector = MaterialSymbols.RoundedFilled.Videocam,
                            contentDescription = "Video Mode",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .background(
                                    if (recordMode) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    CircleShape
                                )
                                .padding(8.dp)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(modifier = Modifier
                        .size(64.dp)
                        .border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                        .padding(6.dp)
                    ) {
                        Button(
                            modifier = Modifier.fillMaxSize(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (recordMode) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            ),
                            shape = CircleShape,
                            onClick = { /* Handle capture click */ }
                        ) {}
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { timer = timer.next() }
                    ) {
                        Icon(
                            imageVector = timer.icon,
                            contentDescription = "Timer",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            val cameraHost = cameraClient.host
            Box {
                if (isRecording) {
                    RtspPreview(
                        url = "rtsp://$cameraHost:554/live",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    ) { state ->
                        playerState = state
                        localErrorMessage = state.errorMessage()
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    )
                }
                Overlay(cameraClient, playerState)
            }
            val activeErrorMessage = localErrorMessage ?: connectionState.errorMessage
            activeErrorMessage?.let {
                statusStrip(
                    icon = MaterialSymbols.RoundedFilled.Warning,
                    dismissIcon = MaterialSymbols.RoundedFilled.Close,
                    message = it,
                    tint = MaterialTheme.colorScheme.errorContainer,
                ) {
                    localErrorMessage = null
                }
            }
            sdCardMessage?.let {
                statusStrip(
                    icon = MaterialSymbols.RoundedFilled.Sd_card_alert,
                    dismissIcon = MaterialSymbols.RoundedFilled.Close,
                    message = it,
                    tint = Color(255, 240, 215)
                ) {
                    cameraClient.clearSdCardAlert()
                }
            }
            ConnectionStrip()
        }
    }
}

@Composable
fun statusStrip(icon: ImageVector, dismissIcon: ImageVector, message: String, tint: Color, modifier: Modifier = Modifier, onDismiss: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .background(tint)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        IconButton(
            modifier = Modifier.size(24.dp),
            onClick = onDismiss
        ) {
            Icon(
                imageVector = dismissIcon,
                contentDescription = "Dismiss"
            )
        }
    }
}
