package com.example.chess.engine

import com.example.chess.model.BotDifficulty
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object ChessAi {

    // Piece-Square table for Pawns (White perspective, invert for Black)
    private val pawnTable = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    )

    // Piece-Square table for Knights
    private val knightTable = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    // Piece-Square table for Bishops
    private val bishopTable = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    suspend fun getBestMove(
        board: ChessBoard,
        color: PieceColor,
        difficulty: BotDifficulty
    ): Pair<Position, Position>? = withContext(Dispatchers.Default) {
        val allMoves = ChessEngine.getAllLegalMoves(board, color)
        if (allMoves.isEmpty()) return@withContext null

        if (difficulty == BotDifficulty.CASUAL) {
            // Casual: 60% best immediate capture or tactical check, 40% random sensible move
            if (Random.nextFloat() < 0.4f) {
                return@withContext allMoves.random()
            }
        }

        var bestMove: Pair<Position, Position>? = null
        val isMaximizing = color == PieceColor.WHITE
        var bestScore = if (isMaximizing) Int.MIN_VALUE else Int.MAX_VALUE

        // Shuffle moves to add opening variation
        val shuffledMoves = allMoves.shuffled()

        val searchDepth = difficulty.depth

        for (move in shuffledMoves) {
            val (from, to) = move
            val nextBoard = ChessEngine.makeMove(board, from, to)

            val score = alphaBeta(
                board = nextBoard,
                depth = searchDepth - 1,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE,
                isMaximizing = !isMaximizing
            )

            if (isMaximizing) {
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMove = move
                }
            }
        }

        bestMove ?: allMoves.random()
    }

    private fun alphaBeta(
        board: ChessBoard,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean
    ): Int {
        if (depth <= 0) {
            return evaluateBoard(board)
        }

        val currentColor = if (isMaximizing) PieceColor.WHITE else PieceColor.BLACK
        val legalMoves = ChessEngine.getAllLegalMoves(board, currentColor)

        if (legalMoves.isEmpty()) {
            return if (ChessEngine.isKingInCheck(board, currentColor)) {
                if (isMaximizing) -100000 + (3 - depth) else 100000 - (3 - depth)
            } else {
                0 // Stalemate
            }
        }

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for ((from, to) in legalMoves) {
                val nextBoard = ChessEngine.makeMove(board, from, to)
                val evaluation = alphaBeta(nextBoard, depth - 1, currentAlpha, currentBeta, false)
                maxEval = maxOf(maxEval, evaluation)
                currentAlpha = maxOf(currentAlpha, evaluation)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for ((from, to) in legalMoves) {
                val nextBoard = ChessEngine.makeMove(board, from, to)
                val evaluation = alphaBeta(nextBoard, depth - 1, currentAlpha, currentBeta, true)
                minEval = minOf(minEval, evaluation)
                currentBeta = minOf(currentBeta, evaluation)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }

    private fun evaluateBoard(board: ChessBoard): Int {
        var score = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board.grid[r][c] ?: continue
                val baseValue = when (piece.type) {
                    PieceType.PAWN -> 100
                    PieceType.KNIGHT -> 320
                    PieceType.BISHOP -> 330
                    PieceType.ROOK -> 500
                    PieceType.QUEEN -> 900
                    PieceType.KING -> 20000
                }

                // Positional bonus
                val tableIndex = if (piece.color == PieceColor.WHITE) r * 8 + c else (7 - r) * 8 + c
                val positionBonus = when (piece.type) {
                    PieceType.PAWN -> pawnTable[tableIndex]
                    PieceType.KNIGHT -> knightTable[tableIndex]
                    PieceType.BISHOP -> bishopTable[tableIndex]
                    else -> 0
                }

                val totalValue = baseValue + positionBonus

                if (piece.color == PieceColor.WHITE) {
                    score += totalValue
                } else {
                    score -= totalValue
                }
            }
        }

        return score
    }
}
