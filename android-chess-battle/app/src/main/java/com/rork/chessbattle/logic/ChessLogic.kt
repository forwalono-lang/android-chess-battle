package com.rork.chessbattle.logic

import com.rork.chessbattle.model.*

typealias Board = Array<Array<Piece?>>

fun createInitialBoard(): Board {
    val board: Board = Array(8) { arrayOfNulls(8) }
    board[0][0] = Piece(PieceType.ROOK, PieceColor.BLACK)
    board[0][1] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
    board[0][2] = Piece(PieceType.BISHOP, PieceColor.BLACK)
    board[0][3] = Piece(PieceType.QUEEN, PieceColor.BLACK)
    board[0][4] = Piece(PieceType.KING, PieceColor.BLACK)
    board[0][5] = Piece(PieceType.BISHOP, PieceColor.BLACK)
    board[0][6] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
    board[0][7] = Piece(PieceType.ROOK, PieceColor.BLACK)
    for (col in 0..7) board[1][col] = Piece(PieceType.PAWN, PieceColor.BLACK)
    board[7][0] = Piece(PieceType.ROOK, PieceColor.WHITE)
    board[7][1] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
    board[7][2] = Piece(PieceType.BISHOP, PieceColor.WHITE)
    board[7][3] = Piece(PieceType.QUEEN, PieceColor.WHITE)
    board[7][4] = Piece(PieceType.KING, PieceColor.WHITE)
    board[7][5] = Piece(PieceType.BISHOP, PieceColor.WHITE)
    board[7][6] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
    board[7][7] = Piece(PieceType.ROOK, PieceColor.WHITE)
    for (col in 0..7) board[6][col] = Piece(PieceType.PAWN, PieceColor.WHITE)
    return board
}

fun copyBoard(board: Board): Board {
    return Array(8) { row -> Array(8) { col -> board[row][col] } }
}

fun deepCopyBoard(board: Board): Board {
    return Array(8) { row ->
        Array(8) { col ->
            board[row][col]?.copy()
        }
    }
}

/** All direction offsets used for sliding pieces and king moves */
private val DIAGONALS = listOf(
    Position(-1, -1), Position(-1, 1), Position(1, -1), Position(1, 1)
)
private val ORTHOGONALS = listOf(
    Position(-1, 0), Position(1, 0), Position(0, -1), Position(0, 1)
)
private val ALL_DIRECTIONS = DIAGONALS + ORTHOGONALS

private val KNIGHT_OFFSETS = listOf(
    Position(-2, -1), Position(-2, 1), Position(-1, -2), Position(-1, 2),
    Position(1, -2), Position(1, 2), Position(2, -1), Position(2, 1)
)

fun findKing(board: Board, color: PieceColor): Position? {
    for (row in 0..7) {
        for (col in 0..7) {
            val piece = board[row][col]
            if (piece?.type == PieceType.KING && piece.color == color) {
                return Position(row, col)
            }
        }
    }
    return null
}

/**
 * Generate all pseudolegal moves for a piece at [pos].
 * These follow piece movement rules but may leave own king in check.
 */
fun getPseudoLegalMoves(
    board: Board,
    pos: Position,
    lastMove: Move?,
    castlingRights: CastlingRights
): List<Move> {
    val piece = board[pos.row][pos.col] ?: return emptyList()
    return when (piece.type) {
        PieceType.PAWN -> getPawnMoves(board, pos, piece.color, lastMove)
        PieceType.KNIGHT -> getKnightMoves(board, pos, piece.color)
        PieceType.BISHOP -> getSlidingMoves(board, pos, piece.color, DIAGONALS)
        PieceType.ROOK -> getSlidingMoves(board, pos, piece.color, ORTHOGONALS)
        PieceType.QUEEN -> getSlidingMoves(board, pos, piece.color, ALL_DIRECTIONS)
        PieceType.KING -> getKingMoves(board, pos, piece.color, castlingRights)
    }
}

private fun getPawnMoves(
    board: Board, pos: Position, color: PieceColor, lastMove: Move?
): List<Move> {
    val moves = mutableListOf<Move>()
    val direction = if (color == PieceColor.WHITE) -1 else 1
    val startRow = if (color == PieceColor.WHITE) 6 else 1
    val promoRow = if (color == PieceColor.WHITE) 0 else 7

    // Single forward
    val oneForward = Position(pos.row + direction, pos.col)
    if (oneForward.isValid() && board[oneForward.row][oneForward.col] == null) {
        if (oneForward.row == promoRow) {
            // Promotion
            for (promoType in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                moves.add(Move(pos, oneForward, isCapture = false, promotionType = promoType))
            }
        } else {
            moves.add(Move(pos, oneForward))
        }
    }

    // Double forward from start
    if (pos.row == startRow) {
        val twoForward = Position(pos.row + 2 * direction, pos.col)
        val between = Position(pos.row + direction, pos.col)
        if (board[between.row][between.col] == null &&
            board[twoForward.row][twoForward.col] == null
        ) {
            moves.add(Move(pos, twoForward))
        }
    }

    // Diagonal captures
    for (dc in listOf(-1, 1)) {
        val capturePos = Position(pos.row + direction, pos.col + dc)
        if (capturePos.isValid()) {
            val target = board[capturePos.row][capturePos.col]
            if (target != null && target.color != color) {
                if (capturePos.row == promoRow) {
                    for (promoType in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                        moves.add(Move(pos, capturePos, isCapture = true, promotionType = promoType))
                    }
                } else {
                    moves.add(Move(pos, capturePos, isCapture = true))
                }
            }
            // En passant
            if (lastMove != null) {
                val lastPiece = board[lastMove.to.row][lastMove.to.col]
                if (lastPiece?.type == PieceType.PAWN &&
                    lastPiece.color != color &&
                    lastMove.to.row == pos.row &&
                    lastMove.to.col == pos.col + dc &&
                    lastMove.from.row == pos.row + 2 * direction
                ) {
                    moves.add(Move(pos, capturePos, isCapture = true, isEnPassant = true))
                }
            }
        }
    }

    return moves
}

private fun getKnightMoves(
    board: Board, pos: Position, color: PieceColor
): List<Move> {
    val moves = mutableListOf<Move>()
    for (offset in KNIGHT_OFFSETS) {
        val target = pos + offset
        if (!target.isValid()) continue
        val targetPiece = board[target.row][target.col]
        if (targetPiece == null) {
            moves.add(Move(pos, target))
        } else if (targetPiece.color != color) {
            moves.add(Move(pos, target, isCapture = true))
        }
    }
    return moves
}

private fun getSlidingMoves(
    board: Board, pos: Position, color: PieceColor, directions: List<Position>
): List<Move> {
    val moves = mutableListOf<Move>()
    for (dir in directions) {
        var current = pos + dir
        while (current.isValid()) {
            val target = board[current.row][current.col]
            if (target == null) {
                moves.add(Move(pos, current))
            } else {
                if (target.color != color) {
                    moves.add(Move(pos, current, isCapture = true))
                }
                break
            }
            current = current + dir
        }
    }
    return moves
}

private fun getKingMoves(
    board: Board,
    pos: Position,
    color: PieceColor,
    castlingRights: CastlingRights
): List<Move> {
    val moves = mutableListOf<Move>()

    // Normal one-step moves
    for (dir in ALL_DIRECTIONS) {
        val target = pos + dir
        if (!target.isValid()) continue
        val targetPiece = board[target.row][target.col]
        if (targetPiece == null) {
            moves.add(Move(pos, target))
        } else if (targetPiece.color != color) {
            moves.add(Move(pos, target, isCapture = true))
        }
    }

    // Castling
    val row = if (color == PieceColor.WHITE) 7 else 0
    val (canCastleK, canCastleQ) = if (color == PieceColor.WHITE) {
        castlingRights.whiteKingSide to castlingRights.whiteQueenSide
    } else {
        castlingRights.blackKingSide to castlingRights.blackQueenSide
    }

    fun isAttacked(targetRow: Int, targetCol: Int, byColor: PieceColor): Boolean {
        return isSquareAttackedBy(board, Position(targetRow, targetCol), byColor)
    }

    if (canCastleK &&
        board[row][5] == null && board[row][6] == null &&
        board[row][7]?.type == PieceType.ROOK && board[row][7]?.color == color &&
        !isAttacked(row, 4, color.opponent()) &&
        !isAttacked(row, 5, color.opponent()) &&
        !isAttacked(row, 6, color.opponent())
    ) {
        moves.add(Move(pos, Position(row, 6), isCastling = true))
    }

    if (canCastleQ &&
        board[row][1] == null && board[row][2] == null && board[row][3] == null &&
        board[row][0]?.type == PieceType.ROOK && board[row][0]?.color == color &&
        !isAttacked(row, 4, color.opponent()) &&
        !isAttacked(row, 3, color.opponent()) &&
        !isAttacked(row, 2, color.opponent())
    ) {
        moves.add(Move(pos, Position(row, 2), isCastling = true))
    }

    return moves
}

/**
 * Check if a square is attacked by any piece of [attackerColor].
 * Used to verify moves don't leave own king in check.
 */
fun isSquareAttackedBy(board: Board, pos: Position, attackerColor: PieceColor): Boolean {
    // Pawn attacks
    val pawnDir = if (attackerColor == PieceColor.WHITE) 1 else -1
    for (dc in listOf(-1, 1)) {
        val pawnPos = Position(pos.row + pawnDir, pos.col + dc)
        if (pawnPos.isValid()) {
            val p = board[pawnPos.row][pawnPos.col]
            if (p?.type == PieceType.PAWN && p.color == attackerColor) return true
        }
    }

    // Knight attacks
    for (off in KNIGHT_OFFSETS) {
        val knPos = pos + off
        if (knPos.isValid()) {
            val p = board[knPos.row][knPos.col]
            if (p?.type == PieceType.KNIGHT && p.color == attackerColor) return true
        }
    }

    // King attacks (adjacent squares)
    for (dir in ALL_DIRECTIONS) {
        val kp = pos + dir
        if (kp.isValid()) {
            val p = board[kp.row][kp.col]
            if (p?.type == PieceType.KING && p.color == attackerColor) return true
        }
    }

    // Sliding piece attacks (bishop/queen on diagonals, rook/queen on orthogonals)
    for (dir in DIAGONALS) {
        var cur = pos + dir
        while (cur.isValid()) {
            val p = board[cur.row][cur.col]
            if (p != null) {
                if (p.color == attackerColor &&
                    (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)
                ) return true
                break
            }
            cur = cur + dir
        }
    }

    for (dir in ORTHOGONALS) {
        var cur = pos + dir
        while (cur.isValid()) {
            val p = board[cur.row][cur.col]
            if (p != null) {
                if (p.color == attackerColor &&
                    (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)
                ) return true
                break
            }
            cur = cur + dir
        }
    }

    return false
}

/**
 * Check if [color]'s king is in check on the given board.
 */
fun isInCheck(board: Board, color: PieceColor): Boolean {
    val kingPos = findKing(board, color) ?: return false
    return isSquareAttackedBy(board, kingPos, color.opponent())
}

/**
 * Get legal moves for a piece — pseudolegal moves that don't leave own king in check.
 */
fun getLegalMoves(
    board: Board,
    pos: Position,
    lastMove: Move?,
    castlingRights: CastlingRights
): List<Move> {
    val piece = board[pos.row][pos.col] ?: return emptyList()
    val pseudoMoves = getPseudoLegalMoves(board, pos, lastMove, castlingRights)
    return pseudoMoves.filter { move ->
        !leavesKingInCheck(board, move, piece.color)
    }
}

/**
 * Apply a move to a board copy and check if the resulting position
 * leaves the moving side's king in check.
 */
private fun leavesKingInCheck(board: Board, move: Move, color: PieceColor): Boolean {
    val testBoard = deepCopyBoard(board)
    applyMoveToBoard(testBoard, move)
    return isInCheck(testBoard, color)
}

/**
 * Apply a move to the board in place. Does NOT validate legality.
 */
fun applyMoveToBoard(board: Board, move: Move) {
    val piece = board[move.from.row][move.from.col]

    // En passant capture: remove the captured pawn
    if (move.isEnPassant) {
        val capturedRow = move.from.row
        board[capturedRow][move.to.col] = null
    }

    // Castling: move the rook
    if (move.isCastling) {
        val row = move.from.row
        if (move.to.col == 6) {
            // Kingside
            board[row][5] = board[row][7]
            board[row][7] = null
        } else {
            // Queenside
            board[row][3] = board[row][0]
            board[row][0] = null
        }
    }

    // Place the piece (handles promotion)
    board[move.to.row][move.to.col] = if (move.promotionType != null && piece != null) {
        Piece(move.promotionType, piece.color)
    } else {
        piece
    }

    // Clear the source square
    board[move.from.row][move.from.col] = null
}

/**
 * Check if there are any legal moves for [color].
 */
fun hasAnyLegalMoves(
    board: Board,
    color: PieceColor,
    lastMove: Move?,
    castlingRights: CastlingRights
): Boolean {
    for (row in 0..7) {
        for (col in 0..7) {
            val piece = board[row][col]
            if (piece?.color == color) {
                if (getLegalMoves(board, Position(row, col), lastMove, castlingRights).isNotEmpty()) {
                    return true
                }
            }
        }
    }
    return false
}

/**
 * Get all legal moves for [color] across the entire board.
 */
fun getAllLegalMoves(
    board: Board,
    color: PieceColor,
    lastMove: Move?,
    castlingRights: CastlingRights
): List<Move> {
    val allMoves = mutableListOf<Move>()
    for (row in 0..7) {
        for (col in 0..7) {
            val piece = board[row][col]
            if (piece?.color == color) {
                allMoves.addAll(
                    getLegalMoves(board, Position(row, col), lastMove, castlingRights)
                )
            }
        }
    }
    return allMoves
}

/**
 * Castling rights tracking.
 */
data class CastlingRights(
    val whiteKingSide: Boolean = true,
    val whiteQueenSide: Boolean = true,
    val blackKingSide: Boolean = true,
    val blackQueenSide: Boolean = true
) {
    fun afterMove(move: Move, piece: Piece): CastlingRights {
        var wk = whiteKingSide; var wq = whiteQueenSide
        var bk = blackKingSide; var bq = blackQueenSide

        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) { wk = false; wq = false }
            else { bk = false; bq = false }
        }

        if (piece.type == PieceType.ROOK) {
            if (piece.color == PieceColor.WHITE) {
                if (move.from.row == 7 && move.from.col == 0) wq = false
                if (move.from.row == 7 && move.from.col == 7) wk = false
            } else {
                if (move.from.row == 0 && move.from.col == 0) bq = false
                if (move.from.row == 0 && move.from.col == 7) bk = false
            }
        }

        // If a rook is captured on its starting square
        val capturedPos = move.to
        if (capturedPos.row == 7 && capturedPos.col == 0) wq = false
        if (capturedPos.row == 7 && capturedPos.col == 7) wk = false
        if (capturedPos.row == 0 && capturedPos.col == 0) bq = false
        if (capturedPos.row == 0 && capturedPos.col == 7) bk = false

        return CastlingRights(wk, wq, bk, bq)
    }
}
