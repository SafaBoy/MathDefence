package com.safaboy.mathdefence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safaboy.mathdefence.game.AppScreen
import com.safaboy.mathdefence.game.GameViewModel
import com.safaboy.mathdefence.ui.screens.GameScreen
import com.safaboy.mathdefence.ui.screens.MainMenuScreen
import com.safaboy.mathdefence.ui.theme.MathDefenceTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MathDefenceTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                BackHandler(enabled = state.screen == AppScreen.PLAYING) {
                    if (state.isGameOver) {
                        viewModel.returnToMainMenu()
                    } else {
                        viewModel.togglePause()
                    }
                }

                when (state.screen) {
                    AppScreen.MAIN_MENU -> {
                        MainMenuScreen(
                            highScores = state.highScoresByCategory,
                            soundEnabled = state.soundEnabled,
                            onToggleSound = viewModel::toggleSound,
                            onSelectCategory = viewModel::startGame
                        )
                    }
                    AppScreen.PLAYING -> {
                        GameScreen(
                            state = state,
                            onTargetTapped = viewModel::onTargetTapped,
                            onTogglePause = viewModel::togglePause,
                            onToggleSound = viewModel::toggleSound,
                            onRestart = viewModel::restartGame,
                            onExitToMenu = viewModel::returnToMainMenu
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppBackgrounded()
    }
}
