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
        const val MAX_PAUSES_PER_GAME = 3
        const val FIRST_BOSS_WAVE = 10
        const val MIN_BOSS_INTERVAL = 10
        const val MAX_BOSS_INTERVAL = 20
        const val BOSS_COMPONENT_COUNT = 6
        const val BOSS_HITS_TO_DEFEAT = 5 // 6 -> 5 -> 4 -> 3 -> 2 -> Core Meltdown!
        const val BOSS_VICTORY_SHIELD_REPAIR = 30
        const val BOSS_TIME_BONUS_MS = 1_400L

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

        private val BOSS_COMPONENT_LAYOUT: List<Triple<BossComponentType, Float, Float>> = listOf(
            Triple(BossComponentType.WING_LASER_L, 0.17f, 0.21f),
            Triple(BossComponentType.DORSAL_RAILGUN, 0.50f, 0.15f),
            Triple(BossComponentType.WING_LASER_R, 0.83f, 0.21f),
            Triple(BossComponentType.MISSILE_POD_L, 0.20f, 0.41f),
            Triple(BossComponentType.NOVA_CORE, 0.50f, 0.37f),
            Triple(BossComponentType.MISSILE_POD_R, 0.80f, 0.41f)
        )

        fun computeAnswerDurationMs(wave: Int): Long {
            val safeWave = max(1, wave)
            val decayed = (INITIAL_ANSWER_TIME_MS * 0.94.pow((safeWave - 1).toDouble())).toLong()
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
        val skillLevel = 1
        val duration = computeAnswerDurationMs(wave)
        val problem = problemGenerator.generate(category, skillLevel, choiceCount = 4)
        val attackers = spawnAttackers(problem)

        return GameUiState(
            screen = AppScreen.PLAYING,
            category = category,
            wave = wave,
            skillLevel = skillLevel,
            skillLevelDescription = MathProblemGenerator.describeSkillLevel(category, skillLevel),
            nextBossWave = FIRST_BOSS_WAVE,
            bossesDefeated = 0,
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
            pausesRemaining = MAX_PAUSES_PER_GAME,
            maxPauses = MAX_PAUSES_PER_GAME,
            currentProblem = problem,
            attackers = attackers,
            activeBoss = null,
            answerDurationMs = duration,
            remainingTimeMs = duration,
            isPaused = false,
            isGameOver = false,
            isNewHighScore = false,
            soundEnabled = soundEnabled,
            statusBanner = "LV 1 • ${MathProblemGenerator.describeSkillLevel(category, skillLevel)}",
            statusBannerLife = 1f
        )
    }

    fun requestTogglePause(state: GameUiState): EngineStepResult {
        if (state.screen != AppScreen.PLAYING || state.isGameOver) {
            return EngineStepResult(state)
        }
        // Always allow resuming without spending a pause
        if (state.isPaused) {
            return EngineStepResult(state.copy(isPaused = false))
        }
        // Enforce 3-pause-per-game limit
        return if (state.pausesRemaining > 0) {
            EngineStepResult(
                state.copy(
                    isPaused = true,
                    pausesRemaining = state.pausesRemaining - 1
                )
            )
        } else {
            EngineStepResult(
                state = state.copy(
                    statusBanner = "NO PAUSES LEFT! ($MAX_PAUSES_PER_GAME/$MAX_PAUSES_PER_GAME USED)",
                    statusBannerLife = 1f
                ),
                events = listOf(GameSoundEvent.PAUSE_DENIED)
            )
        }
    }

    fun requestPauseIfAvailable(state: GameUiState): EngineStepResult {
        if (state.screen != AppScreen.PLAYING || state.isGameOver || state.isPaused) {
            return EngineStepResult(state)
        }
        return requestTogglePause(state)
    }

    fun onAttackerSelected(state: GameUiState, attackerId: Int): EngineStepResult {
        if (state.screen != AppScreen.PLAYING || state.isPaused || state.isGameOver) {
            return EngineStepResult(state)
        }
        if (state.activeBoss != null) {
            return onBossComponentSelected(state, attackerId)
        }
        val target = state.attackers.firstOrNull { it.id == attackerId }
            ?: return EngineStepResult(state)

        return if (target.isCorrect) {
            handleCorrectAttacker(state, target)
        } else {
            handleWrongAttacker(state, target)
        }
    }

    fun onBossComponentSelected(state: GameUiState, componentId: Int): EngineStepResult {
        if (state.screen != AppScreen.PLAYING || state.isPaused || state.isGameOver) {
            return EngineStepResult(state)
        }
        val boss = state.activeBoss ?: return EngineStepResult(state)
        val component = boss.components.firstOrNull { it.id == componentId }
            ?: return EngineStepResult(state)

        // Destroyed components cannot be targeted or used
        if (component.isDestroyed) {
            return EngineStepResult(state)
        }

        return if (component.isCorrect) {
            handleCorrectBossComponent(state, boss, component)
        } else {
            handleWrongBossComponent(state, boss, component)
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
        val nextRemainingMs = (state.remainingTimeMs - clampedDeltaMs).coerceAtLeast(0L)
        val currSeconds = ((nextRemainingMs + 999L) / 1000L).toInt()

        if (currSeconds in 1..3 && currSeconds < prevSeconds) {
            events += GameSoundEvent.TIMER_TICK
        }

        val updatedAttackers = state.attackers.map { attacker ->
            if (attacker.fireFlashProgress > 0f) {
                attacker.copy(fireFlashProgress = (attacker.fireFlashProgress - dtSec * 2.8f).coerceAtLeast(0f))
            } else {
                attacker
            }
        }

        val updatedBoss = state.activeBoss?.let { boss ->
            boss.copy(
                hitFlashProgress = (boss.hitFlashProgress - dtSec * 3.0f).coerceAtLeast(0f),
                components = boss.components.map { comp ->
                    if (comp.fireFlashProgress > 0f) {
                        comp.copy(fireFlashProgress = (comp.fireFlashProgress - dtSec * 2.6f).coerceAtLeast(0f))
                    } else {
                        comp
                    }
                }
            )
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
            activeBoss = updatedBoss,
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

    private fun handleCorrectAttacker(
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

        // Check if next wave is a Boss Wave!
        return if (nextWave >= state.nextBossWave) {
            val bossProblem = problemGenerator.generate(
                category = state.category,
                skillLevel = state.skillLevel,
                choiceCount = BOSS_COMPONENT_COUNT
            )
            val boss = spawnBoss(state.skillLevel, bossProblem)
            val bossDuration = computeAnswerDurationMs(nextWave) + BOSS_TIME_BONUS_MS
            events += GameSoundEvent.BOSS_APPEAR

            EngineStepResult(
                state = state.copy(
                    wave = nextWave,
                    score = newScore,
                    highScore = newHighScore,
                    highScoresByCategory = updatedHighScores,
                    streak = newStreak,
                    bestStreak = bestStreak,
                    correctCount = state.correctCount + 1,
                    shieldHp = newShieldHp,
                    currentProblem = bossProblem,
                    attackers = emptyList(),
                    activeBoss = boss,
                    answerDurationMs = bossDuration,
                    remainingTimeMs = bossDuration,
                    isNewHighScore = state.isNewHighScore || isNewBest,
                    projectiles = state.projectiles + playerBeam,
                    particles = (state.particles + burst).takeLast(90),
                    popups = newPopups.takeLast(10),
                    coreChargeFlash = 1f,
                    statusBanner = "BOSS APPROACHING: ${boss.archetype.title.uppercase()}!",
                    statusBannerLife = 1.4f
                ),
                events = events
            )
        } else {
            val nextDuration = computeAnswerDurationMs(nextWave)
            val nextProblem = problemGenerator.generate(state.category, state.skillLevel, choiceCount = 4)
            val nextAttackers = spawnAttackers(nextProblem)

            if (nextWave % 5 == 0 && bannerLife <= 0.1f) {
                banner = "WAVE $nextWave • ATTACKS ACCELERATING!"
                bannerLife = 1f
            }

            EngineStepResult(
                state = state.copy(
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
                    activeBoss = null,
                    answerDurationMs = nextDuration,
                    remainingTimeMs = nextDuration,
                    isNewHighScore = state.isNewHighScore || isNewBest,
                    projectiles = state.projectiles + playerBeam,
                    particles = (state.particles + burst).takeLast(90),
                    popups = newPopups.takeLast(10),
                    coreChargeFlash = 1f,
                    statusBanner = banner,
                    statusBannerLife = bannerLife
                ),
                events = events
            )
        }
    }

    private fun handleCorrectBossComponent(
        state: GameUiState,
        boss: BossEntity,
        component: BossComponent
    ): EngineStepResult {
        val events = mutableListOf(GameSoundEvent.CORRECT_FIRE, GameSoundEvent.BOSS_COMPONENT_DESTROYED)

        val newStreak = state.streak + 1
        val bestStreak = max(state.bestStreak, newStreak)
        val speedBonus = (state.timerFraction * 75f).toInt()
        val pointsEarned = 180 + (state.skillLevel * 50) + speedBonus + (newStreak * 25)
        var newScore = state.score + pointsEarned

        val playerBeam = Projectile(
            id = nextEntityId++,
            fromX = CASTLE_CORE_X,
            fromY = CASTLE_CORE_Y,
            toX = component.normX,
            toY = component.normY,
            isPlayerShot = true,
            colorArgb = 0xFF22D3EE
        )

        val newParticles = state.particles.toMutableList()
        newParticles += createExplosion(
            normX = component.normX,
            normY = component.normY,
            primaryColor = boss.archetype.accentColor,
            secondaryColor = 0xFFFF6B35,
            count = 24
        )

        val newPopups = state.popups.toMutableList()
        newPopups += FloatingPopup(
            id = nextEntityId++,
            text = "${component.componentType.shortLabel.uppercase()} DESTROYED!",
            normX = component.normX,
            normY = component.normY,
            colorArgb = 0xFFFBBF24
        )

        // Mark hit component as permanently destroyed (unusable by boss)
        val damagedComponents = boss.components.map { c ->
            if (c.id == component.id) {
                c.copy(
                    isDestroyed = true,
                    answerValue = "",
                    isCorrect = false,
                    wrongTried = false,
                    fireFlashProgress = 0f
                )
            } else {
                c.copy(wrongTried = false)
            }
        }
        val hitsLanded = boss.hitsLanded + 1
        val intactRemaining = damagedComponents.filter { !it.isDestroyed }

        // Boss defeated when enough components are destroyed (<= 1 intact left)
        if (hitsLanded >= boss.hitsRequired || intactRemaining.size < 2) {
            val bossBonus = 500 * state.skillLevel
            newScore += bossBonus
            val isNewBest = newScore > state.highScore
            val newHighScore = max(state.highScore, newScore)
            val updatedHighScores = state.highScoresByCategory.toMutableMap()
            updatedHighScores[state.category] = newHighScore

            val nextSkillLevel = state.skillLevel + 1
            val nextWave = state.wave + 1
            val interval = random.nextInt(MIN_BOSS_INTERVAL, MAX_BOSS_INTERVAL + 1)
            val nextBossWave = nextWave + interval

            val nextProblem = problemGenerator.generate(state.category, nextSkillLevel, choiceCount = 4)
            val nextAttackers = spawnAttackers(nextProblem)
            val nextDuration = computeAnswerDurationMs(nextWave)

            val repairedShield = (state.shieldHp + BOSS_VICTORY_SHIELD_REPAIR).coerceAtMost(state.maxShieldHp)
            val nextSkillDesc = MathProblemGenerator.describeSkillLevel(state.category, nextSkillLevel)

            // Huge multi-component chain explosion across boss hull
            for (c in boss.components) {
                newParticles += createExplosion(
                    normX = c.normX,
                    normY = c.normY,
                    primaryColor = boss.archetype.hullPrimary,
                    secondaryColor = 0xFFFBBF24,
                    count = 10
                )
            }
            newPopups += FloatingPopup(
                id = nextEntityId++,
                text = "BOSS DEFEATED! +$bossBonus",
                normX = 0.50f,
                normY = 0.28f,
                colorArgb = 0xFF34D399
            )
            newPopups += FloatingPopup(
                id = nextEntityId++,
                text = "+$BOSS_VICTORY_SHIELD_REPAIR SHIELD",
                normX = CASTLE_CORE_X,
                normY = SHIELD_IMPACT_Y - 0.04f,
                colorArgb = 0xFF22D3EE
            )

            events += GameSoundEvent.BOSS_DEFEATED
            events += GameSoundEvent.SHIELD_RESTORE

            return EngineStepResult(
                state = state.copy(
                    wave = nextWave,
                    skillLevel = nextSkillLevel,
                    skillLevelDescription = nextSkillDesc,
                    nextBossWave = nextBossWave,
                    bossesDefeated = state.bossesDefeated + 1,
                    score = newScore,
                    highScore = newHighScore,
                    highScoresByCategory = updatedHighScores,
                    streak = newStreak,
                    bestStreak = bestStreak,
                    correctCount = state.correctCount + 1,
                    shieldHp = repairedShield,
                    currentProblem = nextProblem,
                    attackers = nextAttackers,
                    activeBoss = null,
                    answerDurationMs = nextDuration,
                    remainingTimeMs = nextDuration,
                    isNewHighScore = state.isNewHighScore || isNewBest,
                    projectiles = state.projectiles + playerBeam,
                    particles = newParticles.takeLast(110),
                    popups = newPopups.takeLast(12),
                    coreChargeFlash = 1f,
                    screenShake = 0.85f,
                    statusBanner = "SKILL LEVEL $nextSkillLevel UNLOCKED • $nextSkillDesc",
                    statusBannerLife = 1.6f
                ),
                events = events
            )
        } else {
            // Boss survives: generate next problem with answers distributed across intact components only
            val isNewBest = newScore > state.highScore
            val newHighScore = max(state.highScore, newScore)
            val updatedHighScores = state.highScoresByCategory.toMutableMap()
            updatedHighScores[state.category] = newHighScore

            val nextProblem = problemGenerator.generate(
                category = state.category,
                skillLevel = state.skillLevel,
                choiceCount = intactRemaining.size
            )
            val reassignedComponents = assignAnswersToIntactComponents(damagedComponents, nextProblem)
            val updatedBoss = boss.copy(
                components = reassignedComponents,
                hitsLanded = hitsLanded,
                hitFlashProgress = 1f
            )
            val bossDuration = computeAnswerDurationMs(state.wave) +
                if (intactRemaining.size >= 5) BOSS_TIME_BONUS_MS else 800L

            return EngineStepResult(
                state = state.copy(
                    score = newScore,
                    highScore = newHighScore,
                    highScoresByCategory = updatedHighScores,
                    streak = newStreak,
                    bestStreak = bestStreak,
                    correctCount = state.correctCount + 1,
                    currentProblem = nextProblem,
                    activeBoss = updatedBoss,
                    answerDurationMs = bossDuration,
                    remainingTimeMs = bossDuration,
                    isNewHighScore = state.isNewHighScore || isNewBest,
                    projectiles = state.projectiles + playerBeam,
                    particles = newParticles.takeLast(100),
                    popups = newPopups.takeLast(10),
                    coreChargeFlash = 1f,
                    screenShake = 0.45f,
                    statusBanner = "${boss.archetype.title.uppercase()}: ${updatedBoss.remainingHits} HITS LEFT!",
                    statusBannerLife = 1f
                ),
                events = events
            )
        }
    }

    private fun handleWrongAttacker(
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
            fromX = shooter.normX,
            fromY = shooter.normY + 0.05f,
            laserColor = shooter.type.laserColor,
            damage = WRONG_ANSWER_DAMAGE,
            isBossShot = false,
            reasonPopup = "WRONG!"
        )
    }

    private fun handleWrongBossComponent(
        state: GameUiState,
        boss: BossEntity,
        shooter: BossComponent
    ): EngineStepResult {
        val updatedBoss = boss.copy(
            components = boss.components.map { c ->
                if (c.id == shooter.id) {
                    c.copy(fireFlashProgress = 1f, wrongTried = true)
                } else {
                    c
                }
            }
        )
        return applyEnemyAttack(
            state = state.copy(
                activeBoss = updatedBoss,
                wrongCount = state.wrongCount + 1,
                streak = 0
            ),
            fromX = shooter.normX,
            fromY = shooter.normY + 0.05f,
            laserColor = boss.archetype.laserColor,
            damage = shooter.componentType.weaponDamage,
            isBossShot = true,
            reasonPopup = "BOSS ${shooter.componentType.shortLabel.uppercase()}!"
        )
    }

    private fun handleTimeout(state: GameUiState): EngineStepResult {
        val boss = state.activeBoss
        if (boss != null) {
            val intact = boss.intactComponents
            if (intact.isEmpty()) {
                return EngineStepResult(state.copy(remainingTimeMs = state.answerDurationMs))
            }
            val shooter = intact[random.nextInt(intact.size)]
            val updatedBoss = boss.copy(
                components = boss.components.map { c ->
                    if (c.id == shooter.id) c.copy(fireFlashProgress = 1f) else c
                }
            )
            val attackResult = applyEnemyAttack(
                state = state.copy(
                    activeBoss = updatedBoss,
                    timeoutCount = state.timeoutCount + 1,
                    streak = 0
                ),
                fromX = shooter.normX,
                fromY = shooter.normY + 0.05f,
                laserColor = boss.archetype.laserColor,
                damage = shooter.componentType.weaponDamage,
                isBossShot = true,
                reasonPopup = "BOSS BARRAGE!"
            )
            val finalState = if (attackResult.state.isGameOver) {
                attackResult.state
            } else {
                attackResult.state.copy(remainingTimeMs = attackResult.state.answerDurationMs)
            }
            return EngineStepResult(finalState, attackResult.events)
        }

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
            fromX = shooter.normX,
            fromY = shooter.normY + 0.05f,
            laserColor = shooter.type.laserColor,
            damage = TIMEOUT_DAMAGE,
            isBossShot = false,
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
        fromX: Float,
        fromY: Float,
        laserColor: Long,
        damage: Int,
        isBossShot: Boolean,
        reasonPopup: String
    ): EngineStepResult {
        val events = mutableListOf(if (isBossShot) GameSoundEvent.BOSS_LASER else GameSoundEvent.ENEMY_LASER)
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
                secondaryColor = laserColor,
                count = if (newShieldHp == 0) 28 else if (isBossShot) 20 else 12
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
                    colorArgb = if (isBossShot) 0xFFF97316 else 0xFFFB7185
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
                count = if (isBossShot) 24 else 18
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
            fromX = fromX,
            fromY = fromY,
            toX = CASTLE_CORE_X,
            toY = impactY,
            isPlayerShot = false,
            isBossShot = isBossShot,
            colorArgb = laserColor
        )

        val nextState = state.copy(
            shieldHp = newShieldHp,
            castleHp = newCastleHp,
            isGameOver = isGameOver,
            projectiles = state.projectiles + enemyShot,
            particles = particles.takeLast(110),
            popups = popups.takeLast(10),
            shieldRipple = shieldRipple,
            castleDamageFlash = castleFlash,
            screenShake = if (isBossShot) 1.35f else 1f,
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

    private fun spawnBoss(skillLevel: Int, problem: MathProblem): BossEntity {
        val archetype = BossArchetype.entries[(skillLevel - 1) % BossArchetype.entries.size]
        val baseComponents = BOSS_COMPONENT_LAYOUT.map { (type, nx, ny) ->
            BossComponent(
                id = nextEntityId++,
                componentType = type,
                normX = nx,
                normY = ny
            )
        }
        val assigned = assignAnswersToIntactComponents(baseComponents, problem)
        return BossEntity(
            id = nextEntityId++,
            archetype = archetype,
            skillLevelTier = skillLevel,
            components = assigned,
            hitsRequired = BOSS_HITS_TO_DEFEAT,
            hitsLanded = 0
        )
    }

    private fun assignAnswersToIntactComponents(
        components: List<BossComponent>,
        problem: MathProblem
    ): List<BossComponent> {
        val shuffledChoices = problem.choices.shuffled(random)
        var choiceIndex = 0
        return components.map { comp ->
            if (comp.isDestroyed) {
                comp.copy(answerValue = "", isCorrect = false)
            } else {
                val choice = shuffledChoices.getOrElse(choiceIndex) { problem.correctAnswer }
                choiceIndex++
                comp.copy(
                    answerValue = choice,
                    isCorrect = choice == problem.correctAnswer,
                    wrongTried = false
                )
            }
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
