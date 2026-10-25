package com.example

import com.example.chess.engine.ChessBoard
import com.example.chess.engine.ChessEngine
import com.example.chess.model.GameStatus
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testInitialBoardSetup() {
        val board = ChessBoard.initial()
        assertEquals(PieceColor.WHITE, board.turn)
        assertEquals(GameStatus.ACTIVE, board.gameStatus)

        // Check White King at e1 (row 7, col 4)
        val whiteKing = board.pieceAt(Position(7, 4))
        assertNotNull(whiteKing)
        assertEquals(PieceType.KING, whiteKing?.type)
        assertEquals(PieceColor.WHITE, whiteKing?.color)

        // Check Black King at e8 (row 0, col 4)
        val blackKing = board.pieceAt(Position(0, 4))
        assertNotNull(blackKing)
        assertEquals(PieceType.KING, blackKing?.type)
        assertEquals(PieceColor.BLACK, blackKing?.color)
    }

    @Test
    fun testPawnInitialMoves() {
        val board = ChessBoard.initial()
        val e2 = Position(6, 4) // e2 pawn
        val legalMoves = ChessEngine.getLegalMoves(board, e2)

        // e2 pawn can move to e3 and e4
        assertTrue(Position(5, 4) in legalMoves)
        assertTrue(Position(4, 4) in legalMoves)
        assertEquals(2, legalMoves.size)
    }

    @Test
    fun testKnightInitialMoves() {
        val board = ChessBoard.initial()
        val g1 = Position(7, 6) // g1 knight
        val legalMoves = ChessEngine.getLegalMoves(board, g1)

        // g1 knight can jump to f3 and h3
        assertTrue(Position(5, 5) in legalMoves)
        assertTrue(Position(5, 7) in legalMoves)
        assertEquals(2, legalMoves.size)
    }

    @Test
    fun testMakeMoveUpdatesTurnAndHistory() {
        val board = ChessBoard.initial()
        val e2 = Position(6, 4)
        val e4 = Position(4, 4)

        val nextBoard = ChessEngine.makeMove(board, e2, e4)
        assertEquals(PieceColor.BLACK, nextBoard.turn)
        assertEquals(1, nextBoard.moveHistory.size)
        assertEquals("e4", nextBoard.moveHistory.first().notation)
    }

    @Test
    fun testTimeoutGameOverState() {
        val board = ChessBoard.initial()
        // Simulate White running out of time
        val timeoutBoard = board.copy(
            gameStatus = GameStatus.TIMEOUT,
            winner = PieceColor.BLACK
        )
        assertEquals(GameStatus.TIMEOUT, timeoutBoard.gameStatus)
        assertEquals(PieceColor.BLACK, timeoutBoard.winner)
    }
}
