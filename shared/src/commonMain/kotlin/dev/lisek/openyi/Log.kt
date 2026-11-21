package dev.lisek.openyi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Arrow_back
import com.composables.icons.materialsymbols.roundedfilled.Delete


interface LogEntry {
    val level: CameraLogLevel
    val message: String
}

@Composable
fun LogView(onBackPressed: () -> Unit) {
    val cameraClient = remember { CameraClient.shared }
    val debugLogs = remember { mutableStateListOf<CameraLogEntry>() }

    // Stream all client logs into the debug log view
    LaunchedEffect(cameraClient) {
        cameraClient.logs.collect { entry ->
            debugLogs.add(0, entry)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .windowInsetsPadding(
                        WindowInsets
                            .safeContent
                            .only(WindowInsetsSides.Top)
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onBackPressed() },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = MaterialSymbols.RoundedFilled.Arrow_back,
                        contentDescription = "Go back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "Debug Logs",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                IconButton(
                    onClick = { debugLogs.clear() },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = MaterialSymbols.RoundedFilled.Delete,
                        contentDescription = "Clear logs",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            ConnectionStrip()
            LazyColumn(
                contentPadding = PaddingValues(bottom = 8.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(debugLogs) { entry ->
                    val badgeColor = when (entry.level) {
                        CameraLogLevel.INFO -> MaterialTheme.colorScheme.primary
                        CameraLogLevel.TX -> Color(0xFF00838F)
                        CameraLogLevel.RX -> Color(0xFF2E7D32)
                        CameraLogLevel.WARN -> Color(0xFFE65100)
                        CameraLogLevel.ERROR -> MaterialTheme.colorScheme.error
                    }
                    val containerColor = badgeColor.copy(alpha = 0.12f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(containerColor, shape = MaterialTheme.shapes.small)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "[${entry.level.name}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = entry.message,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
