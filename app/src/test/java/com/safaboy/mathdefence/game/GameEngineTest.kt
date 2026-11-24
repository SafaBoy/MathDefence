package com.safaboy.mathdefence.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private fun createEngine(seed: Int = 101): GameEngine {
        val random = Random(seed)
        return GameEngine(MathProblemGenerator(random), random)
    }

    @Test
    fun startingGameInitializesShieldCastleProblemAndFourAttackers() {
        val engine = createEngine()
        val state = engine.startNewGame(
            category = MathCategory.MULTIPLICATION,
            highScore = 0,
            highScoresByCategory = emptyMap(),
            soundEnabled = true
        )

        assertEquals(AppScreen.PLAYING, state.screen)
        assertEquals(GameEngine.INITIAL_SHIELD_HP, state.shieldHp)
        assertEquals(GameEngine.INITIAL_CASTLE_HP, state.castleHp)
        assertEquals(1, state.wave)
        assertNotNull(state.currentProblem)
        assertEquals(4, state.attackers.size)
        assertEquals(1, state.attackers.count { it.isCorrect })
        assertEquals(GameEngine.INITIAL_ANSWER_TIME_MS, state.answerDurationMs)
    }

    @Test
    fun correctAnswerAdvancesWaveScoresPointsAndShortensTimer() {
        val engine = createEngine()
        val initial = engine.startNewGame(MathCategory.ADDITION, 0, emptyMap(), true)
        val correctAttacker = initial.attackers.first { it.isCorrect }

        val result = engine.onAttackerSelected(initial, correctAttacker.id)
        val next = result.state

        assertEquals(2, next.wave)
        assertTrue(next.score > 0)
        assertEquals(1, next.streak)
        assertEquals(1, next.correctCount)
        assertTrue(
            "Answer timer should decrease as waves progress",
            next.answerDurationMs < initial.answerDurationMs
        )
        assertEquals(next.answerDurationMs, next.remainingTimeMs)
        assertTrue(result.events.contains(GameSoundEvent.CORRECT_FIRE))
    }

    @Test
    fun wrongAnswerCausesPickedAttackerToFireAndReduceShield() {
        val engine = createEngine()
        val initial = engine.startNewGame(MathCategory.SUBTRACTION, 0, emptyMap(), true)
        val wrongAttacker = initial.attackers.first { !it.isCorrect }

        val result = engine.onAttackerSelected(initial, wrongAttacker.id)
        val next = result.state

        assertEquals(GameEngine.INITIAL_SHIELD_HP - GameEngine.WRONG_ANSWER_DAMAGE, next.shieldHp)
        assertEquals(GameEngine.INITIAL_CASTLE_HP, next.castleHp)
        assertEquals(1, next.wrongCount)
        assertEquals(0, next.streak)

        val updatedShooter = next.attackers.first { it.id == wrongAttacker.id }
        assertTrue(updatedShooter.wrongTried)
        assertTrue(updatedShooter.fireFlashProgress > 0.9f)

        val shot = next.projectiles.lastOrNull()
        assertNotNull(shot)
        assertFalse(shot!!.isPlayerShot)
        assertEquals(wrongAttacker.normX, shot.fromX, 0.001f)
        assertTrue(result.events.contains(GameSoundEvent.ENEMY_LASER))
        assertTrue(result.events.contains(GameSoundEvent.SHIELD_HIT))
    }

    @Test
    fun timeoutCausesRandomAttackerToFireAndResetsTimer() {
        val engine = createEngine()
        var state = engine.startNewGame(MathCategory.DIVISION, 0, emptyMap(), true)

        // Advance time in slices until timer expires
        var timeoutEvents = emptyList<GameSoundEvent>()
        var elapsed = 0L
        while (elapsed < state.answerDurationMs) {
            val step = engine.tick(state, 200L)
            state = step.state
            if (step.events.contains(GameSoundEvent.ENEMY_LASER)) {
                timeoutEvents = step.events
                break
            }
            elapsed += 200L
        }

        assertEquals(1, state.timeoutCount)
        assertEquals(GameEngine.INITIAL_SHIELD_HP - GameEngine.TIMEOUT_DAMAGE, state.shieldHp)
        assertEquals(GameEngine.INITIAL_CASTLE_HP, state.castleHp)
        assertEquals(state.answerDurationMs, state.remainingTimeMs)
        assertTrue(state.attackers.any { it.fireFlashProgress > 0.9f })
        assertTrue(timeoutEvents.contains(GameSoundEvent.ENEMY_LASER))
    }

    @Test
    fun depletedShieldRoutesDamageToCastleUntilGameOver() {
        val engine = createEngine()
        var state = engine.startNewGame(MathCategory.MIXTURE, 0, emptyMap(), true)

        // Drain shields completely (100 / 20 = 5 wrong shots)
        repeat(5) {
            val wrong = state.attackers.first { !it.isCorrect }
            state = engine.onAttackerSelected(state, wrong.id).state
        }
        assertEquals(0, state.shieldHp)
        assertEquals(GameEngine.INITIAL_CASTLE_HP, state.castleHp)
        assertFalse(state.isGameOver)

        // Next wrong shot must damage castle directly
        val wrong1 = state.attackers.first { !it.isCorrect }
        val hitCastle = engine.onAttackerSelected(state, wrong1.id)
        state = hitCastle.state
        assertEquals(GameEngine.INITIAL_CASTLE_HP - GameEngine.WRONG_ANSWER_DAMAGE, state.castleHp)
        assertTrue(hitCastle.events.contains(GameSoundEvent.CASTLE_HIT))

        // Drain remaining castle HP to 0 -> Game Over
        var lastEvents = emptyList<GameSoundEvent>()
        repeat(4) {
            val wrong = state.attackers.first { !it.isCorrect }
            val step = engine.onAttackerSelected(state, wrong.id)
            state = step.state
            lastEvents = step.events
        }

        assertEquals(0, state.castleHp)
        assertTrue(state.isGameOver)
        assertTrue(lastEvents.contains(GameSoundEvent.GAME_OVER))
    }

    @Test
    fun answerDurationDecreasesMonotonicallyAndRespectsMinimumFloor() {
        var previous = GameEngine.computeAnswerDurationMs(1)
        for (wave in 2..30) {
            val current = GameEngine.computeAnswerDurationMs(wave)
            assertTrue("Duration must not increase at wave $wave", current <= previous)
            assertTrue("Duration must respect floor", current >= GameEngine.MIN_ANSWER_TIME_MS)
            previous = current
        }
        assertEquals(GameEngine.MIN_ANSWER_TIME_MS, GameEngine.computeAnswerDurationMs(40))
    }
}
