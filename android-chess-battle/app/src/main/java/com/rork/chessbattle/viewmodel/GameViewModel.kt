package com.rork.chessbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rork.chessbattle.logic.*
import com.rork.chessbattle.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CaptureEffect(
    val position: Position,
    val piece: Piece,
    val id: Long = System.nanoTime()
)

data class GameUiState(
    val board: Board = createInitialBoard(),
    val currentTurn: PieceColor = PieceColor.WHITE,
    val selectedPosition: Position? = null,
    val legalMoves: List<Move> = emptyList(),
    val capturedWhite: List<Piece> = emptyList(),
    val capturedBlack: List<Piece> = emptyList(),
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val gameOver: Boolean = false,
    val statusMessage: String = "Mr Whis' turn",
    val lastMove: Move? = null,
    val lastMoveFrom: Position? = null,
    val lastMoveTo: Position? = null,
    val castlingRights: CastlingRights = CastlingRights(),
    // New fields
    val gameMode: GameMode = GameMode.PLAYER_VS_PLAYER,
    val aiDifficulty: Int = 5,
    val captureEffect: CaptureEffect? = null,
    val showCaptureAnim: Boolean = false,
    val isAiThinking: Boolean = false,
    val showVsIntro: Boolean = false,
    val vsIntroCompleted: Boolean = false,
    val welcomeShown: Boolean = false
)

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var ai: ChessAI? = null

    fun dismissWelcome() {
        _uiState.update { it.copy(welcomeShown = true) }
    }

    fun startGame(mode: GameMode, difficulty: Int = 5) {
        ai = if (mode == GameMode.PLAYER_VS_AI) ChessAI(difficulty) else null
        _uiState.update {
            GameUiState(
                gameMode = mode,
                aiDifficulty = difficulty,
                showVsIntro = true,
                welcomeShown = true,
                vsIntroCompleted = false
            )
        }
    }

    fun onVsIntroFinished() {
        _uiState.update { it.copy(showVsIntro = false, vsIntroCompleted = true) }
    }

    fun onSquareTapped(pos: Position) {
        val state = _uiState.value

        if (state.gameOver || state.showCaptureAnim || state.isAiThinking) return

        // In AI mode, only allow tapping for the human (White) turn
        if (state.gameMode == GameMode.PLAYER_VS_AI && state.currentTurn == PieceColor.BLACK) return

        val clickedPiece = state.board[pos.row][pos.col]

        // If a piece is already selected and the tapped square is a legal move target
        val matchingMove = state.legalMoves.find { it.to == pos }
        if (state.selectedPosition != null && matchingMove != null) {
            executeMove(matchingMove)
            return
        }

        // If the tapped square has a piece of the current player's color, select it
        if (clickedPiece != null && clickedPiece.color == state.currentTurn) {
            val moves = getLegalMoves(
                state.board, pos, state.lastMove, state.castlingRights
            )
            _uiState.update {
                it.copy(
                    selectedPosition = pos,
                    legalMoves = moves
                )
            }
            return
        }

        // Otherwise, clear selection
        _uiState.update {
            it.copy(selectedPosition = null, legalMoves = emptyList())
        }
    }

    private fun executeMove(move: Move) {
        val state = _uiState.value
        val piece = state.board[move.from.row][move.from.col] ?: return

        // Capture tracking
        val capturedPiece = if (move.isEnPassant) {
            state.board[move.from.row][move.to.col]
        } else if (move.isCastling) {
            null
        } else {
            state.board[move.to.row][move.to.col]
        }

        val newCapturedWhite = if (capturedPiece?.color == PieceColor.WHITE) {
            state.capturedWhite + capturedPiece
        } else state.capturedWhite

        val newCapturedBlack = if (capturedPiece?.color == PieceColor.BLACK) {
            state.capturedBlack + capturedPiece
        } else state.capturedBlack

        // If there's a capture, show animation before finalizing
        if (capturedPiece != null) {
            val newBoard = deepCopyBoard(state.board)
            applyMoveToBoard(newBoard, move)

            _uiState.update {
                it.copy(
                    captureEffect = CaptureEffect(move.to, capturedPiece),
                    showCaptureAnim = true,
                    selectedPosition = null,
                    legalMoves = emptyList()
                )
            }

            viewModelScope.launch {
                delay(500) // Animation duration
                finalizeMove(
                    capturedPiece = capturedPiece,
                    newCapturedWhite = newCapturedWhite,
                    newCapturedBlack = newCapturedBlack,
                    move = move,
                    piece = piece
                )
            }
            return
        }

        // No capture — finalize immediately
        finalizeMove(
            capturedPiece = null,
            newCapturedWhite = newCapturedWhite,
            newCapturedBlack = newCapturedBlack,
            move = move,
            piece = piece
        )
    }

    private fun finalizeMove(
        capturedPiece: Piece?,
        newCapturedWhite: List<Piece>,
        newCapturedBlack: List<Piece>,
        move: Move,
        piece: Piece
    ) {
        val state = _uiState.value

        val newBoard = deepCopyBoard(state.board)
        applyMoveToBoard(newBoard, move)

        val newCastlingRights = state.castlingRights.afterMove(move, piece)
        val nextTurn = state.currentTurn.opponent()

        val inCheck = isInCheck(newBoard, nextTurn)
        val hasMoves = hasAnyLegalMoves(newBoard, nextTurn, move, newCastlingRights)
        val isCheckmate = inCheck && !hasMoves
        val isStalemate = !inCheck && !hasMoves
        val gameOver = isCheckmate || isStalemate

        val statusMessage = when {
            isCheckmate -> "${state.currentTurn.displayName()} wins by checkmate!"
            isStalemate -> "Stalemate — draw!"
            inCheck -> "${nextTurn.displayName()} is in check"
            else -> "${nextTurn.displayName()}'s turn"
        }

        _uiState.update {
            it.copy(
                board = newBoard,
                currentTurn = nextTurn,
                selectedPosition = null,
                legalMoves = emptyList(),
                capturedWhite = newCapturedWhite,
                capturedBlack = newCapturedBlack,
                isCheck = inCheck,
                isCheckmate = isCheckmate,
                isStalemate = isStalemate,
                gameOver = gameOver,
                statusMessage = statusMessage,
                lastMove = move,
                lastMoveFrom = move.from,
                lastMoveTo = move.to,
                castlingRights = newCastlingRights,
                captureEffect = null,
                showCaptureAnim = false
            )
        }

        // Trigger AI move if it's AI's turn
        if (!gameOver && state.gameMode == GameMode.PLAYER_VS_AI && nextTurn == PieceColor.BLACK) {
            triggerAiMove()
        }
    }

    private fun triggerAiMove() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiThinking = true) }
            delay(300) // Small thinking delay for realism

            val state = _uiState.value
            val aiInstance = ai ?: return@launch

            val bestMove = aiInstance.getBestMove(
                state.board,
                PieceColor.BLACK,
                state.castlingRights,
                state.lastMove
            )

            _uiState.update { it.copy(isAiThinking = false) }

            if (bestMove != null) {
                executeMove(bestMove)
            }
        }
    }

    fun resetGame() {
        val state = _uiState.value
        ai = if (state.gameMode == GameMode.PLAYER_VS_AI) ChessAI(state.aiDifficulty) else null
        _uiState.update {
            GameUiState(
                gameMode = state.gameMode,
                aiDifficulty = state.aiDifficulty,
                welcomeShown = true,
                vsIntroCompleted = true
            )
        }
    }
}
