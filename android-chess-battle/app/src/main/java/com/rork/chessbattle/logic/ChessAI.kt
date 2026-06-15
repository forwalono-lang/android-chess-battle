package com.rork.chessbattle.logic

import com.rork.chessbattle.model.*
import kotlin.math.max
import kotlin.math.min

/**
 * Minimax chess AI with alpha-beta pruning.
 * Difficulty level 1-10 maps to search depth 1-6.
 */
class ChessAI(private val difficulty: Int) {

    /** Search depth = difficulty / 2 + 1, capped at 6 for performance. */
    private val depth: Int = min(6, max(1, difficulty / 2 + 1))

    /** Piece values for positional evaluation. */
    private val pieceValues = mapOf(
        PieceType.PAWN to 100,
        PieceType.KNIGHT to 320,
        PieceType.BISHOP to 330,
        PieceType.ROOK to 500,
        PieceType.QUEEN to 900,
        PieceType.KING to 20000
    )

    /** Pawn position bonus for white (flip for black). */
    private val pawnTable = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf(5, 5, 10, 25, 25, 10, 5, 5),
        intArrayOf(0, 0, 0, 20, 20, 0, 0, 0),
        intArrayOf(5, -5, -10, 0, 0, -10, -5, 5),
        intArrayOf(5, 10, 10, -20, -20, 10, 10, 5),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    )

    /** Knight position bonus. */
    private val knightTable = arrayOf(
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50),
        intArrayOf(-40, -20, 0, 0, 0, 0, -20, -40),
        intArrayOf(-30, 0, 10, 15, 15, 10, 0, -30),
        intArrayOf(-30, 5, 15, 20, 20, 15, 5, -30),
        intArrayOf(-30, 0, 15, 20, 20, 15, 0, -30),
        intArrayOf(-30, 5, 10, 15, 15, 10, 5, -30),
        intArrayOf(-40, -20, 0, 5, 5, 0, -20, -40),
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50)
    )

    /**
     * Returns the best move for the given color.
     */
    fun getBestMove(
        board: Board,
        color: PieceColor,
        castlingRights: CastlingRights,
        lastMove: Move?
    ): Move? {
        val allMoves = getAllLegalMoves(board, color, lastMove, castlingRights)
        if (allMoves.isEmpty()) return null

        // Shuffle to avoid repetitive play
        val shuffledMoves = allMoves.shuffled()

        var bestMove = shuffledMoves.first()
        var bestValue = Int.MIN_VALUE

        for (move in shuffledMoves) {
            val testBoard = deepCopyBoard(board)
            applyMoveToBoard(testBoard, move)
            val newRights = castlingRights.afterMove(move, board[move.from.row][move.from.col]!!)

            val value = minimax(
                testBoard,
                depth - 1,
                Int.MIN_VALUE,
                Int.MAX_VALUE,
                false,
                color.opponent(),
                newRights,
                move
            )
            if (value > bestValue) {
                bestValue = value
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minimax(
        board: Board,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        color: PieceColor,
        castlingRights: CastlingRights,
        lastMove: Move?
    ): Int {
        if (depth == 0) {
            return evaluate(board, color.opponent()) // evaluate from the perspective of the player whose turn it used to be
        }

        val moves = getAllLegalMoves(board, color, lastMove, castlingRights)
        if (moves.isEmpty()) {
            // Checkmate or stalemate
            if (isInCheck(board, color)) {
                return if (isMaximizing) -100000 + (depth * 100) else 100000 - (depth * 100)
            }
            return 0 // Stalemate
        }

        var a = alpha
        var b = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in moves) {
                val testBoard = deepCopyBoard(board)
                applyMoveToBoard(testBoard, move)
                val piece = board[move.from.row][move.from.col]!!
                val newRights = castlingRights.afterMove(move, piece)
                val eval = minimax(testBoard, depth - 1, a, b, false, color.opponent(), newRights, move)
                maxEval = max(maxEval, eval)
                a = max(a, eval)
                if (b <= a) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in moves) {
                val testBoard = deepCopyBoard(board)
                applyMoveToBoard(testBoard, move)
                val piece = board[move.from.row][move.from.col]!!
                val newRights = castlingRights.afterMove(move, piece)
                val eval = minimax(testBoard, depth - 1, a, b, true, color.opponent(), newRights, move)
                minEval = min(minEval, eval)
                b = min(b, eval)
                if (b <= a) break
            }
            return minEval
        }
    }

    /**
     * Evaluate the board from [perspectiveColor]'s point of view.
     * Positive = good for perspectiveColor.
     */
    private fun evaluate(board: Board, perspectiveColor: PieceColor): Int {
        var score = 0
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col] ?: continue
                val baseValue = pieceValues[piece.type] ?: 0
                val positionBonus = getPositionBonus(piece, row, col)

                val pieceScore = baseValue + positionBonus
                score += if (piece.color == perspectiveColor) pieceScore else -pieceScore
            }
        }
        return score
    }

    private fun getPositionBonus(piece: Piece, row: Int, col: Int): Int {
        return when (piece.type) {
            PieceType.PAWN -> {
                val r = if (piece.color == PieceColor.WHITE) row else 7 - row
                pawnTable[r][col]
            }
            PieceType.KNIGHT -> {
                val r = if (piece.color == PieceColor.WHITE) row else 7 - row
                knightTable[r][col]
            }
            else -> 0
        }
    }
}
