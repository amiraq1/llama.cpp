package com.localai.hub.feature.speech

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.localai.hub.ui.theme.DeepInk

@Composable
fun SpeechRoute(
    viewModel: SpeechViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    SpeechScreen(
        uiState = uiState,
        onClipChanged = viewModel::updateClipLabel,
        onLanguageHintChanged = viewModel::updateLanguageHint,
        onRun = viewModel::runDemo,
    )
}

@Composable
private fun SpeechScreen(
    uiState: SpeechUiState,
    onClipChanged: (String) -> Unit,
    onLanguageHintChanged: (String) -> Unit,
    onRun: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Speech",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepInk,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.activeModel?.let { "النموذج النشط: ${it.name}" }
                        ?: "لا يوجد نموذج STT محلي نشط.",
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                OutlinedTextField(
                    value = uiState.clipLabel,
                    onValueChange = onClipChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم المقطع التجريبي") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.languageHint,
                    onValueChange = onLanguageHintChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("تلميح اللغة") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRun, enabled = !uiState.isRunning) {
                    Text(if (uiState.isRunning) "جاري التشغيل" else "تشغيل نسخ تجريبي")
                }
            }
        }
        uiState.result?.let { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "Output", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = result)
                }
            }
        }
    }
}
