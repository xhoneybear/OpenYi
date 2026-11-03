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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
        entries[ordinal - 1]

    fun next(): Step? =
        entries[ordinal + 1]
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
    val navController = rememberNavController()
    val currentRoute =
        navController
            .currentBackStackEntryAsState()
            .value?.destination?.route

    Scaffold(
        modifier = Modifier.windowInsetsPadding(
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
                    if (currentRoute != Step.WELCOME.name) {
                        TextButton(
                            onClick = {
                                val prevStep = currentRoute?.let { Step.valueOf(it) }?.prev()
                                if (prevStep != null) {
                                    navController.navigate(prevStep.name)
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
                        currentStep = currentRoute?.let { route ->
                            Step.entries.firstOrNull { it.name == route }
                        }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Button(
                        onClick = {
                            if (currentRoute == Step.FINISH.name) {
                                onFinish()
                            } else {
                                val nextStep = currentRoute?.let { Step.valueOf(it) }?.next()
                                if (nextStep != null) {
                                    navController.navigate(nextStep.name)
                                }
                            }
                        }
                    ) {
                        Text(if (currentRoute == Step.FINISH.name) "Finish" else "Next")
                    }
                }
            }
        }
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = Step.WELCOME.name,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
        ) {
            composable(Step.WELCOME.name) {
                Panel(
                    icon = MaterialSymbols.RoundedFilled.Camera,
                    title = "Welcome to OpenYi!",
                    content = {
                        IntroText("A modern application for controlling your Xiaoyi Action Camera.")
                        IntroText("This wizard will help you set up your camera and get started.")
                    }
                )
            }
            composable(Step.CONNECT.name) {
                Panel(
                    icon = MaterialSymbols.RoundedFilled.Wifi,
                    title = "Connect your camera",
                    content = {
                        val coroutineScope = rememberCoroutineScope()
                        val storage = remember { AppStorage() }
                        val cameraClient = remember { CameraClient.shared }
                        var discoveryState by remember { mutableStateOf<DiscoveryState>(DiscoveryState.Idle) }
                        var isScanning by remember { mutableStateOf(false) }

                        val statusTitle = when (val state = discoveryState) {
                            is DiscoveryState.Idle -> "Connection has not been checked."
                            is DiscoveryState.Scanning -> "Scanning network..."
                            is DiscoveryState.Finished -> if (state.hasFoundCamera) "Camera discovered!" else "Camera not found."
                        }

                        val statusDescription = when (val state = discoveryState) {
                            is DiscoveryState.Idle -> "Press the button below to automatically scan the network for your camera."
                            is DiscoveryState.Scanning -> "Scanning ${state.currentIp.ifBlank { "local network" }} (${state.checkedCount}/${state.totalCount})..."
                            is DiscoveryState.Finished -> if (state.hasFoundCamera) {
                                "The camera is reachable at ${state.primaryIp}."
                            } else {
                                "Make sure your camera is powered on and in the same network as your device."
                            }
                        }

                        val statusColor = when (val state = discoveryState) {
                            is DiscoveryState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
                            is DiscoveryState.Scanning -> MaterialTheme.colorScheme.primary
                            is DiscoveryState.Finished -> if (state.hasFoundCamera) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        }

                        val statusBackgroundColor = when (val state = discoveryState) {
                            is DiscoveryState.Idle -> MaterialTheme.colorScheme.surfaceVariant
                            is DiscoveryState.Scanning -> MaterialTheme.colorScheme.surfaceVariant
                            is DiscoveryState.Finished -> if (state.hasFoundCamera) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
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
                            if (discoveryState is DiscoveryState.Scanning) {
                                val scan = discoveryState as DiscoveryState.Scanning
                                LinearProgressIndicator(
                                    progress = { scan.progress },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                )
                            }
                            Button(
                                enabled = !isScanning,
                                onClick = {
                                    coroutineScope.launch {
                                        scanNetwork(
                                            cameraClient = cameraClient,
                                            storage = storage,
                                            onScanState = { state -> discoveryState = state },
                                            onScanning = { scanning -> isScanning = scanning }
                                        )
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
                                    when (discoveryState) {
                                        is DiscoveryState.Idle -> "Scan for camera"
                                        is DiscoveryState.Scanning -> "Scanning..."
                                        is DiscoveryState.Finished -> "Scan again"
                                    }
                                )
                            }
                        }
                    }
                )
            }
            composable(Step.EXTRA.name) {
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
            composable(Step.FINISH.name) {
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
