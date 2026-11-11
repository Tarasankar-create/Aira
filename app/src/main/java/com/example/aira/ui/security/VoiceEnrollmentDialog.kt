package com.example.aira.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

@Composable
fun VoiceEnrollmentDialog(
    viewModel: AiraViewModel,
    onDismissRequest: () -> Unit
) {
    val isEnrolling by viewModel.isEnrollingVoice.collectAsState()
    val countdown by viewModel.enrollmentCountdown.collectAsState()
    val progress by viewModel.enrollmentProgress.collectAsState()
    val amplitude by viewModel.enrollmentAmplitude.collectAsState()
    val error by viewModel.enrollmentError.collectAsState()
    val isSuccess by viewModel.enrollmentSuccess.collectAsState()

    Dialog(
        onDismissRequest = {
            if (!isEnrolling) {
                viewModel.resetEnrollmentState()
                onDismissRequest()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isEnrolling,
            dismissOnClickOutside = !isEnrolling
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        AiraCyan.copy(alpha = 0.8f),
                        AiraPurple.copy(alpha = 0.6f),
                        AiraPink.copy(alpha = 0.4f)
                    )
                )
            ),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    when {
                                        isSuccess -> SuccessGreen.copy(alpha = 0.3f)
                                        error != null -> ErrorRed.copy(alpha = 0.3f)
                                        isEnrolling -> AiraCyan.copy(alpha = 0.35f)
                                        else -> AiraPurple.copy(alpha = 0.25f)
                                    },
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            1.dp,
                            when {
                                isSuccess -> SuccessGreen.copy(alpha = 0.8f)
                                error != null -> ErrorRed.copy(alpha = 0.8f)
                                else -> AiraCyan.copy(alpha = 0.6f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isSuccess -> Icons.Default.CheckCircle
                            error != null -> Icons.Default.ErrorOutline
                            isEnrolling -> Icons.Default.RecordVoiceOver
                            else -> Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = when {
                            isSuccess -> SuccessGreen
                            error != null -> ErrorRed
                            else -> AiraCyan
                        },
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = when {
                        isSuccess -> "Owner Voice Registered!"
                        isEnrolling -> "Recording Voice ($countdown s)..."
                        error != null -> "Registration Incomplete"
                        else -> "Register Owner Voice"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isSuccess -> SuccessGreen
                        error != null -> ErrorRed
                        else -> TextPrimary
                    },
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = when {
                        isSuccess -> "Aira is now secured to your unique voice. Anyone else attempting to issue commands will be rejected."
                        isEnrolling -> "Speak the phrase clearly in your normal voice..."
                        error != null -> error ?: "Could not capture voiceprint. Please try again."
                        else -> "Aira only accepts commands from its owner. Speak the passphrase below to create your offline voice biometric profile."
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Passphrase Card (shown when enrolling or before enrolling)
                if (!isSuccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceVariantDark)
                            .border(
                                1.dp,
                                if (isEnrolling) AiraCyan else CardBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "PHRASE TO SPEAK:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AiraCyan,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"Hey Assistant, this is my voice\"",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Recording Visualizer (Waveform / Pulsing Ring)
                if (isEnrolling) {
                    val pulseScale = 1.0f + (amplitude * 0.8f).coerceIn(0f, 0.6f)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        AiraCyan.copy(alpha = 0.4f * pulseScale),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(
                                width = (1.5f + amplitude * 3f).dp,
                                color = AiraCyan.copy(alpha = (0.5f + amplitude).coerceIn(0.5f, 1f)),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${countdown}s",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AiraCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AiraCyan,
                        trackColor = SurfaceVariantDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Listening... ${(progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                when {
                    isSuccess -> {
                        Button(
                            onClick = {
                                viewModel.resetEnrollmentState()
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = BackgroundDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "DONE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    isEnrolling -> {
                        OutlinedButton(
                            onClick = { viewModel.cancelVoiceEnrollment() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text("Cancel Recording", fontSize = 13.sp)
                        }
                    }

                    error != null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.resetEnrollmentState()
                                    onDismissRequest()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                            ) {
                                Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { viewModel.startVoiceEnrollment() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AiraCyan,
                                    contentColor = BackgroundDark
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Try Again", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    else -> {
                        // Initial un-enrolled state
                        Button(
                            onClick = { viewModel.startVoiceEnrollment() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AiraCyan,
                                contentColor = BackgroundDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "START RECORDING",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = {
                                viewModel.resetEnrollmentState()
                                onDismissRequest()
                            }
                        ) {
                            Text(
                                text = "Skip for Now",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
