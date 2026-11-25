package dev.lisek.openyi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Close

/**
 * Fullscreen viewer overlay for photos and videos stored on the camera.
 */
@Composable
fun MediaViewer(
    media: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraClient = CameraClient.shared
    val mediaUrl = "http://${cameraClient.host}/DCIM/101MEDIA/$media"
    val isVideo = media.endsWith(".MP4", ignoreCase = true) ||
        media.endsWith(".MOV", ignoreCase = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        if (isVideo) {
            var playerState by remember(mediaUrl) { mutableStateOf(RtspPlayerState.Buffering) }

            VideoPlayer(
                url = mediaUrl,
                modifier = Modifier.fillMaxSize(),
                onStateChanged = { playerState = it }
            )

            when (playerState) {
                RtspPlayerState.Buffering -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                RtspPlayerState.Error -> {
                    Text(
                        text = "Failed to load video",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> Unit
            }
        } else {
            var isLoading by remember(mediaUrl) { mutableStateOf(true) }
            var hasError by remember(mediaUrl) { mutableStateOf(false) }

            AsyncImage(
                model = mediaUrl,
                contentDescription = media,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                onLoading = {
                    isLoading = true
                    hasError = false
                },
                onSuccess = {
                    isLoading = false
                    hasError = false
                },
                onError = { state ->
                    isLoading = false
                    hasError = true
                    cameraClient.log(
                        CameraLogLevel.ERROR,
                        "Failed to load $mediaUrl: ${state.result.throwable.message ?: state.result.throwable}"
                    )
                }
            )

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (hasError) {
                Text(
                    text = "Failed to load image",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .windowInsetsPadding(
                    WindowInsets.safeContent.only(WindowInsetsSides.Top)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = media,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = MaterialSymbols.RoundedFilled.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}
