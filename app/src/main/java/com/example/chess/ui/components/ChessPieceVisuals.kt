package com.example.chess.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType

// Color constants for pieces
private val PieceWhiteFill = Color(0xFFF8FAFC)
private val PieceWhiteStroke = Color(0xFF94A3B8)
private val PieceWhiteInnerAccent = Color(0xFFE2E8F0)

private val PieceBlackFill = Color(0xFF1E2430)
private val PieceBlackStroke = Color(0xFF64748B)
private val PieceBlackInnerAccent = Color(0xFF334155)

@Composable
fun ChessPieceVisual(
    piece: ChessPiece,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val isWhite = piece.color == PieceColor.WHITE
        val fillColor = if (isWhite) PieceWhiteFill else PieceBlackFill
        val strokeColor = if (isWhite) PieceWhiteStroke else PieceBlackStroke
        val accentColor = if (isWhite) PieceWhiteInnerAccent else PieceBlackInnerAccent

        val strokeWidth = w * 0.04f

        when (piece.type) {
            PieceType.PAWN -> drawPawn(w, h, fillColor, strokeColor, strokeWidth)
            PieceType.KNIGHT -> drawKnight(w, h, fillColor, strokeColor, strokeWidth, isWhite)
            PieceType.BISHOP -> drawBishop(w, h, fillColor, strokeColor, strokeWidth, accentColor)
            PieceType.ROOK -> drawRook(w, h, fillColor, strokeColor, strokeWidth)
            PieceType.QUEEN -> drawQueen(w, h, fillColor, strokeColor, strokeWidth)
            PieceType.KING -> drawKing(w, h, fillColor, strokeColor, strokeWidth)
        }
    }
}

private fun DrawScope.drawPawn(w: Float, h: Float, fill: Color, stroke: Color, strokeWidth: Float) {
    // Head circle
    drawCircle(color = fill, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.32f))
    drawCircle(color = stroke, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.32f), style = Stroke(strokeWidth))

    // Body & Base
    val path = Path().apply {
        moveTo(w * 0.38f, h * 0.44f)
        cubicTo(w * 0.40f, h * 0.60f, w * 0.32f, h * 0.72f, w * 0.24f, h * 0.82f)
        lineTo(w * 0.76f, h * 0.82f)
        cubicTo(w * 0.68f, h * 0.72f, w * 0.60f, h * 0.60f, w * 0.62f, h * 0.44f)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

    // Base pedestal
    val base = Path().apply {
        moveTo(w * 0.20f, h * 0.82f)
        lineTo(w * 0.80f, h * 0.82f)
        lineTo(w * 0.82f, h * 0.88f)
        lineTo(w * 0.18f, h * 0.88f)
        close()
    }
    drawPath(base, fill)
    drawPath(base, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
}

private fun DrawScope.drawKnight(w: Float, h: Float, fill: Color, stroke: Color, strokeWidth: Float, isWhite: Boolean) {
    val path = Path().apply {
        moveTo(w * 0.68f, h * 0.82f)
        lineTo(w * 0.28f, h * 0.82f)
        cubicTo(w * 0.30f, h * 0.76f, w * 0.32f, h * 0.70f, w * 0.35f, h * 0.65f)
        lineTo(w * 0.26f, h * 0.60f)
        cubicTo(w * 0.22f, h * 0.54f, w * 0.22f, h * 0.46f, w * 0.26f, h * 0.42f)
        lineTo(w * 0.32f, h * 0.38f)
        cubicTo(w * 0.30f, h * 0.33f, w * 0.32f, h * 0.28f, w * 0.37f, h * 0.25f)
        cubicTo(w * 0.42f, h * 0.22f, w * 0.46f, h * 0.24f, w * 0.50f, h * 0.18f)
        cubicTo(w * 0.54f, h * 0.22f, w * 0.58f, h * 0.23f, w * 0.62f, h * 0.28f)
        cubicTo(w * 0.68f, h * 0.35f, w * 0.74f, h * 0.46f, w * 0.74f, h * 0.60f)
        cubicTo(w * 0.74f, h * 0.70f, w * 0.70f, h * 0.78f, w * 0.68f, h * 0.82f)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round, cap = StrokeCap.Round))

    // Eye
    val eyeColor = if (isWhite) stroke else fill
    drawCircle(color = eyeColor, radius = w * 0.035f, center = Offset(w * 0.42f, h * 0.36f))

    // Mane notch
    val mane = Path().apply {
        moveTo(w * 0.58f, h * 0.34f)
        lineTo(w * 0.52f, h * 0.40f)
    }
    drawPath(mane, stroke, style = Stroke(strokeWidth * 0.9f, cap = StrokeCap.Round))

    // Pedestal
    val base = Path().apply {
        moveTo(w * 0.22f, h * 0.82f)
        lineTo(w * 0.78f, h * 0.82f)
        lineTo(w * 0.80f, h * 0.88f)
        lineTo(w * 0.20f, h * 0.88f)
        close()
    }
    drawPath(base, fill)
    drawPath(base, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
}

private fun DrawScope.drawBishop(w: Float, h: Float, fill: Color, stroke: Color, strokeWidth: Float, accent: Color) {
    // Top cross sphere
    drawCircle(color = fill, radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.16f))
    drawCircle(color = stroke, radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.16f), style = Stroke(strokeWidth))

    // Bishop Mitre Head
    val mitre = Path().apply {
        moveTo(w * 0.5f, h * 0.22f)
        cubicTo(w * 0.66f, h * 0.28f, w * 0.68f, h * 0.46f, w * 0.58f, h * 0.56f)
        lineTo(w * 0.68f, h * 0.80f)
        lineTo(w * 0.32f, h * 0.80f)
        lineTo(w * 0.42f, h * 0.56f)
        cubicTo(w * 0.32f, h * 0.46f, w * 0.34f, h * 0.28f, w * 0.5f, h * 0.22f)
        close()
    }
    drawPath(mitre, fill)
    drawPath(mitre, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

    // Slit
    val slit = Path().apply {
        moveTo(w * 0.48f, h * 0.28f)
        lineTo(w * 0.58f, h * 0.38f)
    }
    drawPath(slit, stroke, style = Stroke(strokeWidth * 1.1f, cap = StrokeCap.Round))

    // Pedestal
    val base = Path().apply {
        moveTo(w * 0.24f, h * 0.80f)
        lineTo(w * 0.76f, h * 0.80f)
        lineTo(w * 0.80f, h * 0.87f)
        lineTo(w * 0.20f, h * 0.87f)
        close()
    }
    drawPath(base, fill)
    drawPath(base, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
}

private fun DrawScope.drawRook(w: Float, h: Float, fill: Color, stroke: Color, strokeWidth: Float) {
    // Castle crenellations
    val rook = Path().apply {
        moveTo(w * 0.26f, h * 0.24f)
        lineTo(w * 0.36f, h * 0.24f)
        lineTo(w * 0.36f, h * 0.32f)
        lineTo(w * 0.44f, h * 0.32f)
        lineTo(w * 0.44f, h * 0.24f)
        lineTo(w * 0.56f, h * 0.24f)
        lineTo(w * 0.56f, h * 0.32f)
        lineTo(w * 0.64f, h * 0.32f)
        lineTo(w * 0.64f, h * 0.24f)
        lineTo(w * 0.74f, h * 0.24f)
        lineTo(w * 0.72f, h * 0.42f)
        lineTo(w * 0.66f, h * 0.45f)
        lineTo(w * 0.66f, h * 0.78f)
        lineTo(w * 0.74f, h * 0.82f)
        lineTo(w * 0.76f, h * 0.88f)
        lineTo(w * 0.24f, h * 0.88f)
        lineTo(w * 0.26f, h * 0.82f)
        lineTo(w * 0.34f, h * 0.78f)
        lineTo(w * 0.34f, h * 0.45f)
        lineTo(w * 0.28f, h * 0.42f)
        close()
    }
    drawPath(rook, fill)
    drawPath(rook, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
}

private fun DrawScope.drawQueen(w: Float, h: Float, fill: Color, stroke: Color, strokeWidth: Float) {
    // 5 Crown pearls
    val pearls = listOf(
        Offset(w * 0.22f, h * 0.25f),
        Offset(w * 0.36f, h * 0.20f),
        Offset(w * 0.50f, h * 0.17f),
        Offset(w * 0.64f, h * 0.20f),
        Offset(w * 0.78f, h * 0.25f)
    )
    for (p in pearls) {
        drawCircle(fill, radius = w * 0.04f, center = p)
        drawCircle(stroke, radius = w * 0.04f, center = p, style = Stroke(strokeWidth * 0.8f))
    }

    // Queen body
    val body = Path().apply {
        moveTo(w * 0.22f, h * 0.28f)
        lineTo(w * 0.32f, h * 0.42f)
        lineTo(w * 0.36f, h * 0.24f)
        lineTo(w * 0.44f, h * 0.42f)
        lineTo(w * 0.50f, h * 0.20f)
        lineTo(w * 0.56f, h * 0.42f)
        lineTo(w * 0.64f, h * 0.24f)
        lineTo(w * 0.68f, h * 0.42f)
        lineTo(w * 0.78f, h * 0.28f)
        cubicTo(w * 0.72f, h * 0.55f, w * 0.66f, h * 0.68f, w * 0.70f, h * 0.80f)
        lineTo(w * 0.30f, h * 0.80f)
        cubicTo(w * 0.34f, h * 0.68f, w * 0.28f, h * 0.55f, w * 0.22f, h * 0.28f)
        close()
    }
    drawPath(body, fill)
    drawPath(body, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

    // Pedestal
    val base = Path().apply {
        moveTo(w * 0.22f, h * 0.80f)
        lineTo(w * 0.78f, h * 0.80f)
        lineTo(w * 0.82f, h * 0.88f)
        lineTo(w * 0.18f, h * 0.88f)
        close()
    }
    drawPath(base, fill)
    drawPath(base, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
}

private fun DrawScope.drawKing(w: Float, h: Float, fill: Color, stroke: Color, strokeWidth: Float) {
    // Royal Cross
    val cross = Path().apply {
        // Vertical
        moveTo(w * 0.50f, h * 0.11f)
        lineTo(w * 0.50f, h * 0.23f)
        // Horizontal
        moveTo(w * 0.43f, h * 0.16f)
        lineTo(w * 0.57f, h * 0.16f)
    }
    drawPath(cross, stroke, style = Stroke(strokeWidth * 1.2f, cap = StrokeCap.Round))

    // Crown Dome
    val crown = Path().apply {
        moveTo(w * 0.30f, h * 0.32f)
        cubicTo(w * 0.36f, h * 0.23f, w * 0.64f, h * 0.23f, w * 0.70f, h * 0.32f)
        cubicTo(w * 0.64f, h * 0.52f, w * 0.68f, h * 0.68f, w * 0.72f, h * 0.80f)
        lineTo(w * 0.28f, h * 0.80f)
        cubicTo(w * 0.32f, h * 0.68f, w * 0.36f, h * 0.52f, w * 0.30f, h * 0.32f)
        close()
    }
    drawPath(crown, fill)
    drawPath(crown, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

    // Crown Band Ribbons
    val band = Path().apply {
        moveTo(w * 0.32f, h * 0.48f)
        cubicTo(w * 0.42f, h * 0.52f, w * 0.58f, h * 0.52f, w * 0.68f, h * 0.48f)
    }
    drawPath(band, stroke, style = Stroke(strokeWidth * 0.9f, cap = StrokeCap.Round))

    // Pedestal
    val base = Path().apply {
        moveTo(w * 0.20f, h * 0.80f)
        lineTo(w * 0.80f, h * 0.80f)
        lineTo(w * 0.84f, h * 0.88f)
        lineTo(w * 0.16f, h * 0.88f)
        close()
    }
    drawPath(base, fill)
    drawPath(base, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))
}
