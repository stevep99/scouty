package io.github.stevep99.scouty.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stevep99.scouty.core.ScoutyState

@Composable
fun SettingsScreen(
    ttsReady: Boolean,
    llmReady: Boolean,
    sttReady: Boolean,
    currentState: ScoutyState,
    llmBaseUrl: String,
    llmApiKey: String,
    llmModelName: String,
    onLlmBaseUrlChange: (String) -> Unit,
    onLlmApiKeyChange: (String) -> Unit,
    onLlmModelNameChange: (String) -> Unit,
    onConnectLlm: () -> Unit,
    onSpeak: (String) -> Unit,
    onGenerate: (String) -> Unit,
    onStartListening: () -> Unit,
    onOpenMenu: () -> Unit,
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var promptText by remember { mutableStateOf("Hello! Who are you?") }
    var endpointExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header + status
            Column(modifier = Modifier.weight(1f)) {
                Text("Scouty Settings", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusChip("TTS", ttsReady)
                    StatusChip("LLM", llmReady)
                    StatusChip("STT", sttReady)
                    Text("State: $currentState", fontSize = 13.sp)
                    if (currentState == ScoutyState.Listening) {
                        Text("REC", color = Color(0xFFF44336), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (endpointExpanded) {
                    Button(onClick = { endpointExpanded = false }) { Text("LLM Settings", fontSize = 12.sp) }
                } else {
                    OutlinedButton(onClick = { endpointExpanded = true }) { Text("LLM Settings", fontSize = 12.sp) }
                }
                OutlinedButton(onClick = onOpenMenu) { Text("Menu", fontSize = 12.sp) }
            }
        }

        if (endpointExpanded) {
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = llmBaseUrl,
                    onValueChange = onLlmBaseUrlChange,
                    label = { Text("Base URL") },
                    placeholder = { Text("http://192.168.1.1:8080/v1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = llmApiKey,
                        onValueChange = onLlmApiKeyChange,
                        label = { Text("API key") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = llmModelName,
                        onValueChange = onLlmModelNameChange,
                        label = { Text("Model") },
                        placeholder = { Text("auto") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = onConnectLlm,
                        enabled = llmBaseUrl.isNotBlank(),
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Connect", fontSize = 12.sp) }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(4.dp))

        // Voice pipeline — state-driven
        when (currentState) {
            ScoutyState.Thinking -> {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    color = Color(0xFFFFCB6B),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            ScoutyState.Listening -> {
                Button(
                    onClick = { /* speech recognizer handles this */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Listening... speak now")
                }
            }
            ScoutyState.Speaking -> {
                Button(
                    onClick = { /* TTS handles this */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF89DDFF))
                ) {
                    Text("Speaking...")
                }
            }
            else -> {
                // Idle / Moving
                Button(
                    onClick = onStartListening,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sttReady && ttsReady,
                ) {
                    Text("Tap to talk")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(4.dp))

        // LLM Prompt — input + button in a Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                label = { Text("Prompt") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (promptText.isNotBlank()) onGenerate(promptText)
                },
                modifier = Modifier.padding(top = 8.dp),
                enabled = llmReady && promptText.isNotBlank()
            ) { Text("Go") }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // TTS — input + button in a Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("TTS") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (inputText.isNotBlank()) onSpeak(inputText)
                },
                enabled = ttsReady && inputText.isNotBlank()
            ) { Text("Speak") }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(4.dp))

        // Event log — fills remaining space, independently scrollable
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1E1E1E))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(logs) { entry ->
                Text(
                    text = entry,
                    color = when {
                        entry.startsWith("[SPEECH]") -> Color(0xFF89DDFF)
                        entry.startsWith("[INPUT]") -> Color(0xFF82B1FF)
                        entry.startsWith("[TTS]") -> Color(0xFFC3E88D)
                        entry.startsWith("[LLM]") -> Color(0xFFFFCB6B)
                        entry.startsWith("[MODEL]") -> Color(0xFFC792EA)
                        entry.startsWith("[PARSE]") -> Color(0xFFC792EA)
                        entry.startsWith("[ERROR]") -> Color(0xFFFF5370)
                        entry.startsWith("[STT]") -> Color(0xFF80CBC4)
                        else -> Color(0xFFB0BEC5)
                    },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, ready: Boolean) {
    Text(
        text = "$label:${if (ready) "Ok" else "--"}",
        color = if (ready) Color(0xFF4CAF50) else Color(0xFFF44336),
        fontSize = 13.sp
    )
}
