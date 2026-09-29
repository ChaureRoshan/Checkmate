package com.example.chess.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.voice.VoiceChatState
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceVariant

@Composable
fun VoiceChatBar(
    voiceState: VoiceChatState,
    opponentName: String,
    onToggleMic: () -> Unit,
    onStartPushToTalk: () -> Unit,
    onStopPushToTalk: () -> Unit,
    onToggleDeafen: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp)),
        color = ObsidianSurfaceVariant.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status and Visualizer Area
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Mic State Indicator icon
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !voiceState.hasPermission -> Color.Gray.copy(alpha = 0.2f)
                                voiceState.isRecording || voiceState.isPushToTalk -> ChampagneGold.copy(alpha = 0.2f)
                                voiceState.isOpponentSpeaking -> EmeraldAccent.copy(alpha = 0.2f)
                                else -> Color.White.copy(alpha = 0.05f)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                voiceState.isRecording -> ChampagneGold
                                voiceState.isOpponentSpeaking -> EmeraldAccent
                                else -> ObsidianBorder
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when {
                        !voiceState.hasPermission -> Icons.Default.MicOff
                        voiceState.isOpponentSpeaking -> Icons.Default.RecordVoiceOver
                        voiceState.isMicMuted && !voiceState.isPushToTalk -> Icons.Default.MicOff
                        else -> Icons.Default.Mic
                    }
                    val iconTint = when {
                        voiceState.isRecording -> ChampagneGold
                        voiceState.isOpponentSpeaking -> EmeraldAccent
                        voiceState.isMicMuted -> Color.LightGray.copy(alpha = 0.7f)
                        else -> ElectricCyan
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Voice Status",
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Dynamic Audio Waveform or Status Label
                Column {
                    val label = when {
                        !voiceState.hasPermission -> "Tap mic to enable voice chat"
                        voiceState.isOpponentSpeaking -> "$opponentName is speaking..."
                        voiceState.isPushToTalk -> "Speaking (Hold to talk)..."
                        !voiceState.isMicMuted -> "Microphone Live"
                        else -> "Voice Chat Active (Opponent)"
                    }
                    val labelColor = when {
                        voiceState.isOpponentSpeaking -> EmeraldAccent
                        voiceState.isRecording -> ChampagneGold
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = labelColor
                    )

                    // Audio visualizer bars
                    val activeAmplitude = if (voiceState.isOpponentSpeaking) {
                        voiceState.opponentAmplitude
                    } else if (voiceState.isRecording) {
                        voiceState.userAmplitude
                    } else 0f

                    AudioVisualizer(
                        amplitude = activeAmplitude,
                        isActive = voiceState.isRecording || voiceState.isOpponentSpeaking,
                        color = if (voiceState.isOpponentSpeaking) EmeraldAccent else ChampagneGold,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            // Controls: Push to Talk / Mic Toggle & Deafen
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (voiceState.hasPermission) {
                    // Push to Talk Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (voiceState.isPushToTalk) ChampagneGold else ObsidianSurfaceVariant
                            )
                            .border(
                                1.dp,
                                if (voiceState.isPushToTalk) ChampagneGold else ObsidianBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onStartPushToTalk()
                                        tryAwaitRelease()
                                        onStopPushToTalk()
                                    }
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("push_to_talk_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hold to Talk",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (voiceState.isPushToTalk) Color.Black else ChampagneGold
                        )
                    }

                    // Mic Mute Toggle
                    IconButton(
                        onClick = onToggleMic,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_mic_button")
                    ) {
                        Icon(
                            imageVector = if (voiceState.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Toggle Mic",
                            tint = if (voiceState.isMicMuted) Color.Gray else ChampagneGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Deafen Toggle
                    IconButton(
                        onClick = onToggleDeafen,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_deafen_button")
                    ) {
                        Icon(
                            imageVector = if (voiceState.isDeafened) Icons.Default.HeadsetOff else Icons.Default.Headset,
                            contentDescription = "Deafen Audio",
                            tint = if (voiceState.isDeafened) Color.Gray else ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // Permission Request Prompt
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ChampagneGold)
                            .border(1.dp, ChampagneGold, RoundedCornerShape(10.dp))
                            .clickable { onRequestPermission() }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("enable_voice_permission_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Enable Voice",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioVisualizer(
    amplitude: Float,
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bars = 6
        for (i in 0 until bars) {
            val baseHeight = 4.dp
            val scaleFactor = remember(i) { 0.6f + (i % 3) * 0.2f }
            val dynamicHeight = if (isActive) {
                val wave = (amplitude * 16.dp.value * scaleFactor).coerceIn(3f, 16f)
                wave.dp
            } else {
                baseHeight
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(dynamicHeight)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (isActive) color else Color.Gray.copy(alpha = 0.3f))
            )
        }
    }
}
