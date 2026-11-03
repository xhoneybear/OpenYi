package dev.lisek.openyi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Add_a_photo
import com.composables.icons.materialsymbols.roundedfilled.Camera
import com.composables.icons.materialsymbols.roundedfilled.Rocket_launch
import com.composables.icons.materialsymbols.roundedfilled.Sensors
import com.composables.icons.materialsymbols.roundedfilled.Wifi

enum class Step {
    WELCOME,
    CONNECT,
    EXTRA,
    FINISH;

    fun prev(): Step? =
        entries.getOrNull(ordinal - 1)

    fun next(): Step? =
        entries.getOrNull(ordinal + 1)
}

@Composable
fun IntroText(text: String) {
    Text(text,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}

@Composable
fun Panel(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        content()
    }
}

@Composable
fun PageIndicator(currentStep: Step?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Step.entries
            .forEach { step ->
                Box(
                    modifier = Modifier
                        .size(if (step == currentStep) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (step == currentStep) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
    }
}

@Composable
fun Intro(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { Step.entries.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(
                WindowInsets
                    .safeContent
                    .only(WindowInsetsSides.Vertical)
        ),
        topBar = {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                TextButton(
                    onClick = onFinish
                ) {
                    Text("Skip setup")
                }
            }
        },
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(Modifier.weight(1f)) {
                    if (pagerState.currentPage > 0) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        ) {
                            Text("Back")
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    PageIndicator(
                        currentStep = Step.entries.getOrNull(pagerState.currentPage)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Button(
                        onClick = {
                            if (pagerState.currentPage == Step.entries.lastIndex) {
                                onFinish()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }
                    ) {
                        Text(if (pagerState.currentPage == Step.entries.lastIndex) "Finish" else "Next")
                    }
                }
            }
        }
    ) { contentPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            pageSpacing = 16.dp
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                when (Step.entries[page]) {
                    Step.WELCOME -> {
                        Panel(
                            icon = MaterialSymbols.RoundedFilled.Camera,
                            title = "Welcome to OpenYi!",
                            content = {
                                IntroText("A modern application for controlling your Xiaoyi Action Camera.")
                                IntroText("This wizard will help you set up your camera and get started.")
                            }
                        )
                    }
                    Step.CONNECT -> {
                        Panel(
                            icon = MaterialSymbols.RoundedFilled.Wifi,
                            title = "Connect your camera",
                            content = {
                                val coroutineScope = rememberCoroutineScope()
                                val cameraClient = remember { CameraClient.shared }
                                val connectionState by cameraClient.connectionState.collectAsState()
                                val isConnected by cameraClient.isConnected.collectAsState()
                                val isScanning by cameraClient.isScanning.collectAsState()
                                val discoveryState by cameraClient.discoveryState.collectAsState()
                                val batteryInfo by cameraClient.batteryInfo.collectAsState()

                                val statusTitle = when {
                                    isConnected -> "Camera connected!"
                                    isScanning -> "Scanning network..."
                                    connectionState is CameraConnectionState.Connecting -> "Connecting..."
                                    connectionState is CameraConnectionState.Error -> "Connection failed"
                                    else -> "Connection has not been checked."
                                }

                                val statusDescription = when {
                                    isConnected -> "Connected at ${cameraClient.host}. Battery: ${batteryInfo.displayText}."
                                    isScanning -> {
                                        val s = discoveryState as? DiscoveryState.Scanning
                                        if (s != null) {
                                            "Scanning ${s.currentIp.ifBlank { "local network" }} (${s.checkedCount}/${s.totalCount})..."
                                        } else {
                                            "Scanning local network..."
                                        }
                                    }
                                    connectionState is CameraConnectionState.Connecting -> "Establishing session with camera at ${cameraClient.host}..."
                                    connectionState is CameraConnectionState.Error -> (connectionState as CameraConnectionState.Error).message
                                    else -> "Press the button below to connect and verify your camera."
                                }

                                val statusColor = when {
                                    isConnected -> MaterialTheme.colorScheme.primary
                                    isScanning || connectionState is CameraConnectionState.Connecting -> MaterialTheme.colorScheme.primary
                                    connectionState is CameraConnectionState.Error -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                val statusBackgroundColor = when {
                                    isConnected -> MaterialTheme.colorScheme.primaryContainer
                                    isScanning || connectionState is CameraConnectionState.Connecting -> MaterialTheme.colorScheme.surfaceVariant
                                    connectionState is CameraConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }

                                IntroText("Start your Yi Cam, press the Wi-Fi button, and connect to your camera's network.")
                                IntroText("The name of your camera's network should start with 'YDXJ'.")

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .background(
                                            color = statusBackgroundColor,
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = statusColor,
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = statusTitle,
                                        color = statusColor,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = statusDescription,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    if (isScanning && discoveryState is DiscoveryState.Scanning) {
                                        val scan = discoveryState as DiscoveryState.Scanning
                                        LinearProgressIndicator(
                                            progress = { scan.progress },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                        )
                                    }
                                    Button(
                                        enabled = !isScanning && connectionState !is CameraConnectionState.Connecting,
                                        onClick = {
                                            coroutineScope.launch {
                                                cameraClient.connectAndInitialize(forceReconnect = true)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = MaterialSymbols.RoundedFilled.Sensors,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .padding(end = 8.dp)
                                        )
                                        Text(
                                            when {
                                                isScanning -> "Scanning..."
                                                connectionState is CameraConnectionState.Connecting -> "Connecting..."
                                                isConnected -> "Test again"
                                                else -> "Connect camera"
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                    Step.EXTRA -> {
                        Panel(
                            icon = MaterialSymbols.RoundedFilled.Add_a_photo,
                            title = "Your camera can do more",
                            content = {
                                var telnetStatus: Boolean? = null

                                var statusTitle =
                                    if (telnetStatus == null) {
                                        "Test telnet connection"
                                    } else if (telnetStatus) {
                                        "Telnet accessible!"
                                    } else {
                                        "Telnet is not available."
                                    }
                                var statusDescription =
                                    if (telnetStatus == null) {
                                        "Press the button below to check the connection."
                                    } else if (telnetStatus) {
                                        "Telnet is open at port 23. You can access advanced settings."
                                    } else {
                                        "Make sure that you have completed all the steps."
                                    }
                                var statusColor =
                                    if (telnetStatus == null) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else if (telnetStatus) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                var statusBackgroundColor =
                                    if (telnetStatus == null) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else if (telnetStatus) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    }

                                IntroText("Yi Action Camera has a lot of advanced features available, such as configurable exposure, denoising, button bindings, and more.")
                                IntroText("This app supports these features, But you need to create an empty file on the camera's SD card.")
                                IntroText("The file must be named 'enable_info_display.script' and placed in the root of the SD card. You need to restart your camera after creating the file.")
                                IntroText("The app will scan your camera's telnet port to enable advanced settings section.")

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .background(
                                            color = statusBackgroundColor,
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = statusColor,
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = statusTitle,
                                        color = statusColor,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = statusDescription,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = {
                                            // Handle connection test
                                        }
                                    ) {
                                        Icon(
                                            imageVector = MaterialSymbols.RoundedFilled.Sensors,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .padding(end = 8.dp)
                                        )
                                        Text(
                                            if (telnetStatus == null) {
                                                "Test connection"
                                            } else {
                                                "Check again"
                                            }
                                        )
                                    }
                                }
                                
                            }
                        )
                    }
                    Step.FINISH -> {
                        Panel(
                            icon = MaterialSymbols.RoundedFilled.Rocket_launch,
                            title = "You're all set!",
                            content = {
                                IntroText("The camera is set up and ready to use.")
                                IntroText("You can configure it further in the settings menu.")
                            }
                        )
                    }
                }
            }
        }
    }
}
