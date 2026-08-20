package com.example.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VoiceState
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun VoiceInputPulseBar(
    voiceState: VoiceState,
    amplitude: Float,
    partialText: String,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Live timer
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val formattedTime = "%02d:%02d".format(minutes, seconds)

    // Animated colors based on real-time acoustic HUD states:
    // - Grey: Silence / Idle recording.
    // - Blue: Hearing sound above threshold.
    // - Emerald Green: Speech recognized and transcribed to note.
    // - Red/Amber: Audio detected without decipherable words / Error.
    val targetPulseColor = when (voiceState) {
        VoiceState.SILENCE, VoiceState.IDLE -> Color(0xFF64748B)       // Grey (Silence / Idle recording)
        VoiceState.HEARING_SOUND -> Color(0xFF3B82F6)                  // Blue (Hearing sound above threshold)
        VoiceState.SPEECH_RECOGNIZED -> Color(0xFF10B981)              // Emerald Green (Speech recognized)
        VoiceState.UNDECIPHERABLE -> Color(0xFFF59E0B)                 // Amber (Audio detected without decipherable words)
        VoiceState.PROCESSING -> Color(0xFFA855F7)                     // Violet/Purple (Processing with model)
        VoiceState.ERROR -> Color(0xFFEF4444)                          // Red (No model / Error)
    }

    val animatedBarColor by animateColorAsState(
        targetValue = targetPulseColor,
        animationSpec = tween(durationMillis = 250),
        label = "PulseBarColor"
    )

    // Infinite transition for subtle ambient ripple even during silence
    val infiniteTransition = rememberInfiniteTransition(label = "IdleWave")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "IdleWavePhase"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            animatedBarColor.copy(alpha = 0.4f)
        ),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Top row: Status indicator, Live transcript or State text, and Duration counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(animatedBarColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    val statusLabel = when (voiceState) {
                        VoiceState.SILENCE -> "Listening... (Silence)"
                        VoiceState.HEARING_SOUND -> "Hearing sound..."
                        VoiceState.SPEECH_RECOGNIZED -> "Speech recognized"
                        VoiceState.UNDECIPHERABLE -> "Audio detected (No decipherable words)"
                        VoiceState.PROCESSING -> "Transcribing audio..."
                        VoiceState.ERROR -> "Voice input error"
                        VoiceState.IDLE -> "Listening..."
                    }

                    Text(
                        text = if (partialText.isNotBlank()) partialText else statusLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        fontWeight = if (voiceState == VoiceState.SPEECH_RECOGNIZED) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (partialText.isNotBlank()) MaterialTheme.colorScheme.onSurface else animatedBarColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: Stop button + Live Audio Waveform Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stop Button
                Button(
                    onClick = onStopClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Recording",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Stop",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dynamic Audio Waveform Equalizer (18 bars)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val barCount = 18
                    for (i in 0 until barCount) {
                        // Calculate wave shape
                        val normalizedPos = i.toFloat() / barCount
                        val sinMod = (sin(idlePhase + normalizedPos * Math.PI * 2).toFloat() + 1f) / 2f
                        
                        val baseHeight = when (voiceState) {
                            VoiceState.SPEECH_RECOGNIZED, VoiceState.HEARING_SOUND -> {
                                val dynamicAmp = (amplitude * (0.4f + 0.6f * sinMod) * 28f).coerceIn(5f, 28f)
                                dynamicAmp.dp
                            }
                            VoiceState.SILENCE, VoiceState.IDLE -> {
                                (3f + 3f * sinMod).dp
                            }
                            VoiceState.UNDECIPHERABLE -> {
                                (4f + 8f * sinMod).dp
                            }
                            VoiceState.PROCESSING -> {
                                (6f + 10f * sinMod).dp
                            }
                            else -> 4.dp
                        }

                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(baseHeight)
                                .clip(RoundedCornerShape(2.dp))
                                .background(animatedBarColor)
                        )
                    }
                }
            }
        }
    }
}
