package com.localai.hub.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.localai.hub.core.inference.PerformanceProfile
import com.localai.hub.core.modelregistry.displayName
import com.localai.hub.core.modelregistry.hasRealLocalFile
import com.localai.hub.core.modelregistry.isBundledPreview
import com.localai.hub.ui.theme.Clay
import com.localai.hub.ui.theme.DeepInk
import com.localai.hub.ui.theme.Mist
import com.localai.hub.ui.theme.Sand
import com.localai.hub.ui.theme.Sea
import kotlin.math.roundToInt

@Composable
fun ChatRoute(
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    ChatScreen(
        uiState = uiState,
        onDraftChanged = viewModel::updateDraft,
        onSend = viewModel::sendMessage,
        onTemperatureChanged = viewModel::updateTemperature,
        onMaxTokensChanged = { value -> viewModel.updateMaxTokens(value.roundToInt()) },
        onContextChanged = { value -> viewModel.updateContextSize(value.roundToInt()) },
        onProfileChanged = viewModel::updateProfile,
    )
}

@Composable
private fun ChatScreen(
    uiState: ChatUiState,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onTemperatureChanged: (Float) -> Unit,
    onMaxTokensChanged: (Float) -> Unit,
    onContextChanged: (Float) -> Unit,
    onProfileChanged: (PerformanceProfile) -> Unit,
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val latestAssistantReply = uiState.messages.lastOrNull {
        it.role == MessageRole.ASSISTANT && it.id != 0L
    }

    LaunchedEffect(uiState.messages.lastOrNull()?.id, uiState.isGenerating) {
        if (uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        HeroStatusCard(uiState = uiState)
        Spacer(modifier = Modifier.height(12.dp))
        RuntimeControls(
            uiState = uiState,
            onTemperatureChanged = onTemperatureChanged,
            onMaxTokensChanged = onMaxTokensChanged,
            onContextChanged = onContextChanged,
            onProfileChanged = onProfileChanged,
        )
        if (uiState.isGenerating || latestAssistantReply != null) {
            Spacer(modifier = Modifier.height(12.dp))
            LatestReplyCard(
                reply = latestAssistantReply?.text,
                isGenerating = uiState.isGenerating,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.messages, key = ChatMessage::id) { message ->
                ChatBubble(message = message)
            }
            if (uiState.isGenerating) {
                item {
                    PendingBubble()
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Composer(
            draft = uiState.draft,
            isGenerating = uiState.isGenerating,
            onDraftChanged = onDraftChanged,
            onSend = {
                focusManager.clearFocus(force = true)
                onSend()
            },
        )
    }
}

@Composable
private fun LatestReplyCard(
    reply: String?,
    isGenerating: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Latest Reply",
                style = MaterialTheme.typography.titleLarge,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isGenerating) {
                    "جاري توليد الرد المحلي..."
                } else {
                    reply ?: "لا يوجد رد بعد."
                },
                color = DeepInk,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun HeroStatusCard(uiState: ChatUiState) {
    val activeModel = uiState.activeModel
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(DeepInk, Sea, Clay),
                    ),
                )
                .padding(18.dp),
        ) {
            Text(
                text = "Local Assistant",
                color = Sand,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = activeModel?.let { "النموذج النشط: ${it.displayName}" } ?: "لا يوجد نموذج محادثة نشط",
                color = Sand,
                style = MaterialTheme.typography.bodyLarge,
            )
            activeModel?.let { model ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        model.hasRealLocalFile -> "المصدر: ملف GGUF محلي عبر llama.cpp"
                        model.isBundledPreview -> "المصدر: معاينة مدمجة. استورد GGUF من Models لتفعيل التشغيل الحقيقي."
                        else -> "المصدر: غير متوفر بعد."
                    },
                    color = Mist,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildString {
                    uiState.telemetry?.let { telemetry ->
                        append("RAM ${telemetry.freeRamMb}/${telemetry.totalRamMb}MB")
                        append(" | حرارة ${telemetry.thermalStatus}")
                        append(" | توفير البطارية ${if (telemetry.batterySaverEnabled) "مفعّل" else "متوقف"}")
                    } ?: append("جاري قراءة حالة الجهاز...")
                },
                color = Mist,
                style = MaterialTheme.typography.labelLarge,
            )
            uiState.lastLatencyMs?.let { latency ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "آخر استجابة: ${latency}ms",
                    color = Mist,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            uiState.notice?.let { notice ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = notice,
                    color = Sand,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun RuntimeControls(
    uiState: ChatUiState,
    onTemperatureChanged: (Float) -> Unit,
    onMaxTokensChanged: (Float) -> Unit,
    onContextChanged: (Float) -> Unit,
    onProfileChanged: (PerformanceProfile) -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Runtime Controls",
                style = MaterialTheme.typography.titleLarge,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceProfile.entries.forEach { profile ->
                    FilterChip(
                        selected = uiState.profile == profile,
                        onClick = { onProfileChanged(profile) },
                        label = { Text(profile.displayName) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            SliderRow(
                title = "Temperature",
                valueLabel = uiState.temperature.toString(),
                value = uiState.temperature,
                range = 0.1f..1.2f,
                onValueChange = onTemperatureChanged,
            )
            SliderRow(
                title = "Max tokens",
                valueLabel = uiState.maxTokens.toString(),
                value = uiState.maxTokens.toFloat(),
                range = 128f..1024f,
                onValueChange = onMaxTokensChanged,
            )
            SliderRow(
                title = "Context size",
                valueLabel = uiState.contextSize.toString(),
                value = uiState.contextSize.toFloat(),
                range = 1024f..4096f,
                onValueChange = onContextChanged,
            )
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Text(text = valueLabel, color = Sea)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (isUser) 22.dp else 8.dp,
                        bottomEnd = if (isUser) 8.dp else 22.dp,
                    ),
                )
                .background(if (isUser) DeepInk else Mist)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(0.82f),
        ) {
            Text(
                text = if (isUser) "You" else "Assistant",
                color = if (isUser) Sand else Sea,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message.text,
                color = if (isUser) Sand else DeepInk,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    isGenerating: Boolean,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            modifier = Modifier.weight(1f),
            label = { Text("اكتب رسالة أو نصًا للتلخيص") },
            maxLines = 4,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Button(
            enabled = !isGenerating,
            onClick = onSend,
            modifier = Modifier.height(56.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = "Send",
            )
        }
    }
}

@Composable
private fun PendingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = 8.dp,
                        bottomEnd = 22.dp,
                    ),
                )
                .background(Mist)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(0.72f),
        ) {
            Text(
                text = "Assistant",
                color = Sea,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "جاري توليد الرد المحلي...",
                color = DeepInk,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
