package com.localai.hub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import com.localai.hub.feature.chat.ChatRoute
import com.localai.hub.feature.models.ModelsRoute
import com.localai.hub.feature.speech.SpeechRoute
import com.localai.hub.feature.vision.VisionRoute
import com.localai.hub.ui.theme.Mist
import com.localai.hub.ui.theme.Sand

private enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
) {
    CHAT("Chat", Icons.Outlined.ChatBubbleOutline),
    IMAGES("Images", Icons.Outlined.Image),
    VOICE("Voice", Icons.Outlined.MicNone),
    MODELS("Models", Icons.Outlined.Memory),
}

@Composable
fun LocalAiHubRoot() {
    var selectedTab by rememberSaveable { mutableStateOf(TopLevelDestination.CHAT.name) }
    val currentDestination = TopLevelDestination.valueOf(selectedTab)

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination == destination,
                        onClick = { selectedTab = destination.name },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Sand, Mist),
                    ),
                )
                .padding(innerPadding),
        ) {
            when (currentDestination) {
                TopLevelDestination.CHAT -> ChatRoute()
                TopLevelDestination.IMAGES -> VisionRoute()
                TopLevelDestination.VOICE -> SpeechRoute()
                TopLevelDestination.MODELS -> ModelsRoute()
            }
        }
    }
}
