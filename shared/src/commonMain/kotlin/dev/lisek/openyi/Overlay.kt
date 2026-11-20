package dev.lisek.openyi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Fullscreen

@Composable
fun Overlay(cameraClient: CameraClient, playerState: RtspPlayerState) {
    val batteryInfo by cameraClient.batteryInfo.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text(
                text = batteryInfo.displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Icon(
                imageVector = batteryInfo.icon,
                contentDescription = "Battery Icon",
                tint = Color.White
            )
        }
        playerState.icon(
            Modifier
                .padding(4.dp)
                .align(Alignment.BottomStart)
        )
        IconButton(
            onClick = { /* Handle fullscreen click */ },
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.BottomEnd)
        ) {
            Icon(
                imageVector = MaterialSymbols.RoundedFilled.Fullscreen,
                contentDescription = "Fullscreen",
                tint = Color.White
            )
        }
    }
}
