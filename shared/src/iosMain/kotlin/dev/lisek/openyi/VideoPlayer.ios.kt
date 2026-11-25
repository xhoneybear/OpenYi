package dev.lisek.openyi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onStateChanged: (RtspPlayerState) -> Unit
) {
    VlcPlayerView(
        url = url,
        modifier = modifier,
        createView = { provider -> provider.createVideoView(url, onStateChanged) }
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
internal fun VlcPlayerView(
    url: String,
    modifier: Modifier,
    createView: (NativeRtspViewProvider) -> UIView
) {
    val provider = NativeRtspPlayerRegistry.provider
    if (provider == null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("RTSP player not initialized")
        }
        return
    }

    key(url) {
        UIKitView(
            modifier = modifier,
            factory = { createView(provider) },
            update = {},
            onRelease = { provider.releaseView(it) }
        )
    }
}
