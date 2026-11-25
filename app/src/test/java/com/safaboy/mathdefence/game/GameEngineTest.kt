package com.safaboy.mathdefence.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private fun createEngine(seed: Int = 101): GameEngine {
        val random = Random(seed)
        return GameEngine(MathProblemGenerator(random), random)
    }

    @Test
    fun startingGameInitializesShieldCastlePausesAndSkillLevel() {
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
        assertEquals(1, state.skillLevel)
        assertEquals(GameEngine.FIRST_BOSS_WAVE, state.nextBossWave)
        assertEquals(GameEngine.MAX_PAUSES_PER_GAME, state.pausesRemaining)
        assertNotNull(state.currentProblem)
        assertEquals(4, state.attackers.size)
        assertEquals(1, state.attackers.count { it.isCorrect })
        assertNull(state.activeBoss)
    }

    @Test
    fun pauseLimitEnforcesThreePausesPerGameAndAllowsFreeResume() {
        val engine = createEngine()
        var state = engine.startNewGame(MathCategory.ADDITION, 0, emptyMap(), true)

        // Use all 3 pauses (pause -> resume x3)
        for (expectedRemaining in listOf(2, 1, 0)) {
            val paused = engine.requestTogglePause(state)
            state = paused.state
            assertTrue(state.isPaused)
            assertEquals(expectedRemaining, state.pausesRemaining)

            val resumed = engine.requestTogglePause(state)
            state = resumed.state
            assertFalse(state.isPaused)
            assertEquals(expectedRemaining, state.pausesRemaining)
        }

        // 4th pause attempt must be denied!
        val denied = engine.requestTogglePause(state)
        assertFalse("4th pause must not pause the game", denied.state.isPaused)
        assertEquals(0, denied.state.pausesRemaining)
        assertTrue(denied.events.contains(GameSoundEvent.PAUSE_DENIED))
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
        assertFalse(shot.isBossShot)
        assertEquals(wrongAttacker.normX, shot.fromX, 0.001f)
    }

    @Test
    fun bossAppearsAtWave10ComponentsBreakHeavyWeaponsDealMoreDamageAndSkillLevelAdvances() {
        val engine = createEngine(seed = 77)
        var state = engine.startNewGame(MathCategory.MULTIPLICATION, 0, emptyMap(), true)

        // Clear waves 1 through 9 to reach Boss Wave 10
        repeat(GameEngine.FIRST_BOSS_WAVE - 1) {
            val correct = state.attackers.first { it.isCorrect }
            state = engine.onAttackerSelected(state, correct.id).state
        }

        assertEquals(GameEngine.FIRST_BOSS_WAVE, state.wave)
        assertTrue(state.isBossBattle)
        assertTrue(state.attackers.isEmpty())
        val boss = state.activeBoss!!
        assertEquals(GameEngine.BOSS_COMPONENT_COUNT, boss.components.size)
        assertEquals(GameEngine.BOSS_COMPONENT_COUNT, boss.intactComponents.size)
        assertEquals(1, boss.components.count { it.isCorrect })

        // Test Boss Heavy Weapon deals MORE damage than normal attackers
        val wrongComp = boss.components.first { !it.isCorrect }
        val shieldBefore = state.shieldHp
        val wrongBossHit = engine.onAttackerSelected(state, wrongComp.id)
        state = wrongBossHit.state
        val bossDamageDealt = shieldBefore - state.shieldHp
        assertTrue(
            "Boss weapon damage ($bossDamageDealt) must exceed normal damage (${GameEngine.WRONG_ANSWER_DAMAGE})",
            bossDamageDealt > GameEngine.WRONG_ANSWER_DAMAGE
        )
        assertTrue(wrongBossHit.events.contains(GameSoundEvent.BOSS_LASER))

        // Hit 1st correct boss component -> destroyed and unusable!
        val firstCorrectComp = state.activeBoss!!.components.first { it.isCorrect }
        val hit1 = engine.onAttackerSelected(state, firstCorrectComp.id)
        state = hit1.state
        assertTrue(hit1.events.contains(GameSoundEvent.BOSS_COMPONENT_DESTROYED))
        val destroyedComp = state.activeBoss!!.components.first { it.id == firstCorrectComp.id }
        assertTrue(destroyedComp.isDestroyed)
        assertEquals("", destroyedComp.answerValue)
        assertEquals(GameEngine.BOSS_COMPONENT_COUNT - 1, state.activeBoss!!.intactComponents.size)

        // Tapping destroyed component does nothing (no damage, no state change)
        val shieldAfterHit1 = state.shieldHp
        val tapDestroyed = engine.onAttackerSelected(state, destroyedComp.id)
        assertEquals(shieldAfterHit1, tapDestroyed.state.shieldHp)
        assertTrue(tapDestroyed.events.isEmpty())

        // Timeout during boss battle must ONLY fire an intact component (never destroyed)
        var elapsed = 0L
        while (elapsed < state.answerDurationMs + 250L) {
            val step = engine.tick(state, 200L)
            state = step.state
            if (step.events.contains(GameSoundEvent.BOSS_LASER)) break
            elapsed += 200L
        }
        val stillDestroyed = state.activeBoss!!.components.first { it.id == destroyedComp.id }
        assertEquals(0f, stillDestroyed.fireFlashProgress, 0.001f)
        assertTrue(state.activeBoss!!.intactComponents.any { it.fireFlashProgress > 0.5f })

        // Destroy remaining 4 boss components to defeat the boss!
        var defeatEvents = emptyList<GameSoundEvent>()
        repeat(GameEngine.BOSS_HITS_TO_DEFEAT - 1) {
            val nextCorrect = state.activeBoss!!.components.first { !it.isDestroyed && it.isCorrect }
            val res = engine.onAttackerSelected(state, nextCorrect.id)
            state = res.state
            defeatEvents = res.events
        }

        // Boss defeated -> Skill Level increments 1 -> 2, next boss scheduled 10..20 waves later!
        assertNull(state.activeBoss)
        assertFalse(state.isBossBattle)
        assertEquals(2, state.skillLevel)
        assertEquals(1, state.bossesDefeated)
        assertTrue(defeatEvents.contains(GameSoundEvent.BOSS_DEFEATED))
        val interval = state.nextBossWave - state.wave
        assertTrue(
            "Next boss interval ($interval) must be between 10 and 20 waves",
            interval in GameEngine.MIN_BOSS_INTERVAL..GameEngine.MAX_BOSS_INTERVAL
        )
        assertEquals(4, state.attackers.size)
    }

    @Test
    fun depletedShieldRoutesDamageToCastleUntilGameOver() {
        val engine = createEngine()
        var state = engine.startNewGame(MathCategory.MIXTURE, 0, emptyMap(), true)

        repeat(5) {
            val wrong = state.attackers.first { !it.isCorrect }
            state = engine.onAttackerSelected(state, wrong.id).state
        }
        assertEquals(0, state.shieldHp)
        assertEquals(GameEngine.INITIAL_CASTLE_HP, state.castleHp)
        assertFalse(state.isGameOver)

        var lastEvents = emptyList<GameSoundEvent>()
        repeat(5) {
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
