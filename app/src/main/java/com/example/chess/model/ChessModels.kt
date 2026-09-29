package com.example.chess.model

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val value: Int, val notationLetter: String, val displayName: String) {
    PAWN(1, "", "Pawn"),
    KNIGHT(3, "N", "Knight"),
    BISHOP(3, "B", "Bishop"),
    ROOK(5, "R", "Rook"),
    QUEEN(9, "Q", "Queen"),
    KING(1000, "K", "King")
}

data class Position(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..7 && col in 0..7

    fun toAlgebraic(): String {
        val file = ('a'.code + col).toChar()
        val rank = 8 - row
        return "$file$rank"
    }

    companion object {
        fun fromAlgebraic(algebraic: String): Position? {
            if (algebraic.length != 2) return null
            val fileChar = algebraic[0].lowercaseChar()
            val rankChar = algebraic[1]
            if (fileChar !in 'a'..'h' || rankChar !in '1'..'8') return null
            val col = fileChar.code - 'a'.code
            val row = 8 - (rankChar.code - '0'.code)
            return Position(row, col)
        }
    }
}

data class ChessPiece(
    val id: String,
    val type: PieceType,
    val color: PieceColor,
    val hasMoved: Boolean = false
)

data class Move(
    val from: Position,
    val to: Position,
    val piece: ChessPiece,
    val capturedPiece: ChessPiece? = null,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false,
    val isKingsideCastle: Boolean = false,
    val isQueensideCastle: Boolean = false,
    val promotionType: PieceType? = null,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val notation: String = ""
)

enum class GameStatus {
    ACTIVE,
    CHECK,
    CHECKMATE,
    STALEMATE,
    RESIGNATION,
    DRAW_AGREED,
    TIMEOUT
}

enum class GameMode(val title: String, val subtitle: String) {
    ONLINE_MULTIPLAYER("Online Match", "Real-time play & voice chat with guests/players"),
    VS_BOT("Practice vs Engine", "Sharpen tactics against adaptive bot levels"),
    PASS_AND_PLAY("Pass & Play", "Play face-to-face on a single device")
}

enum class BotDifficulty(val title: String, val rating: Int, val depth: Int) {
    CASUAL("Casual Novice", 800, 1),
    TACTICAL("Tactical Adept", 1500, 2),
    MASTER("Grandmaster AI", 2200, 3)
}

enum class BoardTheme(val displayName: String) {
    SLATE("Slate & Mist"),
    WALNUT("Walnut & Cream"),
    EMERALD("Emerald & Parchment"),
    MIDNIGHT("Midnight Obsidian")
}

data class PlayerInfo(
    val id: String,
    val name: String,
    val rating: Int = 1200,
    val avatarIndex: Int = 0,
    val isGuest: Boolean = true,
    val isSpeaking: Boolean = false,
    val isMicMuted: Boolean = false
)
