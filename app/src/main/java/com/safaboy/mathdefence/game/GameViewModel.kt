package com.safaboy.mathdefence.game

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safaboy.mathdefence.audio.SoundEffects
import com.safaboy.mathdefence.data.ScoreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val scoreRepository = ScoreRepository(application)
    private val engine = GameEngine()
    private val soundEffects = SoundEffects()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = application.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Vibrator::class.java)
    }

    private val _uiState = MutableStateFlow(
        GameUiState(
            screen = AppScreen.MAIN_MENU,
            highScoresByCategory = scoreRepository.getAllHighScores(),
            soundEnabled = scoreRepository.isSoundEnabled()
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var gameLoopJob: Job? = null

    fun startGame(category: MathCategory) {
        val highScores = scoreRepository.getAllHighScores()
        val categoryBest = highScores[category] ?: 0
        val soundOn = _uiState.value.soundEnabled

        _uiState.value = engine.startNewGame(
            category = category,
            highScore = categoryBest,
            highScoresByCategory = highScores,
            soundEnabled = soundOn
        )
        startLoop()
    }

    fun restartGame() {
        startGame(_uiState.value.category)
    }

    fun returnToMainMenu() {
        stopLoop()
        val highScores = scoreRepository.getAllHighScores()
        _uiState.update {
            GameUiState(
                screen = AppScreen.MAIN_MENU,
                category = it.category,
                highScoresByCategory = highScores,
                soundEnabled = it.soundEnabled
            )
        }
    }

    fun togglePause() {
        val result = engine.requestTogglePause(_uiState.value)
        _uiState.value = result.state
        handleEvents(result.events)
    }

    fun onAppBackgrounded() {
        val result = engine.requestPauseIfAvailable(_uiState.value)
        _uiState.value = result.state
        handleEvents(result.events)
    }

    fun toggleSound() {
        val next = !_uiState.value.soundEnabled
        scoreRepository.setSoundEnabled(next)
        _uiState.update { it.copy(soundEnabled = next) }
    }

    fun onTargetTapped(targetId: Int) {
        val current = _uiState.value
        val result = engine.onAttackerSelected(current, targetId)
        _uiState.value = result.state

        if (result.state.score > current.highScore) {
            scoreRepository.saveHighScoreIfBetter(result.state.category, result.state.score)
        }
        handleEvents(result.events)
    }

    private fun startLoop() {
        stopLoop()
        gameLoopJob = viewModelScope.launch {
            var lastTimeMs = System.currentTimeMillis()
            while (isActive) {
                delay(16L)
                val now = System.currentTimeMillis()
                val delta = (now - lastTimeMs).coerceIn(0L, 120L)
                lastTimeMs = now

                val current = _uiState.value
                if (current.screen == AppScreen.PLAYING && !current.isPaused && !current.isGameOver) {
                    val result = engine.tick(current, delta)
                    _uiState.value = result.state
                    if (result.state.isGameOver) {
                        scoreRepository.saveHighScoreIfBetter(result.state.category, result.state.score)
                    }
                    handleEvents(result.events)
                }
            }
        }
    }

    private fun stopLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    private fun handleEvents(events: List<GameSoundEvent>) {
        if (events.isEmpty()) return
        val soundOn = _uiState.value.soundEnabled
        for (event in events) {
            if (soundOn) {
                soundEffects.play(event)
            }
            when (event) {
                GameSoundEvent.SHIELD_HIT -> vibrate(40L, 90)
                GameSoundEvent.BOSS_LASER -> vibrate(95L, 175)
                GameSoundEvent.SHIELD_BREAK -> vibrate(120L, 180)
                GameSoundEvent.CASTLE_HIT -> vibrate(90L, 160)
                GameSoundEvent.BOSS_COMPONENT_DESTROYED -> vibrate(55L, 130)
                GameSoundEvent.BOSS_DEFEATED -> vibrate(160L, 200)
                GameSoundEvent.PAUSE_DENIED -> vibrate(50L, 110)
                GameSoundEvent.GAME_OVER -> vibrate(220L, 210)
                else -> Unit
            }
        }
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        runCatching {
            val vib = vibrator ?: return
            if (!vib.hasVibrator()) return
            vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLoop()
    }
}
