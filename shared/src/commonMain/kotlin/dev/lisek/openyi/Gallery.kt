package dev.lisek.openyi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Image
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray

@Composable
fun Gallery() {
    val cameraClient = CameraClient.shared
    val galleryItems = getGalleryItems()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(galleryItems.entries.toList(), key = { it.key }) { (media, thumbnail) ->
            val file = "http://${cameraClient.host}/DCIM/101MEDIA/$thumbnail"
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(Color.Gray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = MaterialSymbols.RoundedFilled.Image,
                        contentDescription = "Image placeholder",
                        tint = Color.Gray.copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = media,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                AsyncImage(
                    model = file,
                    contentDescription = media,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { state ->
                        cameraClient.log(
                            CameraLogLevel.WARN,
                            "Failed to load $file: ${state.result.throwable.message ?: state.result.throwable}"
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun getGalleryItems(): Map<String, String> {
    val cameraClient = CameraClient.shared
    val connectionState by cameraClient.connectionState.collectAsState()
    val fileList by produceState(
        emptyMap<String, String>(),
        connectionState
    ) {
        if (connectionState !is CameraConnectionState.Connected) {
            return@produceState
        }
        try {
            val packet = cameraClient.listFiles("/tmp/fuse_d/DCIM/101MEDIA")
            if (packet.rval == 0) {
                value = buildMap {
                    packet.listing?.jsonArray?.forEach { element ->
                        if (element is JsonObject) {
                            for (key in element.keys) {
                                if (key.endsWith(".JPG", ignoreCase = true)) {
                                    put(key, key)
                                } else if (key.endsWith(".MP4", ignoreCase = true)) {
                                    put(key, key.replace(".MP4", ".THM", ignoreCase = true))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}
    }
    return fileList
}
