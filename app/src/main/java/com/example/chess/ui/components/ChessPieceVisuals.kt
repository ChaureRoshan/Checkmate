package com.example.chess.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType

/**
 * Flat, outlined pieces (same look as chess.com / lichess "cartoon" sets).
 * Every shape is drawn in a 100 x 100 box and scaled to the square.
 */
private enum class Role { BODY, LINE, DOT }

private class Shape(val path: Path, val role: Role)

private fun shape(d: String, role: Role) = Shape(PathParser().parsePathString(d).toPath(), role)

private val PieceShapes: Map<PieceType, List<Shape>> by lazy {
    mapOf(
        PieceType.PAWN to listOf(
            shape("M 25 80 L 75 80 Q 80 80 80 85 L 80 87 Q 80 92 75 92 L 25 92 Q 20 92 20 87 L 20 85 Q 20 80 25 80 Z", Role.BODY),
            shape("M 30 80 C 32 70 38 62 42 54 L 58 54 C 62 62 68 70 70 80 Z", Role.BODY),
            shape("M 39 46 L 61 46 Q 66 46 66 51 L 66 51 Q 66 56 61 56 L 39 56 Q 34 56 34 51 L 34 51 Q 34 46 39 46 Z", Role.BODY),
            shape("M 50 18 C 57.18 18 63 23.82 63 31 C 63 38.18 57.18 44 50 44 C 42.82 44 37 38.18 37 31 C 37 23.82 42.82 18 50 18 Z", Role.BODY),
        ),
        PieceType.ROOK to listOf(
            shape("M 25 80 L 75 80 Q 80 80 80 85 L 80 87 Q 80 92 75 92 L 25 92 Q 20 92 20 87 L 20 85 Q 20 80 25 80 Z", Role.BODY),
            shape("M 28 80 L 33 40 L 67 40 L 72 80 Z", Role.BODY),
            shape("M 26 40 L 26 16 L 38 16 L 38 24 L 44 24 L 44 16 L 56 16 L 56 24 L 62 24 L 62 16 L 74 16 L 74 40 Z", Role.BODY),
            shape("M 33 40 L 67 40", Role.LINE),
        ),
        PieceType.BISHOP to listOf(
            shape("M 25 80 L 75 80 Q 80 80 80 85 L 80 87 Q 80 92 75 92 L 25 92 Q 20 92 20 87 L 20 85 Q 20 80 25 80 Z", Role.BODY),
            shape("M 32 80 C 34 70 40 64 42 56 L 58 56 C 60 64 66 70 68 80 Z", Role.BODY),
            shape("M 38 50 L 62 50 Q 66 50 66 54 L 66 55 Q 66 59 62 59 L 38 59 Q 34 59 34 55 L 34 54 Q 34 50 38 50 Z", Role.BODY),
            shape("M 50 18 C 62 26 70 36 68 46 C 67 50 64 50 60 50 L 40 50 C 36 50 33 50 32 46 C 30 36 38 26 50 18 Z", Role.BODY),
            shape("M 50 8.5 C 53.04 8.5 55.5 10.96 55.5 14 C 55.5 17.04 53.04 19.5 50 19.5 C 46.96 19.5 44.5 17.04 44.5 14 C 44.5 10.96 46.96 8.5 50 8.5 Z", Role.BODY),
            shape("M 52 26 L 58 36", Role.LINE),
        ),
        PieceType.KNIGHT to listOf(
            shape("M 25 80 L 75 80 Q 80 80 80 85 L 80 87 Q 80 92 75 92 L 25 92 Q 20 92 20 87 L 20 85 Q 20 80 25 80 Z", Role.BODY),
            shape("M 28 80 C 28 62 34 52 42 44 C 38 45 34 48 30 50 C 25 53 20 52 19 48 C 18 44 24 36 30 30 C 34 26 36 22 38 15 C 42 18 44 18 48 15 L 52 11 C 54 17 58 19 62 23 C 72 31 78 50 74 80 Z", Role.BODY),
            shape("M 32 80 L 74 80", Role.LINE),
            shape("M 40 28.4 C 41.44 28.4 42.6 29.56 42.6 31 C 42.6 32.44 41.44 33.6 40 33.6 C 38.56 33.6 37.4 32.44 37.4 31 C 37.4 29.56 38.56 28.4 40 28.4 Z", Role.DOT),
            shape("M 21 47 C 22 49 24 49 25 47", Role.LINE),
            shape("M 54 24 C 62 30 66 40 66 52", Role.LINE),
        ),
        PieceType.QUEEN to listOf(
            shape("M 25 80 L 75 80 Q 80 80 80 85 L 80 87 Q 80 92 75 92 L 25 92 Q 20 92 20 87 L 20 85 Q 20 80 25 80 Z", Role.BODY),
            shape("M 28 80 L 32 46 L 68 46 L 72 80 Z", Role.BODY),
            shape("M 22 24 L 34 44 L 40 22 L 50 42 L 60 22 L 66 44 L 78 24 L 74 52 L 26 52 Z", Role.BODY),
            shape("M 34 50 L 66 50 Q 70 50 70 54 L 70 55 Q 70 59 66 59 L 34 59 Q 30 59 30 55 L 30 54 Q 30 50 34 50 Z", Role.BODY),
            shape("M 22 17 C 24.76 17 27 19.24 27 22 C 27 24.76 24.76 27 22 27 C 19.24 27 17 24.76 17 22 C 17 19.24 19.24 17 22 17 Z", Role.BODY),
            shape("M 40 12 C 42.76 12 45 14.24 45 17 C 45 19.76 42.76 22 40 22 C 37.24 22 35 19.76 35 17 C 35 14.24 37.24 12 40 12 Z", Role.BODY),
            shape("M 60 12 C 62.76 12 65 14.24 65 17 C 65 19.76 62.76 22 60 22 C 57.24 22 55 19.76 55 17 C 55 14.24 57.24 12 60 12 Z", Role.BODY),
            shape("M 78 17 C 80.76 17 83 19.24 83 22 C 83 24.76 80.76 27 78 27 C 75.24 27 73 24.76 73 22 C 73 19.24 75.24 17 78 17 Z", Role.BODY),
            shape("M 50 8 C 52.76 8 55 10.24 55 13 C 55 15.76 52.76 18 50 18 C 47.24 18 45 15.76 45 13 C 45 10.24 47.24 8 50 8 Z", Role.BODY),
        ),
        PieceType.KING to listOf(
            shape("M 25 80 L 75 80 Q 80 80 80 85 L 80 87 Q 80 92 75 92 L 25 92 Q 20 92 20 87 L 20 85 Q 20 80 25 80 Z", Role.BODY),
            shape("M 28 80 L 32 52 L 68 52 L 72 80 Z", Role.BODY),
            shape("M 24 56 C 18 42 24 32 36 30 C 42 29 46 32 50 36 C 54 32 58 29 64 30 C 76 32 82 42 76 56 Z", Role.BODY),
            shape("M 34 54 L 66 54 Q 70 54 70 58 L 70 59 Q 70 63 66 63 L 34 63 Q 30 63 30 59 L 30 58 Q 30 54 34 54 Z", Role.BODY),
            shape("M 46 8 L 54 8 L 54 14 L 60 14 L 60 21 L 54 21 L 54 36 L 46 36 L 46 21 L 40 21 L 40 14 L 46 14 Z", Role.BODY),
        ),
    )
}

private val WhiteFill = Color(0xFFF5F5F5)
private val WhiteLine = Color(0xFF4A4A4A)
private val BlackFill = Color(0xFF4C4C4C)
private val BlackLine = Color(0xFF262626)

@Composable
fun ChessPieceVisual(
    piece: ChessPiece,
    modifier: Modifier = Modifier
) {
    val isWhite = piece.color == PieceColor.WHITE
    val fill = if (isWhite) WhiteFill else BlackFill
    val line = if (isWhite) WhiteLine else BlackLine
    val shapes = PieceShapes[piece.type] ?: return

    Canvas(modifier = modifier.fillMaxSize()) {
        val s = minOf(size.width, size.height) / 100f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            for (sh in shapes) {
                when (sh.role) {
                    Role.BODY -> {
                        drawPath(sh.path, fill)
                        drawPath(sh.path, line, style = Stroke(width = 1.7f, join = StrokeJoin.Round))
                    }
                    Role.LINE -> drawPath(
                        sh.path, line,
                        style = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    Role.DOT -> drawPath(sh.path, line)
                }
            }
        }
    }
}
