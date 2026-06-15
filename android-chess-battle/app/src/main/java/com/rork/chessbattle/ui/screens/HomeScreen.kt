package com.rork.chessbattle.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.chessbattle.model.GameMode

private val AppBackground = Color(0xFF1E1B18)
private val AppSurface = Color(0xFF2A2724)
private val GoldAccent = Color(0xFFF0D9B5)
private val BrownAccent = Color(0xFFB58863)
private val GreenAccent = Color(0xFF829769)
private val RedAccent = Color(0xFFE53935)

@Composable
fun HomeScreen(
    onStartGame: (GameMode, Int) -> Unit,
    onDismissWelcome: () -> Unit,
    welcomeShown: Boolean
) {
    if (!welcomeShown) {
        WelcomeDialog(onDismiss = onDismissWelcome)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF24211E), AppBackground, Color(0xFF1A1816))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "CHESS BATTLE",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Mr Whis vs Oliver Vert",
                fontSize = 16.sp,
                color = BrownAccent,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Player vs Player button
            GameModeButton(
                title = "Player vs Player",
                subtitle = "Local multiplayer on one device",
                color1 = BrownAccent,
                color2 = GoldAccent,
                onClick = { onStartGame(GameMode.PLAYER_VS_PLAYER, 5) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Player vs AI button with difficulty
            PlayerVsAiSection(onStartGame = onStartGame)

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "who is the Good dog?",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
private fun PlayerVsAiSection(onStartGame: (GameMode, Int) -> Unit) {
    var difficulty by remember { mutableIntStateOf(5) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Player vs AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldAccent
                    )
                    Text(
                        text = "Battle against the machine",
                        fontSize = 13.sp,
                        color = Color(0xFF999999)
                    )
                }
                Text(
                    text = if (expanded) "?" else "?",
                    fontSize = 16.sp,
                    color = BrownAccent
                )
            }

            // Expanded difficulty panel
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFF3D3A37))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "AI Difficulty: $difficulty / 10",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCCCCCC)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = difficulty.toFloat(),
                        onValueChange = { difficulty = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldAccent,
                            activeTrackColor = BrownAccent,
                            inactiveTrackColor = Color(0xFF3D3A37)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Easy", fontSize = 11.sp, color = Color(0xFF666666))
                        Text("Hard", fontSize = 11.sp, color = Color(0xFF666666))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onStartGame(GameMode.PLAYER_VS_AI, difficulty) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Start Game",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Quick start when not expanded
            if (!expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onStartGame(GameMode.PLAYER_VS_AI, difficulty) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "Quick Start (Level $difficulty)",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GameModeButton(
    title: String,
    subtitle: String,
    color1: Color,
    color2: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(color1.copy(alpha = 0.3f), color2.copy(alpha = 0.15f))
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldAccent
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}

@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Welcome Alexis!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "to your chess game,\nI hope you enjoy!",
                fontSize = 16.sp,
                color = Color(0xFFCCCCCC),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrownAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "OK",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        },
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}
