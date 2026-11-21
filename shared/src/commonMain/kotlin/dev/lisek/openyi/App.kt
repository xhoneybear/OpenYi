package dev.lisek.openyi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.painterResource

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

import openyi.shared.generated.resources.Res
import openyi.shared.generated.resources.compose_multiplatform

@Composable
@Preview
fun App() {
    val storage = AppStorage()

    val showIntro = remember { mutableStateOf(storage.showIntro) }
    val navController = rememberNavController()

    MaterialTheme {
        NavHost(
            navController = navController,
            startDestination = if (showIntro.value)
                "intro"
            else
                "control",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("intro") {
                Intro(onFinish = {
                    showIntro.value = false
                    storage.showIntro = false
                    navController.navigate("control") {
                        popUpTo("intro") { inclusive = true }
                    }
                })
            }
            composable("control") {
                Control(onSettingsClick = {
                    navController.navigate("log")
                })
            }
            composable("log") {
                LogView(onBackPressed = {
                    navController.popBackStack()
                })
            }
            composable("settings") {
                Settings()
            }
        }
    }
}
