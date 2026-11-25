package dev.lisek.openyi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray

@Composable
fun Gallery() {
    val galleryItems = getGalleryItems()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(galleryItems) { item ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun getGalleryItems(): List<String> {
    val cameraClient = CameraClient.shared
    val connectionState by cameraClient.connectionState.collectAsState()
    val fileList by produceState(initialValue = emptyList<String>(), connectionState) {
        if (connectionState !is CameraConnectionState.Connected) {
            return@produceState
        }
        try {
            val packet = cameraClient.listFiles("/tmp/fuse_d/DCIM/101MEDIA")
            if (packet.rval == 0) {
                value = packet.listing?.jsonArray?.flatMap { element ->
                    when (element) {
                        is JsonObject -> element.keys
                        is JsonPrimitive -> listOfNotNull(element.contentOrNull)
                        else -> emptyList()
                    }
                } ?: emptyList()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}
    }
    return fileList
}
