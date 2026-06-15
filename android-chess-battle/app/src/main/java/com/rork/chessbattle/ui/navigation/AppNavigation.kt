package com.rork.chessbattle.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rork.chessbattle.model.GameMode
import com.rork.chessbattle.ui.screens.ChessScreen
import com.rork.chessbattle.ui.screens.HomeScreen
import com.rork.chessbattle.viewmodel.GameViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: GameViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // If game is in progress, navigate to game screen
            LaunchedEffect(uiState.showVsIntro, uiState.vsIntroCompleted) {
                if (uiState.showVsIntro || uiState.vsIntroCompleted) {
                    navController.navigate("game") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            }

            HomeScreen(
                onStartGame = { mode, difficulty ->
                    viewModel.startGame(mode, difficulty)
                },
                onDismissWelcome = { viewModel.dismissWelcome() },
                welcomeShown = uiState.welcomeShown
            )
        }

        composable("game") {
            ChessScreen(viewModel = viewModel)
        }
    }
}
