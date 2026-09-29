package com.example.chess.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.ChessEngine
import com.example.chess.model.BoardTheme
import com.example.chess.model.ChessPiece
import com.example.chess.model.GameStatus
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Position
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonCheck
import com.example.ui.theme.CrimsonCheckGlow
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.MidnightDark
import com.example.ui.theme.MidnightLight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SlateLight
import com.example.ui.theme.WalnutDark
import com.example.ui.theme.WalnutLight

@Composable
fun ChessBoardView(
    board: ChessBoard,
    selectedPos: Position?,
    legalMoves: List<Position>,
    perspective: PieceColor = PieceColor.WHITE,
    theme: BoardTheme = BoardTheme.SLATE,
    onSquareClick: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    val (lightColor, darkColor) = when (theme) {
        BoardTheme.SLATE -> SlateLight to SlateDark
        BoardTheme.WALNUT -> WalnutLight to WalnutDark
        BoardTheme.EMERALD -> EmeraldLight to EmeraldDark
        BoardTheme.MIDNIGHT -> MidnightLight to MidnightDark
    }

    val kingInCheckPos = if (board.gameStatus == GameStatus.CHECK || board.gameStatus == GameStatus.CHECKMATE) {
        ChessEngine.findKing(board, board.turn)
    } else null

    val infiniteTransition = rememberInfiniteTransition(label = "check_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(12.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, ObsidianBorder, RoundedCornerShape(12.dp))
    ) {
        val squareSize = maxWidth / 8

        Column(modifier = Modifier.fillMaxSize()) {
            for (displayRow in 0..7) {
                val actualRow = if (perspective == PieceColor.WHITE) displayRow else 7 - displayRow

                Row(modifier = Modifier.fillMaxWidth()) {
                    for (displayCol in 0..7) {
                        val actualCol = if (perspective == PieceColor.WHITE) displayCol else 7 - displayCol
                        val pos = Position(actualRow, actualCol)

                        val isLightSquare = (actualRow + actualCol) % 2 == 0
                        val baseSquareColor = if (isLightSquare) lightColor else darkColor

                        val isSelected = selectedPos == pos
                        val isLegalMove = pos in legalMoves
                        val isLastMove = board.lastMove?.from == pos || board.lastMove?.to == pos
                        val isKingInCheck = kingInCheckPos == pos

                        val piece = board.pieceAt(pos)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(baseSquareColor)
                                .drawBehind {
                                    if (isLastMove) {
                                        drawRect(color = ChampagneGold.copy(alpha = 0.35f))
                                    }
                                    if (isSelected) {
                                        drawRect(color = ChampagneGold.copy(alpha = 0.5f))
                                    }
                                    if (isKingInCheck) {
                                        drawRect(color = CrimsonCheck.copy(alpha = pulseAlpha * 0.7f))
                                    }
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onSquareClick(pos)
                                }
                                .testTag("square_${pos.toAlgebraic()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            // File and Rank coordinate labels on borders
                            val showRankLabel = displayCol == 0
                            val showFileLabel = displayRow == 7

                            if (showRankLabel) {
                                Text(
                                    text = "${8 - actualRow}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLightSquare) darkColor.copy(alpha = 0.85f) else lightColor.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 3.dp, top = 2.dp)
                                )
                            }

                            if (showFileLabel) {
                                Text(
                                    text = "${('a'.code + actualCol).toChar()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLightSquare) darkColor.copy(alpha = 0.85f) else lightColor.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 3.dp, bottom = 2.dp)
                                )
                            }

                            // Piece rendering
                            if (piece != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(squareSize * 0.12f)
                                ) {
                                    ChessPieceVisual(piece = piece)
                                }
                            }

                            // Legal Move Marker
                            if (isLegalMove) {
                                if (piece != null) {
                                    // Capture target halo ring
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .border(3.dp, ChampagneGold.copy(alpha = 0.9f), CircleShape)
                                    )
                                } else {
                                    // Move destination dot
                                    Box(
                                        modifier = Modifier
                                            .size(squareSize * 0.28f)
                                            .clip(CircleShape)
                                            .background(
                                                if (isLightSquare)
                                                    darkColor.copy(alpha = 0.45f)
                                                else
                                                    lightColor.copy(alpha = 0.55f)
                                            )
                                    )
                                }
                            }

                            // Selected border outline
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(2.5.dp, ChampagneGold, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PawnPromotionDialog(
    color: PieceColor,
    onSelectPiece: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ChampagneGold),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Promote Pawn",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ChampagneGold
                )
                Text(
                    text = "Choose a piece to replace your pawn",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val promotionChoices = listOf(
                        PieceType.QUEEN,
                        PieceType.KNIGHT,
                        PieceType.ROOK,
                        PieceType.BISHOP
                    )

                    for (type in promotionChoices) {
                        val piece = ChessPiece("prom_${type.name}", type, color)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                                .clickable { onSelectPiece(type) }
                                .padding(8.dp)
                                .testTag("promote_${type.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            ChessPieceVisual(piece = piece)
                        }
                    }
                }
            }
        }
    }
}
