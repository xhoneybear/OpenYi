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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Battery_0_bar
import com.composables.icons.materialsymbols.roundedfilled.Battery_1_bar
import com.composables.icons.materialsymbols.roundedfilled.Battery_2_bar
import com.composables.icons.materialsymbols.roundedfilled.Battery_3_bar
import com.composables.icons.materialsymbols.roundedfilled.Battery_4_bar
import com.composables.icons.materialsymbols.roundedfilled.Battery_5_bar
import com.composables.icons.materialsymbols.roundedfilled.Battery_6_bar
import com.composables.icons.materialsymbols.roundedfilled.Battery_charging_20
import com.composables.icons.materialsymbols.roundedfilled.Battery_charging_30
import com.composables.icons.materialsymbols.roundedfilled.Battery_charging_50
import com.composables.icons.materialsymbols.roundedfilled.Battery_charging_60
import com.composables.icons.materialsymbols.roundedfilled.Battery_charging_80
import com.composables.icons.materialsymbols.roundedfilled.Battery_charging_90
import com.composables.icons.materialsymbols.roundedfilled.Battery_charging_full
import com.composables.icons.materialsymbols.roundedfilled.Battery_full
import com.composables.icons.materialsymbols.roundedfilled.Battery_unknown
import com.composables.icons.materialsymbols.roundedfilled.Fullscreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class BatteryLevel(val icon: ImageVector, val chargingIcon: ImageVector) {
    B_0(MaterialSymbols.RoundedFilled.Battery_0_bar, MaterialSymbols.RoundedFilled.Battery_charging_full),
    B_20(MaterialSymbols.RoundedFilled.Battery_1_bar, MaterialSymbols.RoundedFilled.Battery_charging_20),
    B_30(MaterialSymbols.RoundedFilled.Battery_2_bar, MaterialSymbols.RoundedFilled.Battery_charging_30),
    B_50(MaterialSymbols.RoundedFilled.Battery_3_bar, MaterialSymbols.RoundedFilled.Battery_charging_50),
    B_60(MaterialSymbols.RoundedFilled.Battery_4_bar, MaterialSymbols.RoundedFilled.Battery_charging_60),
    B_80(MaterialSymbols.RoundedFilled.Battery_5_bar, MaterialSymbols.RoundedFilled.Battery_charging_80),
    B_90(MaterialSymbols.RoundedFilled.Battery_6_bar, MaterialSymbols.RoundedFilled.Battery_charging_90),
    B_100(MaterialSymbols.RoundedFilled.Battery_full, MaterialSymbols.RoundedFilled.Battery_full),
    UNKNOWN(MaterialSymbols.RoundedFilled.Battery_unknown, MaterialSymbols.RoundedFilled.Battery_unknown);
    
    fun classify(level: Int): BatteryLevel {
        return when {
            level == 100 -> B_100
            level > 90 -> B_90
            level > 80 -> B_80
            level > 60 -> B_60
            level > 50 -> B_50
            level > 30 -> B_30
            level > 20 -> B_20
            level >= 0 -> B_0
            else -> UNKNOWN
        }
    }
}

@Composable
fun Overlay(cameraClient: CameraClient, playerState: RtspPlayerState) {
    var battery: BatteryLevel by remember { mutableStateOf(BatteryLevel.UNKNOWN) }
    var batteryLevel: String by remember { mutableStateOf("N/A") }
    var charging: Boolean by remember { mutableStateOf(false) }

    val dispatcher = remember {
        object : CameraSignalDispatcher() {
            override fun onBattery(param: String?) {
                charging = false
                batteryLevel = if (param.isNullOrEmpty()) "N/A" else "$param%"
                battery = battery.classify(param?.toIntOrNull() ?: -1)
            }
            override fun onAdapter(param: String?) {
                charging = true
                batteryLevel = if (param.isNullOrEmpty()) "N/A" else "$param%"
                battery = battery.classify(param?.toIntOrNull() ?: -1)
            }
            override fun onAdapterStatus(param: String?) {
                charging = param == "1"
            }
        }
    }

    LaunchedEffect(cameraClient) {
        cameraClient.signals.collect { signal ->
            dispatcher.onSignal(signal.type, signal.param, signal.rawResponse)
        }
    }

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
                text = batteryLevel,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Icon(
                imageVector = if (charging) battery.chargingIcon else battery.icon,
                contentDescription = "Battery Icon",
                tint = Color.White
            )
        }
        playerState.icon(Modifier
            .padding(4.dp)
            .align(Alignment.BottomStart)
        )
        IconButton(
            onClick = { /* Handle charging icon click */ },
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
