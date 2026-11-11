package com.example.aira.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.aira.data.model.ListeningState
import com.example.aira.ui.theme.AiraCyan
import com.example.aira.ui.theme.AiraCyanDark
import com.example.aira.ui.theme.AiraPink
import com.example.aira.ui.theme.AiraPurple
import com.example.aira.ui.theme.CardBorder

@Composable
fun AiraOrb(
    state: ListeningState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbTransition")

    // Breathing pulse for idle wake-word state
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Fast rotation for processing state
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Halo wave animation for command listening
    val haloExpansion by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloExpansion"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloAlpha"
    )

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 3.4f

            when (state) {
                ListeningState.OFF -> {
                    // Inactive muted state
                    drawCircle(
                        color = CardBorder,
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 4f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1E2232), Color(0xFF12141D)),
                            center = center,
                            radius = baseRadius
                        ),
                        radius = baseRadius - 4f,
                        center = center
                    )
                }

                ListeningState.STARTING,
                ListeningState.LISTENING_FOR_WAKE_WORD,
                ListeningState.WAKE_WORD_DETECTED -> {
                    // Breathing cyan glow waiting for "Hey Assistant"
                    val currentRadius = baseRadius * pulseScale

                    // Subtle outer glow ring
                    drawCircle(
                        color = AiraCyan.copy(alpha = 0.2f),
                        radius = currentRadius * 1.25f,
                        center = center
                    )

                    // Core glowing orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AiraCyan, AiraCyanDark, Color(0xFF003852)),
                            center = center,
                            radius = currentRadius
                        ),
                        radius = currentRadius,
                        center = center
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = currentRadius,
                        center = center,
                        style = Stroke(width = 3f)
                    )
                }

                ListeningState.LISTENING_FOR_COMMAND -> {
                    // High-energy amplitude reactive waves
                    val reactiveRadius = baseRadius + (amplitude * 50f)

                    // Expanding halo wave
                    drawCircle(
                        color = AiraCyan.copy(alpha = haloAlpha),
                        radius = baseRadius * haloExpansion,
                        center = center,
                        style = Stroke(width = 4f)
                    )

                    // Outer reactive wave
                    drawCircle(
                        color = AiraCyan.copy(alpha = 0.35f),
                        radius = reactiveRadius * 1.3f,
                        center = center
                    )

                    // Core orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, AiraCyan, AiraPurple),
                            center = center,
                            radius = reactiveRadius
                        ),
                        radius = reactiveRadius,
                        center = center
                    )
                }

                ListeningState.PROCESSING_COMMAND -> {
                    // Spinning gradient vortex
                    drawContext.transform.rotate(rotation, center)
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(AiraCyan, AiraPurple, AiraPink, AiraCyan),
                            center = center
                        ),
                        radius = baseRadius * 1.15f,
                        center = center,
                        style = Stroke(width = 6f)
                    )
                    drawContext.transform.rotate(-rotation, center)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AiraPurple, Color(0xFF1B0B33)),
                            center = center,
                            radius = baseRadius
                        ),
                        radius = baseRadius,
                        center = center
                    )
                }

                ListeningState.SPEAKING_RESPONSE -> {
                    // Warm pulsating purple & pink aura
                    val speechRadius = baseRadius * pulseScale

                    drawCircle(
                        color = AiraPink.copy(alpha = 0.25f),
                        radius = speechRadius * 1.35f,
                        center = center
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AiraPink, AiraPurple, Color(0xFF280B3A)),
                            center = center,
                            radius = speechRadius
                        ),
                        radius = speechRadius,
                        center = center
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = speechRadius,
                        center = center,
                        style = Stroke(width = 3f)
                    )
                }

                ListeningState.ERROR -> {
                    // Red error ring
                    drawCircle(
                        color = Color(0xFFFF5252).copy(alpha = 0.4f),
                        radius = baseRadius * 1.2f,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF5252), Color(0xFF420A0A)),
                            center = center,
                            radius = baseRadius
                        ),
                        radius = baseRadius,
                        center = center
                    )
                }
            }
        }
    }
}
