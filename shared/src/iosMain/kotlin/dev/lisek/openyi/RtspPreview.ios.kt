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

interface NativeRtspViewProvider {
    fun createView(url: String, onStateChanged: (RtspPlayerState) -> Unit): UIView
    fun releaseView(view: UIView)
}

object NativeRtspPlayerRegistry {
    var provider: NativeRtspViewProvider? = null
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RtspPreview(
    url: String,
    restartToken: Int,
    modifier: Modifier,
    onStateChanged: (RtspPlayerState) -> Unit
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

    key(url, restartToken) {
        UIKitView(
            modifier = modifier,
            factory = { provider.createView(url, onStateChanged) },
            update = {},
            onRelease = { provider.releaseView(it) }
        )
    }
}
