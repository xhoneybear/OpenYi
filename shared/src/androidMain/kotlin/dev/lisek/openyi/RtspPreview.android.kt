package dev.lisek.openyi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun RtspPreview(
    url: String,
    modifier: Modifier,
    onStateChanged: (RtspPlayerState) -> Unit
) {
    ExoPlayerView(
        url = url,
        useController = false,
        modifier = modifier,
        onStateChanged = onStateChanged
    )
}
