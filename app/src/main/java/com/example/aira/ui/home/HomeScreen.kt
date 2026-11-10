package com.example.aira.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aira.data.model.ListeningState
import com.example.aira.ui.AiraViewModel
import com.example.aira.ui.components.AiraOrb
import com.example.aira.ui.theme.AiraCyan
import com.example.aira.ui.theme.AiraPink
import com.example.aira.ui.theme.AiraPurple
import com.example.aira.ui.theme.BackgroundDark
import com.example.aira.ui.theme.CardBorder
import com.example.aira.ui.theme.ErrorRed
import com.example.aira.ui.theme.SuccessGreen
import com.example.aira.ui.theme.SurfaceDark
import com.example.aira.ui.theme.SurfaceVariantDark
import com.example.aira.ui.theme.TextPrimary
import com.example.aira.ui.theme.TextSecondary
import com.example.aira.ui.theme.TextTertiary

@Composable
fun HomeScreen(
    viewModel: AiraViewModel,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val status by viewModel.assistantStatus.collectAsState()
    val wakePhrase by viewModel.wakePhrase.collectAsState()
    val selectedModelName by viewModel.selectedModelName.collectAsState()

    val isModelInstalled = viewModel.modelManager.isModelInstalled(selectedModelName)
    val hasMicPermission = viewModel.isRecordAudioGranted()
    var showWakePhraseDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "AIRA",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AiraCyan,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Offline Voice Assistant",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Inactivity countdown badge when listening
            if (status.isListening) {
                val minutes = status.timeoutRemainingSeconds / 60
                val seconds = status.timeoutRemainingSeconds % 60
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceVariantDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Timeout",
                            tint = AiraCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center Assistant Orb
        AiraOrb(
            state = status.state,
            amplitude = status.audioAmplitude,
            modifier = Modifier.size(190.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status Header Text
        val stateTitle = when (status.state) {
            ListeningState.OFF -> "Assistant is off"
            ListeningState.STARTING -> "Initializing Aira..."
            ListeningState.LISTENING_FOR_WAKE_WORD -> "Listening for \"$wakePhrase\""
            ListeningState.LISTENING_FOR_COMMAND -> "Listening for command..."
            ListeningState.PROCESSING_COMMAND -> "Processing command..."
            ListeningState.SPEAKING_RESPONSE -> "Responding..."
            ListeningState.ERROR -> status.errorMessage ?: "Error"
        }

        val stateColor = when (status.state) {
            ListeningState.OFF -> TextSecondary
            ListeningState.STARTING -> AiraPurple
            ListeningState.LISTENING_FOR_WAKE_WORD -> AiraCyan
            ListeningState.LISTENING_FOR_COMMAND -> AiraCyan
            ListeningState.PROCESSING_COMMAND -> AiraPurple
            ListeningState.SPEAKING_RESPONSE -> AiraPink
            ListeningState.ERROR -> ErrorRed
        }

        Text(
            text = stateTitle,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = stateColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Master Listening Toggle Button
        Button(
            onClick = {
                if (!hasMicPermission) {
                    onRequestPermissions()
                } else {
                    viewModel.toggleListening(context)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (status.isListening) AiraCyan else SurfaceVariantDark,
                contentColor = if (status.isListening) BackgroundDark else TextPrimary
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(56.dp)
                .border(
                    width = 1.dp,
                    color = if (status.isListening) AiraCyan else CardBorder,
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (status.isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Toggle",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (status.isListening) "STOP LISTENING" else "START LISTENING",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Diagnostic / Status Badges Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusBadge(
                label = "Wake Word (Tap)",
                value = wakePhrase,
                isOk = true,
                onClick = { showWakePhraseDialog = true }
            )
            StatusBadge(
                label = "Speech Model",
                value = if (isModelInstalled) "Ready" else "Missing",
                isOk = isModelInstalled
            )
            StatusBadge(
                label = "Mic Permission",
                value = if (hasMicPermission) "Granted" else "Needed",
                isOk = hasMicPermission
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Real-time Recognized Text Card
        AnimatedVisibility(
            visible = status.lastRecognizedText.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = AiraCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recognized Command",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${status.lastRecognizedText}\"",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }

        // Assistant Response Card
        AnimatedVisibility(
            visible = status.lastAssistantResponse.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, AiraPurple.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AiraPink)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aira Response",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AiraPink
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status.lastAssistantResponse,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    if (status.lastCommandStatus.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = status.lastCommandStatus,
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        // Helpful Command Suggestions
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Supported Voice Commands",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val exampleCommands = listOf(
            "\"Open WhatsApp\"",
            "\"Open YouTube\"",
            "\"Turn on flashlight\"",
            "\"What is the battery level?\"",
            "\"What time is it?\"",
            "\"Open Wi-Fi settings\"",
            "\"Volume up\"",
            "\"Stop listening\""
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                onClick = {},
                label = { Text("Open YouTube", fontSize = 11.sp, color = TextPrimary) },
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SurfaceDark),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = CardBorder
                )
            )
            SuggestionChip(
                onClick = {},
                label = { Text("Flashlight ON", fontSize = 11.sp, color = TextPrimary) },
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SurfaceDark),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = CardBorder
                )
            )
            SuggestionChip(
                onClick = {},
                label = { Text("Battery Level", fontSize = 11.sp, color = TextPrimary) },
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SurfaceDark),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = CardBorder
                )
            )
        }
    }

    if (showWakePhraseDialog) {
        var inputPhrase by remember(wakePhrase) { mutableStateOf(wakePhrase) }
        val presets = listOf("Hey Assistant", "Hey Aira", "Aira", "Computer")

        AlertDialog(
            onDismissRequest = { showWakePhraseDialog = false },
            title = {
                Text(
                    text = "Configure Wake Phrase",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Customize the phrase Aira listens for in the background to activate.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputPhrase,
                        onValueChange = { inputPhrase = it },
                        label = { Text("Wake Phrase") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Presets:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            OutlinedButton(
                                onClick = { inputPhrase = preset },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
                            ) {
                                Text(preset, fontSize = 9.sp, maxLines = 1, color = AiraCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputPhrase.isNotBlank()) {
                            viewModel.setWakePhrase(inputPhrase)
                        }
                        showWakePhraseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AiraCyan)
                ) {
                    Text("Save", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWakePhraseDialog = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun StatusBadge(
    label: String,
    value: String,
    isOk: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 10.sp, color = TextTertiary)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isOk) SuccessGreen else ErrorRed)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }
    }
}
