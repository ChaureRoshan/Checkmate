package com.example.chess.engine

import com.example.chess.model.ChessPiece
import com.example.chess.model.GameStatus
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Position
import java.util.UUID

data class ChessBoard(
    val grid: Array<Array<ChessPiece?>> = Array(8) { arrayOfNulls(8) },
    val turn: PieceColor = PieceColor.WHITE,
    val lastMove: Move? = null,
    val enPassantTarget: Position? = null,
    val moveHistory: List<Move> = emptyList(),
    val capturedByWhite: List<ChessPiece> = emptyList(),
    val capturedByBlack: List<ChessPiece> = emptyList(),
    val gameStatus: GameStatus = GameStatus.ACTIVE,
    val winner: PieceColor? = null
) {
    fun pieceAt(pos: Position): ChessPiece? = if (pos.isValid()) grid[pos.row][pos.col] else null

    fun copyBoard(): ChessBoard {
        val newGrid = Array(8) { r ->
            Array(8) { c ->
                grid[r][c]
            }
        }
        return copy(grid = newGrid)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChessBoard) return false
        if (turn != other.turn) return false
        if (lastMove != other.lastMove) return false
        if (enPassantTarget != other.enPassantTarget) return false
        if (gameStatus != other.gameStatus) return false
        if (winner != other.winner) return false
        for (r in 0..7) {
            for (c in 0..7) {
                if (grid[r][c] != other.grid[r][c]) return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var result = turn.hashCode()
        result = 31 * result + (lastMove?.hashCode() ?: 0)
        result = 31 * result + (enPassantTarget?.hashCode() ?: 0)
        result = 31 * result + gameStatus.hashCode()
        return result
    }

    companion object {
        fun initial(): ChessBoard {
            val grid = Array(8) { arrayOfNulls<ChessPiece>(8) }

            // Black major pieces (Row 0)
            val blackBackRow = listOf(
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP,
                PieceType.QUEEN, PieceType.KING, PieceType.BISHOP,
                PieceType.KNIGHT, PieceType.ROOK
            )
            for (c in 0..7) {
                grid[0][c] = ChessPiece("b_${blackBackRow[c]}_$c", blackBackRow[c], PieceColor.BLACK)
                grid[1][c] = ChessPiece("b_pawn_$c", PieceType.PAWN, PieceColor.BLACK)
            }

            // White major pieces (Row 7)
            val whiteBackRow = listOf(
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP,
                PieceType.QUEEN, PieceType.KING, PieceType.BISHOP,
                PieceType.KNIGHT, PieceType.ROOK
            )
            for (c in 0..7) {
                grid[6][c] = ChessPiece("w_pawn_$c", PieceType.PAWN, PieceColor.WHITE)
                grid[7][c] = ChessPiece("w_${whiteBackRow[c]}_$c", whiteBackRow[c], PieceColor.WHITE)
            }

            return ChessBoard(grid = grid, turn = PieceColor.WHITE)
        }
    }
}

object ChessEngine {

    fun getLegalMoves(board: ChessBoard, from: Position): List<Position> {
        val piece = board.pieceAt(from) ?: return emptyList()
        if (piece.color != board.turn) return emptyList()

        val pseudoMoves = getPseudoLegalMoves(board, from, piece)
        val legalMoves = mutableListOf<Position>()

        for (target in pseudoMoves) {
            val simulated = simulateMove(board, from, target, piece, promotionType = PieceType.QUEEN)
            if (!isKingInCheck(simulated, piece.color)) {
                legalMoves.add(target)
            }
        }

        return legalMoves
    }

    fun getAllLegalMoves(board: ChessBoard, color: PieceColor): List<Pair<Position, Position>> {
        val moves = mutableListOf<Pair<Position, Position>>()
        for (r in 0..7) {
            for (c in 0..7) {
                val pos = Position(r, c)
                val piece = board.pieceAt(pos)
                if (piece != null && piece.color == color) {
                    val targets = getLegalMoves(board, pos)
                    for (target in targets) {
                        moves.add(pos to target)
                    }
                }
            }
        }
        return moves
    }

    fun isSquareAttacked(board: ChessBoard, pos: Position, byColor: PieceColor): Boolean {
        // 1. Attacked by Pawns
        val pawnPawnRow = if (byColor == PieceColor.WHITE) pos.row + 1 else pos.row - 1
        for (dc in listOf(-1, 1)) {
            val checkPos = Position(pawnPawnRow, pos.col + dc)
            val p = board.pieceAt(checkPos)
            if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
        }

        // 2. Attacked by Knights
        val knightOffsets = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        for ((dr, dc) in knightOffsets) {
            val checkPos = Position(pos.row + dr, pos.col + dc)
            val p = board.pieceAt(checkPos)
            if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
        }

        // 3. Attacked by King
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val checkPos = Position(pos.row + dr, pos.col + dc)
                val p = board.pieceAt(checkPos)
                if (p != null && p.color == byColor && p.type == PieceType.KING) return true
            }
        }

        // 4. Attacked along Orthogonals (Rook / Queen)
        val orthogonalDirections = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in orthogonalDirections) {
            var curr = Position(pos.row + dr, pos.col + dc)
            while (curr.isValid()) {
                val p = board.pieceAt(curr)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                curr = Position(curr.row + dr, curr.col + dc)
            }
        }

        // 5. Attacked along Diagonals (Bishop / Queen)
        val diagonalDirections = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for ((dr, dc) in diagonalDirections) {
            var curr = Position(pos.row + dr, pos.col + dc)
            while (curr.isValid()) {
                val p = board.pieceAt(curr)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                curr = Position(curr.row + dr, curr.col + dc)
            }
        }

        return false
    }

    fun findKing(board: ChessBoard, color: PieceColor): Position? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board.grid[r][c]
                if (p != null && p.color == color && p.type == PieceType.KING) {
                    return Position(r, c)
                }
            }
        }
        return null
    }

    fun isKingInCheck(board: ChessBoard, color: PieceColor): Boolean {
        val kingPos = findKing(board, color) ?: return false
        return isSquareAttacked(board, kingPos, color.opposite())
    }

    private fun getPseudoLegalMoves(board: ChessBoard, from: Position, piece: ChessPiece): List<Position> {
        val moves = mutableListOf<Position>()

        when (piece.type) {
            PieceType.PAWN -> {
                val direction = if (piece.color == PieceColor.WHITE) -1 else 1
                val startRow = if (piece.color == PieceColor.WHITE) 6 else 1

                // 1 step forward
                val oneStep = Position(from.row + direction, from.col)
                if (oneStep.isValid() && board.pieceAt(oneStep) == null) {
                    moves.add(oneStep)

                    // 2 steps forward from initial rank
                    if (from.row == startRow) {
                        val twoSteps = Position(from.row + 2 * direction, from.col)
                        if (board.pieceAt(twoSteps) == null) {
                            moves.add(twoSteps)
                        }
                    }
                }

                // Regular and En Passant captures
                for (dc in listOf(-1, 1)) {
                    val capturePos = Position(from.row + direction, from.col + dc)
                    if (capturePos.isValid()) {
                        val targetPiece = board.pieceAt(capturePos)
                        if (targetPiece != null && targetPiece.color != piece.color) {
                            moves.add(capturePos)
                        } else if (board.enPassantTarget == capturePos) {
                            moves.add(capturePos)
                        }
                    }
                }
            }

            PieceType.KNIGHT -> {
                val offsets = listOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1
                )
                for ((dr, dc) in offsets) {
                    val target = Position(from.row + dr, from.col + dc)
                    if (target.isValid()) {
                        val destPiece = board.pieceAt(target)
                        if (destPiece == null || destPiece.color != piece.color) {
                            moves.add(target)
                        }
                    }
                }
            }

            PieceType.BISHOP -> {
                moves.addAll(getRayMoves(board, from, piece.color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)))
            }

            PieceType.ROOK -> {
                moves.addAll(getRayMoves(board, from, piece.color, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)))
            }

            PieceType.QUEEN -> {
                moves.addAll(
                    getRayMoves(
                        board, from, piece.color,
                        listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1)
                    )
                )
            }

            PieceType.KING -> {
                // 1 step in all directions
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val target = Position(from.row + dr, from.col + dc)
                        if (target.isValid()) {
                            val destPiece = board.pieceAt(target)
                            if (destPiece == null || destPiece.color != piece.color) {
                                moves.add(target)
                            }
                        }
                    }
                }

                // Castling
                if (!piece.hasMoved && !isKingInCheck(board, piece.color)) {
                    val row = if (piece.color == PieceColor.WHITE) 7 else 0
                    if (from.row == row && from.col == 4) {
                        // Kingside
                        val rookKingside = board.pieceAt(Position(row, 7))
                        if (rookKingside != null && rookKingside.type == PieceType.ROOK && !rookKingside.hasMoved) {
                            if (board.pieceAt(Position(row, 5)) == null && board.pieceAt(Position(row, 6)) == null) {
                                if (!isSquareAttacked(board, Position(row, 5), piece.color.opposite()) &&
                                    !isSquareAttacked(board, Position(row, 6), piece.color.opposite())
                                ) {
                                    moves.add(Position(row, 6))
                                }
                            }
                        }

                        // Queenside
                        val rookQueenside = board.pieceAt(Position(row, 0))
                        if (rookQueenside != null && rookQueenside.type == PieceType.ROOK && !rookQueenside.hasMoved) {
                            if (board.pieceAt(Position(row, 1)) == null &&
                                board.pieceAt(Position(row, 2)) == null &&
                                board.pieceAt(Position(row, 3)) == null
                            ) {
                                if (!isSquareAttacked(board, Position(row, 3), piece.color.opposite()) &&
                                    !isSquareAttacked(board, Position(row, 2), piece.color.opposite())
                                ) {
                                    moves.add(Position(row, 2))
                                }
                            }
                        }
                    }
                }
            }
        }

        return moves
    }

    private fun getRayMoves(board: ChessBoard, from: Position, color: PieceColor, directions: List<Pair<Int, Int>>): List<Position> {
        val result = mutableListOf<Position>()
        for ((dr, dc) in directions) {
            var curr = Position(from.row + dr, from.col + dc)
            while (curr.isValid()) {
                val targetPiece = board.pieceAt(curr)
                if (targetPiece == null) {
                    result.add(curr)
                } else {
                    if (targetPiece.color != color) {
                        result.add(curr)
                    }
                    break
                }
                curr = Position(curr.row + dr, curr.col + dc)
            }
        }
        return result
    }

    private fun simulateMove(
        board: ChessBoard,
        from: Position,
        to: Position,
        piece: ChessPiece,
        promotionType: PieceType? = null
    ): ChessBoard {
        val newGrid = Array(8) { r -> Array(8) { c -> board.grid[r][c] } }

        var captured = newGrid[to.row][to.col]
        val isEnPassant = piece.type == PieceType.PAWN && board.enPassantTarget == to

        if (isEnPassant) {
            val capturedPawnRow = from.row
            val capturedPawnCol = to.col
            captured = newGrid[capturedPawnRow][capturedPawnCol]
            newGrid[capturedPawnRow][capturedPawnCol] = null
        }

        // Move piece
        newGrid[from.row][from.col] = null

        val finalPiece = if (piece.type == PieceType.PAWN && (to.row == 0 || to.row == 7)) {
            val promotedType = promotionType ?: PieceType.QUEEN
            ChessPiece(piece.id, promotedType, piece.color, hasMoved = true)
        } else {
            piece.copy(hasMoved = true)
        }
        newGrid[to.row][to.col] = finalPiece

        // Castling rook movement
        if (piece.type == PieceType.KING && kotlin.math.abs(to.col - from.col) == 2) {
            val row = from.row
            if (to.col == 6) { // Kingside
                val rook = newGrid[row][7]
                newGrid[row][7] = null
                newGrid[row][5] = rook?.copy(hasMoved = true)
            } else if (to.col == 2) { // Queenside
                val rook = newGrid[row][0]
                newGrid[row][0] = null
                newGrid[row][3] = rook?.copy(hasMoved = true)
            }
        }

        return board.copy(grid = newGrid)
    }

    fun makeMove(
        board: ChessBoard,
        from: Position,
        to: Position,
        promotionType: PieceType? = null
    ): ChessBoard {
        val piece = board.pieceAt(from) ?: return board
        val legalMoves = getLegalMoves(board, from)
        if (to !in legalMoves) return board

        val newGrid = Array(8) { r -> Array(8) { c -> board.grid[r][c] } }

        var capturedPiece = newGrid[to.row][to.col]
        val isEnPassant = piece.type == PieceType.PAWN && board.enPassantTarget == to

        if (isEnPassant) {
            val capturedPawnRow = from.row
            val capturedPawnCol = to.col
            capturedPiece = newGrid[capturedPawnRow][capturedPawnCol]
            newGrid[capturedPawnRow][capturedPawnCol] = null
        }

        newGrid[from.row][from.col] = null

        // En passant target calculation for next turn
        val newEnPassantTarget = if (piece.type == PieceType.PAWN && kotlin.math.abs(to.row - from.row) == 2) {
            Position((from.row + to.row) / 2, from.col)
        } else null

        val isCastling = piece.type == PieceType.KING && kotlin.math.abs(to.col - from.col) == 2
        val isKingsideCastle = isCastling && to.col == 6
        val isQueensideCastle = isCastling && to.col == 2

        // Move castling rook
        if (isCastling) {
            val row = from.row
            if (isKingsideCastle) {
                val rook = newGrid[row][7]
                newGrid[row][7] = null
                newGrid[row][5] = rook?.copy(hasMoved = true)
            } else if (isQueensideCastle) {
                val rook = newGrid[row][0]
                newGrid[row][0] = null
                newGrid[row][3] = rook?.copy(hasMoved = true)
            }
        }

        // Promotion handling
        val isPromotion = piece.type == PieceType.PAWN && (to.row == 0 || to.row == 7)
        val finalPromotionType = if (isPromotion) (promotionType ?: PieceType.QUEEN) else null

        val finalPiece = if (isPromotion) {
            ChessPiece(piece.id, finalPromotionType!!, piece.color, hasMoved = true)
        } else {
            piece.copy(hasMoved = true)
        }
        newGrid[to.row][to.col] = finalPiece

        val nextTurn = board.turn.opposite()

        // Check if next player is in check or checkmate
        val tempBoard = board.copy(
            grid = newGrid,
            turn = nextTurn,
            enPassantTarget = newEnPassantTarget
        )

        val nextPlayerInCheck = isKingInCheck(tempBoard, nextTurn)
        val nextPlayerLegalMoves = getAllLegalMoves(tempBoard, nextTurn)

        val isCheckmate = nextPlayerInCheck && nextPlayerLegalMoves.isEmpty()
        val isStalemate = !nextPlayerInCheck && nextPlayerLegalMoves.isEmpty()

        val notation = buildNotation(
            from = from,
            to = to,
            piece = piece,
            captured = capturedPiece != null,
            isCastling = isCastling,
            isKingsideCastle = isKingsideCastle,
            isPromotion = isPromotion,
            promotionType = finalPromotionType,
            isCheck = nextPlayerInCheck,
            isCheckmate = isCheckmate
        )

        val move = Move(
            from = from,
            to = to,
            piece = piece,
            capturedPiece = capturedPiece,
            isEnPassant = isEnPassant,
            isCastling = isCastling,
            isKingsideCastle = isKingsideCastle,
            isQueensideCastle = isQueensideCastle,
            promotionType = finalPromotionType,
            isCheck = nextPlayerInCheck,
            isCheckmate = isCheckmate,
            notation = notation
        )

        val newCapturedByWhite = board.capturedByWhite.toMutableList()
        val newCapturedByBlack = board.capturedByBlack.toMutableList()
        if (capturedPiece != null) {
            if (piece.color == PieceColor.WHITE) {
                newCapturedByWhite.add(capturedPiece)
            } else {
                newCapturedByBlack.add(capturedPiece)
            }
        }

        val gameStatus = when {
            isCheckmate -> GameStatus.CHECKMATE
            isStalemate -> GameStatus.STALEMATE
            nextPlayerInCheck -> GameStatus.CHECK
            isInsufficientMaterial(tempBoard) -> GameStatus.DRAW_AGREED
            else -> GameStatus.ACTIVE
        }

        val winner = if (isCheckmate) piece.color else null

        return ChessBoard(
            grid = newGrid,
            turn = nextTurn,
            lastMove = move,
            enPassantTarget = newEnPassantTarget,
            moveHistory = board.moveHistory + move,
            capturedByWhite = newCapturedByWhite,
            capturedByBlack = newCapturedByBlack,
            gameStatus = gameStatus,
            winner = winner
        )
    }

    private fun buildNotation(
        from: Position,
        to: Position,
        piece: ChessPiece,
        captured: Boolean,
        isCastling: Boolean,
        isKingsideCastle: Boolean,
        isPromotion: Boolean,
        promotionType: PieceType?,
        isCheck: Boolean,
        isCheckmate: Boolean
    ): String {
        if (isCastling) {
            val base = if (isKingsideCastle) "O-O" else "O-O-O"
            return if (isCheckmate) "$base#" else if (isCheck) "$base+" else base
        }

        val sb = StringBuilder()
        if (piece.type == PieceType.PAWN) {
            if (captured) {
                sb.append(('a'.code + from.col).toChar())
                sb.append('x')
            }
            sb.append(to.toAlgebraic())
            if (isPromotion && promotionType != null) {
                sb.append('=').append(promotionType.notationLetter)
            }
        } else {
            sb.append(piece.type.notationLetter)
            if (captured) sb.append('x')
            sb.append(to.toAlgebraic())
        }

        if (isCheckmate) {
            sb.append('#')
        } else if (isCheck) {
            sb.append('+')
        }

        return sb.toString()
    }

    fun isInsufficientMaterial(board: ChessBoard): Boolean {
        val pieces = mutableListOf<ChessPiece>()
        for (r in 0..7) {
            for (c in 0..7) {
                board.grid[r][c]?.let { pieces.add(it) }
            }
        }

        // King vs King
        if (pieces.size == 2) return true

        // King + Knight vs King OR King + Bishop vs King
        if (pieces.size == 3) {
            return pieces.any { it.type == PieceType.KNIGHT || it.type == PieceType.BISHOP }
        }

        return false
    }
}
