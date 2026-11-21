package dev.lisek.openyi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
actual fun RtspPreview(
    url: String,
    modifier: Modifier,
    onStateChanged: (RtspPlayerState) -> Unit
) {
    val context = LocalContext.current
    val currentOnStateChanged = rememberUpdatedState(onStateChanged)
    val player = remember(url) {
        ExoPlayer.Builder(context).build().also { exoPlayer ->
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    currentOnStateChanged.value(
                        when (playbackState) {
                            Player.STATE_BUFFERING -> RtspPlayerState.Buffering
                            Player.STATE_READY -> RtspPlayerState.Playing
                            else -> RtspPlayerState.Idle
                        }
                    )
                }

                override fun onPlayerError(error: PlaybackException) {
                    currentOnStateChanged.value(RtspPlayerState.Error)
                }
            })
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = false
            }
        },
        update = { it.player = player }
    )
}
