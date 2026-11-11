package com.example.aira.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aira.data.model.AssistantStatus
import com.example.aira.data.model.ListeningState
import com.example.aira.ui.components.AiraSiriOrb
import com.example.aira.ui.theme.AiraCyan
import com.example.aira.ui.theme.AiraPink
import com.example.aira.ui.theme.AiraPurple
import com.example.aira.ui.theme.TextPrimary
import com.example.aira.ui.theme.TextSecondary

/**
 * Modern Siri / Gemini-inspired floating voice assistant popup.
 * Displays the glowing electric-blue voice orb, live transcription, audio waveform,
 * and status indicators.
 */
@Composable
fun VoiceAssistantPopup(
    status: AssistantStatus,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micIndicatorTransition")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    AnimatedVisibility(
        visible = status.isPopupVisible,
        enter = fadeIn(tween(250)) + scaleIn(tween(250, easing = FastOutSlowInEasing), initialScale = 0.85f),
        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.9f)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = AiraCyan.copy(alpha = 0.35f),
                        spotColor = AiraPurple.copy(alpha = 0.5f)
                    ),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xF2080D1A) // 95% translucent dark cosmic slate
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AiraCyan.copy(alpha = 0.55f),
                            AiraPurple.copy(alpha = 0.35f),
                            Color(0x3300D2FF)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top header row: small listening pill + close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x33101C33),
                            border = BorderStroke(1.dp, Color(0x3300D2FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (status.isSpeaking) AiraCyan else AiraCyan.copy(alpha = dotAlpha)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (status.state) {
                                        ListeningState.PROCESSING_COMMAND -> "Processing..."
                                        ListeningState.SPEAKING_RESPONSE -> "Responding..."
                                        else -> "Aira is listening..."
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (status.isSpeaking) Color.White else TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss voice popup",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Animated Glowing Orb (with audio-reactive ribbons & waveform)
                    AiraSiriOrb(
                        state = status.state,
                        amplitude = status.audioAmplitude,
                        isSpeaking = status.isSpeaking,
                        size = 135.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Speech Transcription / Feedback Text
                    val displayText = when {
                        status.lastRecognizedText.isNotBlank() -> "\"${status.lastRecognizedText}\""
                        status.state == ListeningState.PROCESSING_COMMAND -> "Processing your command..."
                        status.state == ListeningState.SPEAKING_RESPONSE && status.lastAssistantResponse.isNotBlank() ->
                            status.lastAssistantResponse
                        status.isSpeaking -> "Listening..."
                        else -> "Say a command, e.g. \"Open YouTube\""
                    }

                    val isUserSpeech = status.lastRecognizedText.isNotBlank()

                    Text(
                        text = displayText,
                        fontSize = if (isUserSpeech) 18.sp else 14.sp,
                        fontWeight = if (isUserSpeech) FontWeight.SemiBold else FontWeight.Normal,
                        fontStyle = if (isUserSpeech) FontStyle.Normal else FontStyle.Italic,
                        color = if (isUserSpeech) TextPrimary else TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
