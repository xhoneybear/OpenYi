package dev.lisek.openyi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Link_off
import com.composables.icons.materialsymbols.roundedfilled.Refresh


@Composable
fun ConnectionStrip() {
    val coroutineScope = rememberCoroutineScope()
    val cameraClient = remember { CameraClient.shared }
    val connectionState by cameraClient.connectionState.collectAsState()
    val isConnected by cameraClient.isConnected.collectAsState()
    val isConnecting = connectionState.isConnecting
    val scanState by cameraClient.discoveryState.collectAsState()
    val isScanning by cameraClient.isScanning.collectAsState()

    fun triggerReconnect() {
        coroutineScope.launch {
            cameraClient.connectAndInitialize(forceReconnect = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with status indicator and quick actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isConnected -> Color(0xFF4CAF50)
                                isConnecting -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                )
                Text(
                    text = when {
                        isConnected -> "Connected"
                        isConnecting -> "Connecting..."
                        else -> "Disconnected"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        isConnected -> Color(0xFF2E7D32)
                        isConnecting -> Color(0xFFE65100)
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = "(${cameraClient.host}:${cameraClient.port})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            runCatching { cameraClient.stopSession() }
                        }
                    },
                    enabled = isConnected && !isConnecting,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = MaterialSymbols.RoundedFilled.Link_off,
                        contentDescription = "Stop Session",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { triggerReconnect() },
                    enabled = !isConnecting,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = MaterialSymbols.RoundedFilled.Refresh,
                        contentDescription = "Reconnect",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
