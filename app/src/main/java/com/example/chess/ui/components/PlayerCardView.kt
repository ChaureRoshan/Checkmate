package com.example.chess.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceColor
import com.example.chess.model.PlayerInfo
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonCheck
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceVariant
import java.util.Locale

@Composable
fun PlayerCardView(
    player: PlayerInfo,
    color: PieceColor,
    remainingTimeMs: Long,
    isTurn: Boolean,
    capturedPieces: List<ChessPiece>,
    materialAdvantage: Int,
    isOpponent: Boolean,
    modifier: Modifier = Modifier
) {
    val totalSeconds = (remainingTimeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = ((remainingTimeMs % 1000) / 100).coerceAtLeast(0)

    val isTimeOut = remainingTimeMs <= 0
    val isCriticalTime = totalSeconds < 15 && !isTimeOut
    val isWarningTime = totalSeconds < 45 && !isCriticalTime && !isTimeOut

    val timeFormatted = when {
        isTimeOut -> "00:00.0"
        totalSeconds < 20 -> String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, tenths)
        else -> String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "critical_clock_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "clock_pulse_alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isTurn) 1.5.dp else 1.dp,
                color = when {
                    isTimeOut -> CrimsonCheck
                    isCriticalTime && isTurn -> CrimsonCheck.copy(alpha = pulseAlpha)
                    isTurn -> ChampagneGold
                    else -> ObsidianBorder
                },
                shape = RoundedCornerShape(14.dp)
            ),
        color = if (isTurn) ObsidianSurfaceVariant else ObsidianSurfaceVariant.copy(alpha = 0.55f)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Avatar + Name + Rating + Color indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Avatar & Piece Color Badge
                    Box {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOpponent) ElectricCyan.copy(alpha = 0.2f)
                                    else ChampagneGold.copy(alpha = 0.2f)
                                )
                                .border(
                                    1.dp,
                                    if (isOpponent) ElectricCyan.copy(alpha = 0.5f)
                                    else ChampagneGold.copy(alpha = 0.5f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (player.isSpeaking) Icons.Default.RecordVoiceOver else Icons.Default.Person,
                                contentDescription = player.name,
                                tint = if (player.isSpeaking) EmeraldAccent else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Piece color dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(if (color == PieceColor.WHITE) Color.White else Color.Black)
                                .border(1.dp, Color.Gray, CircleShape)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = player.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (player.isGuest) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "GUEST",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChampagneGold
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${player.rating} ELO",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Captured pieces preview
                            if (capturedPieces.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((-4).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (p in capturedPieces.takeLast(4)) {
                                        Box(modifier = Modifier.size(16.dp)) {
                                            ChessPieceVisual(piece = p)
                                        }
                                    }
                                }
                            }

                            if (materialAdvantage > 0) {
                                Text(
                                    text = "+$materialAdvantage",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChampagneGold
                                )
                            }
                        }
                    }
                }

                // Right: Chess Clock Countdown Timer
                val clockBgColor = when {
                    isTimeOut -> CrimsonCheck.copy(alpha = 0.35f)
                    isCriticalTime && isTurn -> CrimsonCheck.copy(alpha = 0.25f * pulseAlpha)
                    isWarningTime && isTurn -> ChampagneGold.copy(alpha = 0.2f)
                    isTurn -> ChampagneGold.copy(alpha = 0.15f)
                    else -> Color.Black.copy(alpha = 0.35f)
                }

                val clockBorderColor = when {
                    isTimeOut -> CrimsonCheck
                    isCriticalTime && isTurn -> CrimsonCheck.copy(alpha = pulseAlpha)
                    isWarningTime && isTurn -> ChampagneGold.copy(alpha = 0.8f)
                    isTurn -> ChampagneGold
                    else -> ObsidianBorder
                }

                val clockTextColor = when {
                    isTimeOut -> CrimsonCheck
                    isCriticalTime -> CrimsonCheck
                    isWarningTime -> ChampagneGold
                    isTurn -> ChampagneGold
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(clockBgColor)
                        .border(1.2.dp, clockBorderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag(if (isOpponent) "opponent_clock" else "player_clock")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isTimeOut) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Time Out",
                                tint = CrimsonCheck,
                                modifier = Modifier.size(14.dp)
                            )
                        } else if (isTurn) {
                            // Ticking indicator pulse dot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(clockTextColor)
                            )
                        }

                        Text(
                            text = timeFormatted,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (totalSeconds < 20 && !isTimeOut) 15.sp else 16.sp,
                            color = clockTextColor
                        )

                        if (isTimeOut) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CrimsonCheck)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "0:00",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Active turn underline
            if (isTurn) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(if (isCriticalTime) CrimsonCheck else ChampagneGold)
                )
            }
        }
    }
}
