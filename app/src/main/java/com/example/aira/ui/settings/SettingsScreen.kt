package com.example.aira.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aira.speech.SpeechModelManager
import com.example.aira.ui.AiraViewModel
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
import com.example.aira.utils.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AiraViewModel,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val timeoutMinutes by viewModel.timeoutMinutes.collectAsState()
    val voiceResponseEnabled by viewModel.voiceResponseEnabled.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val selectedModelName by viewModel.selectedModelName.collectAsState()
    val wakePhrase by viewModel.wakePhrase.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadingModelName by viewModel.downloadingModelName.collectAsState()
    val downloadError by viewModel.downloadError.collectAsState()
    val modelsUpdated by viewModel.modelsUpdated.collectAsState()

    val hasMic = viewModel.isRecordAudioGranted()
    val hasNotif = viewModel.isNotificationGranted()
    val hasContacts = viewModel.isReadContactsGranted()
    val ignoringBattery = viewModel.isIgnoringBatteryOptimizations()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Screen Title
        Text(
            text = "Settings",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Configure speech recognition, behavior & permissions",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Inactivity Timeout Section
        SectionHeader(title = "Inactivity Timeout", icon = Icons.Default.Timer)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Auto-stop Listening",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Aira stops background listening after this period of inactivity to conserve battery.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                var timeoutExpanded by remember { mutableStateOf(false) }
                val options = listOf(5, 10, 15, 30)

                ExposedDropdownMenuBox(
                    expanded = timeoutExpanded,
                    onExpandedChange = { timeoutExpanded = !timeoutExpanded }
                ) {
                    OutlinedTextField(
                        value = "$timeoutMinutes Minutes (Default: 10m)",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeoutExpanded) },
                        modifier = Modifier
                            .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = timeoutExpanded,
                        onDismissRequest = { timeoutExpanded = false }
                    ) {
                        options.forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text("$minutes Minutes") },
                                onClick = {
                                    viewModel.setTimeoutMinutes(minutes)
                                    timeoutExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Speech Models Section
        SectionHeader(title = "Offline Speech Models", icon = Icons.Default.CloudDownload)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Vosk STT Models",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Stored locally on your device for 100% offline speech-to-text recognition.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                SpeechModelManager.AVAILABLE_MODELS.forEach { modelInfo ->
                    val isInstalled = remember(modelsUpdated, selectedModelName) {
                        viewModel.modelManager.isModelInstalled(modelInfo.name)
                    }
                    val isSelected = selectedModelName == modelInfo.name
                    val isDownloading = (downloadingModelName == modelInfo.name)

                    ModelItemRow(
                        modelInfo = modelInfo,
                        isInstalled = isInstalled,
                        isSelected = isSelected,
                        isDownloading = isDownloading,
                        progress = if (isDownloading) downloadProgress else null,
                        onSelect = { viewModel.setSelectedModel(modelInfo.name) },
                        onDownload = { viewModel.downloadModel(modelInfo) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (downloadError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Error: $downloadError",
                            fontSize = 12.sp,
                            color = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearDownloadError() }) {
                            Text("Dismiss", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Storage Path: ${viewModel.modelManager.getModelsDirectory().absolutePath}",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Voice Output & TTS Section
        SectionHeader(title = "Voice Response (TTS)", icon = Icons.Default.RecordVoiceOver)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Spoken Responses",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Aira speaks command feedback through Android TextToSpeech.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = voiceResponseEnabled,
                        onCheckedChange = { viewModel.setVoiceResponse(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = AiraCyan
                        )
                    )
                }

                if (voiceResponseEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Speech Rate: ${String.format("%.1f", speechRate)}x",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Slider(
                        value = speechRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = AiraCyan,
                            activeTrackColor = AiraCyan,
                            inactiveTrackColor = SurfaceVariantDark
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Permissions & Battery Section
        SectionHeader(title = "Permissions & Background", icon = Icons.Default.Security)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Microphone
                PermissionRow(
                    title = "Microphone Permission",
                    description = "Required to capture audio for wake word and commands.",
                    isGranted = hasMic,
                    onAction = onRequestPermissions
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Notification
                PermissionRow(
                    title = "Notification Permission",
                    description = "Required for Android foreground listening notification.",
                    isGranted = hasNotif,
                    onAction = onRequestPermissions
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Contacts
                PermissionRow(
                    title = "Contacts Permission",
                    description = "Required to search contacts for voice calls (e.g. 'Call Mom').",
                    isGranted = hasContacts,
                    onAction = onRequestPermissions
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Battery Optimization
                PermissionRow(
                    title = "Battery Optimization",
                    description = if (ignoringBattery) "Unrestricted background execution enabled." else "OEM battery restrictions may kill background listening.",
                    isGranted = ignoringBattery,
                    actionText = "Configure",
                    onAction = { PermissionManager.requestIgnoreBatteryOptimizations(context) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Wake-Word Configuration & Engine
        SectionHeader(title = "Wake-Word Configuration", icon = Icons.Default.RecordVoiceOver)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Custom Wake-Up Phrase",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Say this phrase to wake up Aira from background listening. You can customize it to any phrase below.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                var customPhraseInput by remember(wakePhrase) { mutableStateOf(wakePhrase) }
                var showSavedBanner by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = customPhraseInput,
                    onValueChange = {
                        customPhraseInput = it
                        showSavedBanner = false
                    },
                    label = { Text("Wake Phrase") },
                    placeholder = { Text("e.g. Hey Assistant, Hey Aira") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Preset quick suggestions
                Text(
                    text = "Quick Presets:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                val presetPhrases = listOf(
                    "Hey Assistant",
                    "Hey Aira",
                    "Aira",
                    "Computer",
                    "Hello Assistant"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetPhrases.take(3).forEach { preset ->
                        val isPresetActive = wakePhrase.equals(preset, ignoreCase = true)
                        OutlinedButton(
                            onClick = {
                                customPhraseInput = preset
                                viewModel.setWakePhrase(preset)
                                showSavedBanner = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPresetActive) AiraCyan else CardBorder
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isPresetActive) SurfaceVariantDark else BackgroundDark,
                                contentColor = if (isPresetActive) AiraCyan else TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(preset, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetPhrases.drop(3).forEach { preset ->
                        val isPresetActive = wakePhrase.equals(preset, ignoreCase = true)
                        OutlinedButton(
                            onClick = {
                                customPhraseInput = preset
                                viewModel.setWakePhrase(preset)
                                showSavedBanner = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPresetActive) AiraCyan else CardBorder
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isPresetActive) SurfaceVariantDark else BackgroundDark,
                                contentColor = if (isPresetActive) AiraCyan else TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(preset, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (customPhraseInput.isNotBlank()) {
                                viewModel.setWakePhrase(customPhraseInput)
                                showSavedBanner = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AiraCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text("Save Wake Phrase", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            customPhraseInput = "Hey Assistant"
                            viewModel.setWakePhrase("Hey Assistant")
                            showSavedBanner = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text("Reset", color = TextPrimary, fontSize = 12.sp)
                    }
                }

                if (showSavedBanner) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ Wake phrase updated to \"$wakePhrase\"",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SuccessGreen
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Aira runs a dedicated Vosk acoustic keyword grammar instance dynamically updated to this phrase, processing audio entirely offline.",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stop Assistant Button
        OutlinedButton(
            onClick = { viewModel.stopListening(context) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Stop Assistant Listening Service", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = AiraCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AiraCyan)
    }
}

@Composable
fun ModelItemRow(
    modelInfo: SpeechModelManager.ModelInfo,
    isInstalled: Boolean,
    isSelected: Boolean,
    isDownloading: Boolean,
    progress: Int?,
    onSelect: () -> Unit,
    onDownload: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SurfaceVariantDark else BackgroundDark)
            .border(
                1.dp,
                if (isSelected) AiraCyan else CardBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = modelInfo.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = modelInfo.description,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                if (isInstalled) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AiraCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AiraCyan)
                        }
                    } else {
                        Button(
                            onClick = onSelect,
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Use", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                } else if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = AiraCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = AiraCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Get", fontSize = 11.sp, color = BackgroundDark, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isDownloading && progress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = AiraCyan,
                    trackColor = SurfaceDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Downloading model: $progress%",
                    fontSize = 10.sp,
                    color = AiraCyan
                )
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    actionText: String = "Grant",
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = description, fontSize = 11.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SuccessGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
            }
        } else {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(actionText, fontSize = 11.sp, color = AiraCyan)
            }
        }
    }
}
