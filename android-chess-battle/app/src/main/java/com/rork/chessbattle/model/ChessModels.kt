package com.rork.chessbattle.model

enum class PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }

enum class PieceColor {
    WHITE, BLACK;

    fun opponent(): PieceColor = when (this) {
        WHITE -> BLACK
        BLACK -> WHITE
    }

    fun displayName(): String = when (this) {
        WHITE -> "Mr Whis"
        BLACK -> "Oliver Vert"
    }
}

data class Piece(val type: PieceType, val color: PieceColor) {
    /**
     * Use filled Unicode chess symbols for BOTH colors, so white pieces
     * appear solid (not hollow outlines). We color them via the UI.
     */
    fun unicode(): String = when (type) {
        PieceType.KING -> "\u265A"
        PieceType.QUEEN -> "\u265B"
        PieceType.ROOK -> "\u265C"
        PieceType.BISHOP -> "\u265D"
        PieceType.KNIGHT -> "\u265E"
        PieceType.PAWN -> "\u265F"
    }
}

data class Position(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..7 && col in 0..7

    operator fun plus(other: Position): Position =
        Position(row + other.row, col + other.col)
}

data class Move(
    val from: Position,
    val to: Position,
    val isCapture: Boolean = false,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false,
    val promotionType: PieceType? = null
)

enum class GameMode {
    PLAYER_VS_PLAYER,
    PLAYER_VS_AI
}
