package com.rork.chessbattle.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.rork.chessbattle.model.Piece
import com.rork.chessbattle.model.PieceColor
import com.rork.chessbattle.model.PieceType
import com.rork.chessbattle.model.Position
import com.rork.chessbattle.viewmodel.CaptureEffect
import com.rork.chessbattle.viewmodel.GameViewModel

// Board colors
private val LightSquare = Color(0xFFF0D9B5)
private val DarkSquare = Color(0xFFB58863)
private val SelectedHighlight = Color(0x66829769)
private val LegalMoveDot = Color(0x55829769)
private val LegalCaptureRing = Color(0x66829769)
private val LastMoveHighlight = Color(0x44CDD26A)
private val CheckHighlight = Color(0xCCE53935)

// Piece colors
private val WhitePieceColor = Color(0xFFFFFFFF)
private val WhitePieceOutline = Color(0xFF333333)
private val BlackPieceColor = Color(0xFF1A1A1A)
private val BlackPieceHighlight = Color(0xFF444444)

// App colors
private val AppBackground = Color(0xFF1E1B18)
private val AppSurface = Color(0xFF2A2724)
private val AppBarColor = Color(0xFF24211E)
private val GoldAccent = Color(0xFFF0D9B5)
private val BrownAccent = Color(0xFFB58863)
private val GreenAccent = Color(0xFF829769)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // VS Intro overlay
        if (uiState.showVsIntro) {
            VsIntroOverlay(
                onFinished = { viewModel.onVsIntroFinished() }
            )
            return@Box
        }

        Scaffold(
            containerColor = AppBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Chess Battle",
                                fontWeight = FontWeight.SemiBold,
                                color = GoldAccent,
                                fontSize = 18.sp
                            )
                            if (uiState.isAiThinking) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Thinking…",
                                    fontSize = 13.sp,
                                    color = BrownAccent
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppBarColor
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Turn indicator with player names
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerLabel(
                        name = "Mr Whis",
                        color = PieceColor.WHITE,
                        isActive = uiState.currentTurn == PieceColor.WHITE,
                        capturedCount = uiState.capturedWhite.size
                    )

                    Text(
                        text = "VS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666)
                    )

                    PlayerLabel(
                        name = "Oliver Vert",
                        color = PieceColor.BLACK,
                        isActive = uiState.currentTurn == PieceColor.BLACK,
                        capturedCount = uiState.capturedBlack.size
                    )
                }

                // Status bar
                AnimatedContent(
                    targetState = uiState.statusMessage,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "status"
                ) { msg ->
                    Text(
                        text = msg,
                        color = when {
                            uiState.isCheckmate -> Color(0xFFF4A236)
                            uiState.isStalemate -> Color(0xFF82AADB)
                            uiState.isCheck -> Color(0xFFE53935)
                            else -> Color(0xFFCCCCCC)
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Captured pieces row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppSurface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("? ${uiState.capturedWhite.size}", color = Color(0xFF999999), fontSize = 13.sp)
                    Text("Captured", color = Color(0xFF777777), fontSize = 12.sp)
                    Text("? ${uiState.capturedBlack.size}", color = Color(0xFF999999), fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chess board
                ChessBoard(
                    board = uiState.board,
                    selectedPosition = uiState.selectedPosition,
                    legalMoves = uiState.legalMoves,
                    lastMoveFrom = uiState.lastMoveFrom,
                    lastMoveTo = uiState.lastMoveTo,
                    isCheck = uiState.isCheck,
                    currentTurn = uiState.currentTurn,
                    captureEffect = if (uiState.showCaptureAnim) uiState.captureEffect else null,
                    onSquareTapped = { pos -> viewModel.onSquareTapped(pos) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(2.dp, Color(0xFF3D3A37), RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (uiState.gameMode == com.rork.chessbattle.model.GameMode.PLAYER_VS_AI)
                            "AI Level ${uiState.aiDifficulty}" else "Local PvP",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = if (uiState.gameOver) "Game Over" else "Playing",
                        fontSize = 12.sp,
                        color = if (uiState.gameOver) Color(0xFFE53935) else Color(0xFF666666)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reset button
                Button(
                    onClick = { viewModel.resetGame() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrownAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "Reset Game",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PlayerLabel(
    name: String,
    color: PieceColor,
    isActive: Boolean,
    capturedCount: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) {
                if (color == PieceColor.WHITE) Color.White else Color(0xFFCCCCCC)
            } else {
                Color(0xFF666666)
            }
        )
        Text(
            text = if (color == PieceColor.WHITE) "?" else "?",
            fontSize = 12.sp,
            color = if (isActive) BrownAccent else Color(0xFF444444)
        )
    }
}

@Composable
fun VsIntroOverlay(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2600)
        visible = false
        delay(400)
        onFinished()
    }

    val vsScale by animateFloatAsState(
        targetValue = if (visible) 1f else 1.5f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "vsScale"
    )
    val vsAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "vsAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2A2724),
                        Color(0xFF1E1B18),
                        Color(0xFF0D0D0D)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(vsScale)
                .graphicsLayer { this.alpha = vsAlpha }
        ) {
            // VS Text
            val vsAnimProgress = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                vsAnimProgress.animateTo(1f, animationSpec = tween(1200, easing = EaseOutCubic))
            }

            Text(
                text = "Mr Whis",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 3.sp,
                modifier = Modifier
                    .offset {
                        IntOffset(0, ((1f - vsAnimProgress.value) * 60).toInt())
                    }
                    .graphicsLayer { alpha = vsAnimProgress.value }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "?  VS  ?",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldAccent,
                modifier = Modifier.graphicsLayer { alpha = vsAnimProgress.value }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Oliver Vert",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCCCCCC),
                letterSpacing = 3.sp,
                modifier = Modifier
                    .offset {
                        IntOffset(0, ((vsAnimProgress.value - 1f) * 60).toInt())
                    }
                    .graphicsLayer { alpha = vsAnimProgress.value }
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "who is the Good dog?",
                fontSize = 18.sp,
                color = BrownAccent,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.graphicsLayer {
                    alpha = (vsAnimProgress.value - 0.3f).coerceIn(0f, 1f)
                }
            )
        }
    }
}

@Composable
private fun ChessBoard(
    board: Array<Array<Piece?>>,
    selectedPosition: Position?,
    legalMoves: List<com.rork.chessbattle.model.Move>,
    lastMoveFrom: Position?,
    lastMoveTo: Position?,
    isCheck: Boolean,
    currentTurn: PieceColor,
    captureEffect: CaptureEffect?,
    onSquareTapped: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        for (row in 0..7) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0..7) {
                    val pos = Position(row, col)
                    val piece = board[row][col]
                    val isLightSquare = (row + col) % 2 == 0
                    val isSelected = pos == selectedPosition
                    val isLegalTarget = legalMoves.any { it.to == pos }
                    val isCaptureTarget = legalMoves.any { it.to == pos && it.isCapture }
                    val wasLastMove = pos == lastMoveFrom || pos == lastMoveTo
                    val kingInCheck = isCheck && piece?.type == PieceType.KING &&
                        piece.color == currentTurn

                    val baseColor = if (isLightSquare) LightSquare else DarkSquare
                    val targetColor by animateColorAsState(
                        targetValue = when {
                            kingInCheck -> blendColors(baseColor, CheckHighlight, 0.7f)
                            isSelected -> blendColors(baseColor, SelectedHighlight, 0.8f)
                            wasLastMove -> blendColors(baseColor, LastMoveHighlight, 0.6f)
                            else -> baseColor
                        },
                        animationSpec = tween(200),
                        label = "squareColor"
                    )

                    // Check if this square has a capture effect
                    val isCaptureSquare = captureEffect?.position == pos

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(targetColor)
                            .clickable { onSquareTapped(pos) },
                        contentAlignment = Alignment.Center
                    ) {
                        // Legal move indicator
                        if (isLegalTarget && !isSelected) {
                            if (isCaptureTarget) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.85f)
                                        .border(4.dp, LegalCaptureRing, RoundedCornerShape(99.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.28f)
                                        .background(LegalMoveDot, RoundedCornerShape(99.dp))
                                )
                            }
                        }

                        // Capture shatter animation
                        if (isCaptureSquare && captureEffect != null) {
                            CaptureShatterAnimation(piece = captureEffect.piece)
                        }
                        // Normal piece
                        else if (piece != null) {
                            PieceView(piece = piece)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieceView(piece: Piece) {
    val isWhite = piece.color == PieceColor.WHITE
    Text(
        text = piece.unicode(),
        fontSize = 34.sp,
        color = if (isWhite) WhitePieceColor else BlackPieceColor,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge.copy(
            shadow = Shadow(
                color = if (isWhite) Color(0x55000000) else Color(0x33FFFFFF),
                offset = Offset(1.5f, 2.5f),
                blurRadius = 3f
            )
        )
    )
}

@Composable
private fun CaptureShatterAnimation(piece: Piece) {
    // Particle shatter effect
    val isWhite = piece.color == PieceColor.WHITE
    val particleColor = if (isWhite) WhitePieceColor else BlackPieceColor

    // Generate random particle offsets
    val particles = remember {
        (0 until 8).map { i ->
            val angle = (360.0 / 8) * i + Math.random() * 20 - 10
            val distance = 40.0 + Math.random() * 30.0
            val rad = Math.toRadians(angle)
            Pair(
                (Math.cos(rad) * distance).toFloat(),
                (Math.sin(rad) * distance).toFloat()
            )
        }
    }

    // Animate
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(piece) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            1f,
            animationSpec = tween(450, easing = EaseOutCubic)
        )
    }

    Box(contentAlignment = Alignment.Center) {
        // Central piece that scales down and fades
        Text(
            text = piece.unicode(),
            fontSize = (34 * (1f - animProgress.value * 0.7f)).sp,
            color = particleColor.copy(alpha = 1f - animProgress.value),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .rotate(animProgress.value * 45f)
                .scale(1f + animProgress.value * 0.3f)
        )

        // Flying particles
        particles.forEachIndexed { index, (dx, dy) ->
            val particleProgress = (animProgress.value * 3f - index * 0.08f).coerceIn(0f, 1f)
            Text(
                text = "?",
                fontSize = (14 - index * 1.2f).sp,
                color = particleColor.copy(alpha = (1f - particleProgress) * 0.8f),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (dx * particleProgress).toInt(),
                            (dy * particleProgress).toInt()
                        )
                    }
                    .rotate(particleProgress * 360f)
            )
        }
    }
}

private fun blendColors(c1: Color, c2: Color, ratio: Float): Color {
    return Color(
        red = c1.red + (c2.red - c1.red) * ratio,
        green = c1.green + (c2.green - c1.green) * ratio,
        blue = c1.blue + (c2.blue - c1.blue) * ratio,
        alpha = 1f
    )
}
