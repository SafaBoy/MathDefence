package com.safaboy.mathdefence.game

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

data class EngineStepResult(
    val state: GameUiState,
    val events: List<GameSoundEvent> = emptyList()
)

class GameEngine(
    private val problemGenerator: MathProblemGenerator = MathProblemGenerator(),
    private val random: Random = Random.Default
) {
    companion object {
        const val INITIAL_SHIELD_HP = 100
        const val INITIAL_CASTLE_HP = 100
        const val INITIAL_ANSWER_TIME_MS = 10_000L
        const val MIN_ANSWER_TIME_MS = 2_800L
        const val WRONG_ANSWER_DAMAGE = 20
        const val TIMEOUT_DAMAGE = 18
        const val STREAK_RECHARGE_INTERVAL = 5
        const val STREAK_SHIELD_RECHARGE = 10

        const val CASTLE_CORE_X = 0.50f
        const val CASTLE_CORE_Y = 0.77f
        const val SHIELD_IMPACT_Y = 0.67f
        const val CASTLE_HULL_Y = 0.78f

        private val ATTACKER_SLOTS: List<Pair<Float, Float>> = listOf(
            0.25f to 0.17f,
            0.75f to 0.17f,
            0.25f to 0.43f,
            0.75f to 0.43f
        )

        fun computeAnswerDurationMs(wave: Int): Long {
            val safeWave = max(1, wave)
            val decayed = (INITIAL_ANSWER_TIME_MS * 0.935.pow((safeWave - 1).toDouble())).toLong()
            return decayed.coerceAtLeast(MIN_ANSWER_TIME_MS)
        }
    }

    private var nextEntityId = 1

    fun startNewGame(
        category: MathCategory,
        highScore: Int,
        highScoresByCategory: Map<MathCategory, Int>,
        soundEnabled: Boolean
    ): GameUiState {
        val wave = 1
        val duration = computeAnswerDurationMs(wave)
        val problem = problemGenerator.generate(category, wave)
        val attackers = spawnAttackers(problem)

        return GameUiState(
            screen = AppScreen.PLAYING,
            category = category,
            wave = wave,
            score = 0,
            highScore = highScore,
            highScoresByCategory = highScoresByCategory,
            streak = 0,
            bestStreak = 0,
            correctCount = 0,
            wrongCount = 0,
            timeoutCount = 0,
            shieldHp = INITIAL_SHIELD_HP,
            maxShieldHp = INITIAL_SHIELD_HP,
            castleHp = INITIAL_CASTLE_HP,
            maxCastleHp = INITIAL_CASTLE_HP,
            currentProblem = problem,
            attackers = attackers,
            answerDurationMs = duration,
            remainingTimeMs = duration,
            isPaused = false,
            isGameOver = false,
            isNewHighScore = false,
            soundEnabled = soundEnabled,
            statusBanner = "WAVE 1 • DEFEND THE CASTLE!",
            statusBannerLife = 1f
        )
    }

    fun onAttackerSelected(state: GameUiState, attackerId: Int): EngineStepResult {
        if (state.screen != AppScreen.PLAYING || state.isPaused || state.isGameOver) {
            return EngineStepResult(state)
        }
        val target = state.attackers.firstOrNull { it.id == attackerId }
            ?: return EngineStepResult(state)

        return if (target.isCorrect) {
            handleCorrectAnswer(state, target)
        } else {
            handleWrongAnswer(state, target)
        }
    }

    fun tick(state: GameUiState, deltaMs: Long): EngineStepResult {
        if (state.screen != AppScreen.PLAYING || state.isPaused || state.isGameOver) {
            return EngineStepResult(state)
        }
        val clampedDeltaMs = deltaMs.coerceIn(0L, 250L)
        val dtSec = clampedDeltaMs / 1000f
        val events = mutableListOf<GameSoundEvent>()

        val prevSeconds = ((state.remainingTimeMs + 999L) / 1000L).toInt()
        var nextRemainingMs = (state.remainingTimeMs - clampedDeltaMs).coerceAtLeast(0L)
        val currSeconds = ((nextRemainingMs + 999L) / 1000L).toInt()

        if (currSeconds in 1..3 && currSeconds < prevSeconds) {
            events += GameSoundEvent.TIMER_TICK
        }

        // Advance visual animations
        val updatedAttackers = state.attackers.map { attacker ->
            if (attacker.fireFlashProgress > 0f) {
                attacker.copy(fireFlashProgress = (attacker.fireFlashProgress - dtSec * 2.8f).coerceAtLeast(0f))
            } else {
                attacker
            }
        }

        val updatedProjectiles = state.projectiles
            .map { it.copy(progress = it.progress + dtSec * 3.6f) }
            .filter { it.progress < 1f }

        val updatedParticles = state.particles
            .map { p ->
                p.copy(
                    x = p.x + p.vx * dtSec,
                    y = p.y + p.vy * dtSec,
                    vy = p.vy + 0.35f * dtSec,
                    life = p.life - dtSec * 1.9f
                )
            }
            .filter { it.life > 0f }

        val updatedPopups = state.popups
            .map { popup ->
                popup.copy(
                    normY = popup.normY - dtSec * 0.11f,
                    life = popup.life - dtSec * 1.15f
                )
            }
            .filter { it.life > 0f }

        var nextState = state.copy(
            remainingTimeMs = nextRemainingMs,
            attackers = updatedAttackers,
            projectiles = updatedProjectiles,
            particles = updatedParticles,
            popups = updatedPopups,
            shieldRipple = (state.shieldRipple - dtSec * 2.3f).coerceAtLeast(0f),
            castleDamageFlash = (state.castleDamageFlash - dtSec * 2.4f).coerceAtLeast(0f),
            screenShake = (state.screenShake - dtSec * 3.6f).coerceAtLeast(0f),
            coreChargeFlash = (state.coreChargeFlash - dtSec * 2.5f).coerceAtLeast(0f),
            statusBannerLife = (state.statusBannerLife - dtSec * 0.55f).coerceAtLeast(0f),
            statusBanner = if (state.statusBannerLife - dtSec * 0.55f <= 0f) null else state.statusBanner,
            elapsedAnimSec = state.elapsedAnimSec + dtSec
        )

        if (nextRemainingMs <= 0L) {
            val timeoutResult = handleTimeout(nextState)
            nextState = timeoutResult.state
            events += timeoutResult.events
        }

        return EngineStepResult(nextState, events)
    }

    private fun handleCorrectAnswer(
        state: GameUiState,
        target: AttackerEntity
    ): EngineStepResult {
        val events = mutableListOf(GameSoundEvent.CORRECT_FIRE)

        val newStreak = state.streak + 1
        val bestStreak = max(state.bestStreak, newStreak)
        val speedBonus = (state.timerFraction * 50f).toInt()
        val pointsEarned = 100 + (state.wave * 15) + speedBonus + (newStreak * 20)
        val newScore = state.score + pointsEarned
        val isNewBest = newScore > state.highScore
        val newHighScore = max(state.highScore, newScore)

        val nextWave = state.wave + 1
        val nextDuration = computeAnswerDurationMs(nextWave)
        val nextProblem = problemGenerator.generate(state.category, nextWave)
        val nextAttackers = spawnAttackers(nextProblem)

        var newShieldHp = state.shieldHp
        var banner = state.statusBanner
        var bannerLife = state.statusBannerLife
        val newPopups = state.popups.toMutableList()
        newPopups += FloatingPopup(
            id = nextEntityId++,
            text = "+$pointsEarned",
            normX = target.normX,
            normY = target.normY,
            colorArgb = 0xFF34D399
        )

        if (newStreak % STREAK_RECHARGE_INTERVAL == 0 && newShieldHp in 1 until state.maxShieldHp) {
            newShieldHp = (newShieldHp + STREAK_SHIELD_RECHARGE).coerceAtMost(state.maxShieldHp)
            events += GameSoundEvent.SHIELD_RESTORE
            banner = "STREAK x$newStreak! +$STREAK_SHIELD_RECHARGE SHIELD"
            bannerLife = 1f
            newPopups += FloatingPopup(
                id = nextEntityId++,
                text = "+$STREAK_SHIELD_RECHARGE SHIELD",
                normX = CASTLE_CORE_X,
                normY = SHIELD_IMPACT_Y - 0.03f,
                colorArgb = 0xFF22D3EE
            )
        } else if (nextWave % 5 == 0) {
            banner = "WAVE $nextWave • ATTACKS ACCELERATING!"
            bannerLife = 1f
        }

        val playerBeam = Projectile(
            id = nextEntityId++,
            fromX = CASTLE_CORE_X,
            fromY = CASTLE_CORE_Y,
            toX = target.normX,
            toY = target.normY,
            isPlayerShot = true,
            colorArgb = 0xFF22D3EE
        )

        val burst = createExplosion(
            normX = target.normX,
            normY = target.normY,
            primaryColor = target.type.primaryColor,
            secondaryColor = 0xFF22D3EE,
            count = 18
        )

        val updatedHighScores = state.highScoresByCategory.toMutableMap()
        updatedHighScores[state.category] = newHighScore

        val nextState = state.copy(
            wave = nextWave,
            score = newScore,
            highScore = newHighScore,
            highScoresByCategory = updatedHighScores,
            streak = newStreak,
            bestStreak = bestStreak,
            correctCount = state.correctCount + 1,
            shieldHp = newShieldHp,
            currentProblem = nextProblem,
            attackers = nextAttackers,
            answerDurationMs = nextDuration,
            remainingTimeMs = nextDuration,
            isNewHighScore = state.isNewHighScore || isNewBest,
            projectiles = state.projectiles + playerBeam,
            particles = (state.particles + burst).takeLast(90),
            popups = newPopups.takeLast(10),
            coreChargeFlash = 1f,
            statusBanner = banner,
            statusBannerLife = bannerLife
        )

        return EngineStepResult(nextState, events)
    }

    private fun handleWrongAnswer(
        state: GameUiState,
        shooter: AttackerEntity
    ): EngineStepResult {
        val updatedAttackers = state.attackers.map { attacker ->
            if (attacker.id == shooter.id) {
                attacker.copy(fireFlashProgress = 1f, wrongTried = true)
            } else {
                attacker
            }
        }
        return applyEnemyAttack(
            state = state.copy(
                attackers = updatedAttackers,
                wrongCount = state.wrongCount + 1,
                streak = 0
            ),
            shooter = shooter,
            damage = WRONG_ANSWER_DAMAGE,
            reasonPopup = "WRONG!"
        )
    }

    private fun handleTimeout(state: GameUiState): EngineStepResult {
        if (state.attackers.isEmpty()) {
            return EngineStepResult(state.copy(remainingTimeMs = state.answerDurationMs))
        }
        val shooter = state.attackers[random.nextInt(state.attackers.size)]
        val updatedAttackers = state.attackers.map { attacker ->
            if (attacker.id == shooter.id) {
                attacker.copy(fireFlashProgress = 1f)
            } else {
                attacker
            }
        }

        val attackResult = applyEnemyAttack(
            state = state.copy(
                attackers = updatedAttackers,
                timeoutCount = state.timeoutCount + 1,
                streak = 0
            ),
            shooter = shooter,
            damage = TIMEOUT_DAMAGE,
            reasonPopup = "TOO SLOW!"
        )

        val finalState = if (attackResult.state.isGameOver) {
            attackResult.state
        } else {
            attackResult.state.copy(remainingTimeMs = attackResult.state.answerDurationMs)
        }

        return EngineStepResult(finalState, attackResult.events)
    }

    private fun applyEnemyAttack(
        state: GameUiState,
        shooter: AttackerEntity,
        damage: Int,
        reasonPopup: String
    ): EngineStepResult {
        val events = mutableListOf(GameSoundEvent.ENEMY_LASER)
        val hittingShield = state.shieldHp > 0
        val impactY = if (hittingShield) SHIELD_IMPACT_Y else CASTLE_HULL_Y

        var newShieldHp = state.shieldHp
        var newCastleHp = state.castleHp
        var shieldRipple = state.shieldRipple
        var castleFlash = state.castleDamageFlash
        var banner = state.statusBanner
        var bannerLife = state.statusBannerLife
        var isGameOver = false

        val particles = state.particles.toMutableList()
        val popups = state.popups.toMutableList()

        if (hittingShield) {
            newShieldHp = (state.shieldHp - damage).coerceAtLeast(0)
            shieldRipple = 1f
            particles += createExplosion(
                normX = CASTLE_CORE_X,
                normY = impactY,
                primaryColor = 0xFF22D3EE,
                secondaryColor = shooter.type.laserColor,
                count = if (newShieldHp == 0) 26 else 12
            )
            if (newShieldHp == 0) {
                events += GameSoundEvent.SHIELD_BREAK
                banner = "SHIELD DEPLETED! CASTLE EXPOSED!"
                bannerLife = 1f
                popups += FloatingPopup(
                    id = nextEntityId++,
                    text = "SHIELD BROKEN!",
                    normX = CASTLE_CORE_X,
                    normY = impactY - 0.04f,
                    colorArgb = 0xFFF59E0B
                )
            } else {
                events += GameSoundEvent.SHIELD_HIT
                popups += FloatingPopup(
                    id = nextEntityId++,
                    text = "$reasonPopup -$damage SHIELD",
                    normX = CASTLE_CORE_X,
                    normY = impactY - 0.03f,
                    colorArgb = 0xFFFB7185
                )
            }
        } else {
            newCastleHp = (state.castleHp - damage).coerceAtLeast(0)
            castleFlash = 1f
            events += GameSoundEvent.CASTLE_HIT
            particles += createExplosion(
                normX = CASTLE_CORE_X,
                normY = impactY,
                primaryColor = 0xFFEF4444,
                secondaryColor = 0xFFF97316,
                count = 18
            )
            popups += FloatingPopup(
                id = nextEntityId++,
                text = "$reasonPopup -$damage CASTLE",
                normX = CASTLE_CORE_X,
                normY = impactY - 0.04f,
                colorArgb = 0xFFEF4444
            )
            if (newCastleHp <= 0) {
                isGameOver = true
                events += GameSoundEvent.GAME_OVER
                banner = "CASTLE DESTROYED!"
                bannerLife = 1f
            }
        }

        val enemyShot = Projectile(
            id = nextEntityId++,
            fromX = shooter.normX,
            fromY = shooter.normY + 0.05f,
            toX = CASTLE_CORE_X,
            toY = impactY,
            isPlayerShot = false,
            colorArgb = shooter.type.laserColor
        )

        val nextState = state.copy(
            shieldHp = newShieldHp,
            castleHp = newCastleHp,
            isGameOver = isGameOver,
            projectiles = state.projectiles + enemyShot,
            particles = particles.takeLast(90),
            popups = popups.takeLast(10),
            shieldRipple = shieldRipple,
            castleDamageFlash = castleFlash,
            screenShake = 1f,
            statusBanner = banner,
            statusBannerLife = bannerLife
        )

        return EngineStepResult(nextState, events)
    }

    private fun spawnAttackers(problem: MathProblem): List<AttackerEntity> {
        val types = AttackerType.entries.shuffled(random)
        return problem.choices.mapIndexed { index, answer ->
            val (baseX, baseY) = ATTACKER_SLOTS[index % ATTACKER_SLOTS.size]
            val jitterX = (random.nextFloat() - 0.5f) * 0.05f
            val jitterY = (random.nextFloat() - 0.5f) * 0.04f
            AttackerEntity(
                id = nextEntityId++,
                type = types[index % types.size],
                answerValue = answer,
                isCorrect = answer == problem.correctAnswer,
                normX = (baseX + jitterX).coerceIn(0.18f, 0.82f),
                normY = (baseY + jitterY).coerceIn(0.13f, 0.48f),
                bobPhase = random.nextFloat() * (2f * PI.toFloat())
            )
        }
    }

    private fun createExplosion(
        normX: Float,
        normY: Float,
        primaryColor: Long,
        secondaryColor: Long,
        count: Int
    ): List<ExplosionParticle> = List(count) { idx ->
        val angle = random.nextFloat() * (2f * PI.toFloat())
        val speed = 0.14f + random.nextFloat() * 0.34f
        ExplosionParticle(
            x = normX,
            y = normY,
            vx = cos(angle) * speed,
            vy = sin(angle) * speed - 0.06f,
            life = 0.7f + random.nextFloat() * 0.4f,
            colorArgb = if (idx % 2 == 0) primaryColor else secondaryColor,
            radius = 4f + random.nextFloat() * 6f
        )
    }
}
