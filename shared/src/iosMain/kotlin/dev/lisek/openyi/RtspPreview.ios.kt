package dev.lisek.openyi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import platform.UIKit.UIView

interface NativeRtspViewProvider {
    fun createView(url: String, onStateChanged: (RtspPlayerState) -> Unit): UIView
    fun createVideoView(url: String, onStateChanged: (RtspPlayerState) -> Unit): UIView
    fun releaseView(view: UIView)
}

object NativeRtspPlayerRegistry {
    var provider: NativeRtspViewProvider? = null
}

@Composable
actual fun RtspPreview(
    url: String,
    modifier: Modifier,
    onStateChanged: (RtspPlayerState) -> Unit
) {
    VlcPlayerView(
        url = url,
        modifier = modifier,
        createView = { provider -> provider.createView(url, onStateChanged) }
    )
}
