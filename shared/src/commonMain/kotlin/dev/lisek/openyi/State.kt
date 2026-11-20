package dev.lisek.openyi

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Android_cell_4_bar_off
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
import com.composables.icons.materialsymbols.roundedfilled.Error
import com.composables.icons.materialsymbols.roundedfilled.Signal_disconnected
import com.composables.icons.materialsymbols.roundedfilled.Timer_10_alt_1
import com.composables.icons.materialsymbols.roundedfilled.Timer_3_alt_1
import com.composables.icons.materialsymbols.roundedfilled.Timer_5
import com.composables.icons.materialsymbols.roundedfilled.Timer_off


sealed interface CameraConnectionState {
    data object Disconnected : CameraConnectionState
    data class Connecting(val host: String) : CameraConnectionState
    data class Connected(val host: String, val token: Int?) : CameraConnectionState
    data class Error(val message: String) : CameraConnectionState

    val isConnected: Boolean
        get() = this is Connected

    val isConnecting: Boolean
        get() = this is Connecting

    val errorMessage: String?
        get() = (this as? Error)?.message
}


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

    companion object {
        fun fromLevel(level: Int): BatteryLevel {
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
}

data class BatteryInfo(
    val level: Int = -1,
    val isCharging: Boolean = false
) {
    val displayText: String
        get() = if (level >= 0) "$level%" else "N/A"

    val batteryLevel: BatteryLevel
        get() = BatteryLevel.fromLevel(level)

    val icon: ImageVector
        get() = if (isCharging) batteryLevel.chargingIcon else batteryLevel.icon
}


enum class TimerDuration(val duration: Int, val icon: ImageVector) {
    OFF(0, MaterialSymbols.RoundedFilled.Timer_off),
    S_3(3000, MaterialSymbols.RoundedFilled.Timer_3_alt_1),
    S_5(5000, MaterialSymbols.RoundedFilled.Timer_5),
    S_10(10000, MaterialSymbols.RoundedFilled.Timer_10_alt_1);

    fun next(): TimerDuration = when (this) {
        OFF -> S_3
        S_3 -> S_5
        S_5 -> S_10
        S_10 -> OFF
    }
}


enum class RtspPlayerState {
    Disconnected,
    Idle,
    Buffering,
    Playing,
    Error;

    fun errorMessage(): String? = when (this) {
        Error -> "Unable to decode RTSP stream!"
        Idle -> "Connection lost. Dismiss to retry."
        else -> null
    }

    @Composable
    fun icon(modifier: Modifier = Modifier, tint: Color = Color.White) = when (this) {
        Disconnected -> Icon(MaterialSymbols.RoundedFilled.Signal_disconnected, null, modifier, tint)
        Idle -> Icon(MaterialSymbols.RoundedFilled.Android_cell_4_bar_off, null, modifier, tint)
        Buffering -> DotsPulsing(modifier, tint)
        Playing -> RecordingIndicator(modifier, tint)
        Error -> Icon(MaterialSymbols.RoundedFilled.Error, null, modifier, tint)
    }
}


enum class CameraLogLevel {
    TX,
    RX,
    INFO,
    WARN,
    ERROR
}

data class CameraLogEntry(
    val level: CameraLogLevel,
    val message: String
)
