package com.localai.hub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.localai.hub.ui.LocalAiHubRoot
import com.localai.hub.ui.theme.LocalAIHubTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocalAIHubTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LocalAiHubRoot()
                }
            }
        }
    }
}

