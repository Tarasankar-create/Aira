package com.example.aira.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.aira.data.model.ListeningState
import com.example.aira.ui.theme.AiraCyan
import com.example.aira.ui.theme.AiraCyanDark
import com.example.aira.ui.theme.AiraPink
import com.example.aira.ui.theme.AiraPurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern Siri / Gemini inspired luminous voice orb designed based on the glowing
 * electric-blue swirling sphere.
 *
 * It dynamically scales, pulses, and animates its internal flowing ribbons based on
 * real-time microphone input amplitude.
 */
@Composable
fun AiraSiriOrb(
    state: ListeningState,
    amplitude: Float,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbInfiniteTransition")

    // Idle breathing cycle when no speech is heard
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )

    // Continuous flow rotation
    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuousRotation"
    )

    // Fast rotation for processing state
    val processingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "processingRotation"
    )

    // Smooth amplitude-reactive scale (bounces physically to voice volume)
    val clampedAmp = amplitude.coerceIn(0f, 1f)
    val animatedScale by animateFloatAsState(
        targetValue = if (state == ListeningState.OFF) 0.85f else (idlePulse + (clampedAmp * 0.38f)),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animatedScale"
    )

    val activeRotation = if (state == ListeningState.PROCESSING_COMMAND) {
        processingRotation
    } else {
        continuousRotation
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(animatedScale),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val canvasSize = this.size
                val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                val baseRadius = (canvasSize.minDimension / 2f) * 0.78f

                // 1. Outer Atmospheric Luminous Glow (Pulsing with voice amplitude)
                val glowRadius = baseRadius * (1.18f + clampedAmp * 0.45f)
                val glowAlpha = (0.28f + clampedAmp * 0.45f).coerceIn(0f, 0.85f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AiraCyan.copy(alpha = glowAlpha),
                            AiraPurple.copy(alpha = glowAlpha * 0.6f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = center
                )

                // 2. Cosmic Dark Core Sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0B1426),
                            Color(0xFF050A14),
                            Color(0xFF020409)
                        ),
                        center = center,
                        radius = baseRadius
                    ),
                    radius = baseRadius,
                    center = center
                )

                // 3. Electric Circumference Rim Ring
                val rimGradient = Brush.sweepGradient(
                    colors = listOf(
                        AiraCyan,
                        Color(0xFF0070F3),
                        AiraPurple,
                        AiraPink,
                        AiraCyan
                    ),
                    center = center
                )
                drawCircle(
                    brush = rimGradient,
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 3.5f + (clampedAmp * 3.5f))
                )

                // Thin hot-white rim highlight on upper-right crest
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.8f), Color.Transparent),
                        center = Offset(center.x + baseRadius * 0.6f, center.y - baseRadius * 0.6f),
                        radius = baseRadius * 0.7f
                    ),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 2f)
                )

                // 4. Swirling Luminous Ribbons (The Signature Design from the Image)
                rotate(degrees = activeRotation, pivot = center) {
                    val r = baseRadius * 0.92f

                    // Path for the primary sweeping S-curve ribbon
                    val path1 = Path().apply {
                        val startAngle = -45f * (PI / 180f)
                        val startX = center.x + r * cos(startAngle).toFloat()
                        val startY = center.y + r * sin(startAngle).toFloat()
                        moveTo(startX, startY)

                        // Control points for organic S-wave through center
                        val cp1X = center.x + r * 0.35f
                        val cp1Y = center.y - r * 0.45f
                        val cp2X = center.x - r * 0.55f
                        val cp2Y = center.y + r * 0.25f

                        val endAngle = 135f * (PI / 180f)
                        val endX = center.x + r * cos(endAngle).toFloat()
                        val endY = center.y + r * sin(endAngle).toFloat()

                        cubicTo(cp1X, cp1Y, cp2X, cp2Y, endX, endY)
                    }

                    // Soft ambient violet under-stroke
                    drawPath(
                        path = path1,
                        brush = Brush.linearGradient(
                            colors = listOf(AiraPurple.copy(alpha = 0.7f), AiraPink.copy(alpha = 0.5f))
                        ),
                        style = Stroke(
                            width = 16f + (clampedAmp * 16f),
                            cap = StrokeCap.Round
                        )
                    )

                    // Vibrant cyan & electric blue core ribbon
                    drawPath(
                        path = path1,
                        brush = Brush.linearGradient(
                            colors = listOf(AiraCyan, Color(0xFF0070F3), AiraPurple)
                        ),
                        style = Stroke(
                            width = 8f + (clampedAmp * 8f),
                            cap = StrokeCap.Round
                        )
                    )

                    // Luminous hot-white core spine
                    drawPath(
                        path = path1,
                        color = Color.White.copy(alpha = 0.9f),
                        style = Stroke(
                            width = 2.5f + (clampedAmp * 2f),
                            cap = StrokeCap.Round
                        )
                    )

                    // Secondary counter-curve ribbon (adding 3D depth like the image)
                    val path2 = Path().apply {
                        val startAngle = 140f * (PI / 180f)
                        val startX = center.x + r * cos(startAngle).toFloat()
                        val startY = center.y + r * sin(startAngle).toFloat()
                        moveTo(startX, startY)

                        val cp1X = center.x - r * 0.25f
                        val cp1Y = center.y + r * 0.55f
                        val cp2X = center.x + r * 0.45f
                        val cp2Y = center.y + r * 0.45f

                        val endAngle = 45f * (PI / 180f)
                        val endX = center.x + r * cos(endAngle).toFloat()
                        val endY = center.y + r * sin(endAngle).toFloat()

                        cubicTo(cp1X, cp1Y, cp2X, cp2Y, endX, endY)
                    }

                    drawPath(
                        path = path2,
                        brush = Brush.linearGradient(
                            colors = listOf(AiraCyan.copy(alpha = 0.8f), AiraCyanDark.copy(alpha = 0.4f))
                        ),
                        style = Stroke(
                            width = 6f + (clampedAmp * 6f),
                            cap = StrokeCap.Round
                        )
                    )
                }

                // 5. State-specific overlay flourishes
                when (state) {
                    ListeningState.PROCESSING_COMMAND -> {
                        // High-speed energetic vortex ring
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(AiraCyan, AiraPurple, Color.White, AiraCyan),
                                center = center
                            ),
                            radius = baseRadius * 0.95f,
                            center = center,
                            style = Stroke(width = 5f)
                        )
                    }
                    ListeningState.SPEAKING_RESPONSE -> {
                        // Warm glowing response aura
                        drawCircle(
                            color = AiraPink.copy(alpha = 0.3f),
                            radius = baseRadius * 1.08f,
                            center = center,
                            style = Stroke(width = 4f)
                        )
                    }
                    ListeningState.ERROR -> {
                        // Subtle warning tint
                        drawCircle(
                            color = Color(0xFFFF5252).copy(alpha = 0.4f),
                            radius = baseRadius,
                            center = center,
                            style = Stroke(width = 3f)
                        )
                    }
                    else -> Unit
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 6. Subtle Audio Waveform Visualizer
        AudioWaveformBars(
            amplitude = clampedAmp,
            isSpeaking = isSpeaking,
            state = state
        )
    }
}

/**
 * 7 reactive audio waveform bars that scale dynamically with actual microphone input.
 */
@Composable
fun AudioWaveformBars(
    amplitude: Float,
    isSpeaking: Boolean,
    state: ListeningState,
    modifier: Modifier = Modifier
) {
    val barWeights = listOf(0.35f, 0.6f, 0.85f, 1.0f, 0.85f, 0.6f, 0.35f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barWeights.forEach { weight ->
            val minHeight = 4.dp
            val maxHeight = 28.dp

            val targetHeight = if (state == ListeningState.OFF) {
                minHeight
            } else {
                val voiceBoost = if (isSpeaking) 1.2f else 0.4f
                val dynamicHeight = minHeight + (maxHeight - minHeight) * (amplitude * weight * voiceBoost)
                dynamicHeight.coerceIn(minHeight, maxHeight)
            }

            val animatedHeight by animateDpAsState(
                targetValue = targetHeight,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "barHeight"
            )

            Surface(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(animatedHeight),
                shape = RoundedCornerShape(50),
                color = if (isSpeaking) AiraCyan else AiraCyan.copy(alpha = 0.45f)
            ) {}
        }
    }
}
