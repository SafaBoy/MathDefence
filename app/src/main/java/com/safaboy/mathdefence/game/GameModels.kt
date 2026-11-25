package com.safaboy.mathdefence.game

enum class AppScreen {
    MAIN_MENU,
    PLAYING
}

enum class MathCategory(
    val title: String,
    val symbol: String,
    val subtitle: String,
    val accentHex: Long
) {
    ADDITION(
        title = "Addition",
        symbol = "+",
        subtitle = "2d+1d up to 4d+2d & decimals",
        accentHex = 0xFF34D399
    ),
    SUBTRACTION(
        title = "Subtraction",
        symbol = "−",
        subtitle = "2d−1d up to 4d−2d & decimals",
        accentHex = 0xFF38BDF8
    ),
    MULTIPLICATION(
        title = "Multiplication",
        symbol = "×",
        subtitle = "1–5× tables up to 13×+ bosses",
        accentHex = 0xFFFBBF24
    ),
    DIVISION(
        title = "Division",
        symbol = "÷",
        subtitle = "1–5÷ tables up to 13÷+ bosses",
        accentHex = 0xFFC084FC
    ),
    MIXTURE(
        title = "Mixture",
        symbol = "±×÷",
        subtitle = "All operations scaling by skill level",
        accentHex = 0xFFFB7185
    )
}

enum class MathOperation(val symbol: String) {
    ADD("+"),
    SUBTRACT("−"),
    MULTIPLY("×"),
    DIVIDE("÷")
}

data class MathProblem(
    val displayOperandA: String,
    val displayOperandB: String,
    val operation: MathOperation,
    val correctAnswer: String,
    val choices: List<String>
) {
    val displayEquation: String
        get() = "$displayOperandA ${operation.symbol} $displayOperandB = ?"
}

enum class AttackerType(
    val displayName: String,
    val primaryColor: Long,
    val accentColor: Long,
    val laserColor: Long
) {
    BLASTER_BOT("Blaster Bot", 0xFFEF4444, 0xFFFCA5A5, 0xFFFF4D6D),
    HOVER_DRONE("Hover Drone", 0xFFF59E0B, 0xFFFDE68A, 0xFFFFB703),
    SIEGE_MECH("Siege Mech", 0xFF8B5CF6, 0xFFDDD6FE, 0xFFC084FC),
    VOID_STRIKER("Void Striker", 0xFFEC4899, 0xFFFBCFE8, 0xFFFF4DA6),
    PLASMA_TANK("Plasma Tank", 0xFFF97316, 0xFFFED7AA, 0xFFFF7A00),
    ARTILLERY_ORB("Artillery Orb", 0xFF10B981, 0xFFA7F3D0, 0xFF34D399)
}

data class AttackerEntity(
    val id: Int,
    val type: AttackerType,
    val answerValue: String,
    val isCorrect: Boolean,
    val normX: Float,
    val normY: Float,
    val bobPhase: Float,
    val fireFlashProgress: Float = 0f,
    val wrongTried: Boolean = false
)

enum class BossArchetype(
    val title: String,
    val hullPrimary: Long,
    val hullSecondary: Long,
    val accentColor: Long,
    val laserColor: Long
) {
    IRON_DREADNOUGHT("Iron Dreadnought", 0xFF991B1B, 0xFF450A0A, 0xFFFCA5A5, 0xFFFF3B5C),
    SOLAR_LEVIATHAN("Solar Leviathan", 0xFFB45309, 0xFF451A03, 0xFFFDE68A, 0xFFFFB703),
    VOID_TITAN("Void Titan", 0xFF6D28D9, 0xFF2E1065, 0xFFDDD6FE, 0xFFC084FC),
    PLASMA_COLOSSUS("Plasma Colossus", 0xFFC2410C, 0xFF431407, 0xFFFED7AA, 0xFFFF7A00),
    OMEGA_CITADEL("Omega Citadel", 0xFF047857, 0xFF064E3B, 0xFFA7F3D0, 0xFF34D399)
}

enum class BossComponentType(
    val shortLabel: String,
    val weaponDamage: Int
) {
    WING_LASER_L("L-Laser", 32),
    DORSAL_RAILGUN("Railgun", 36),
    WING_LASER_R("R-Laser", 32),
    MISSILE_POD_L("L-Missile", 30),
    NOVA_CORE("Nova Core", 36),
    MISSILE_POD_R("R-Missile", 30)
}

data class BossComponent(
    val id: Int,
    val componentType: BossComponentType,
    val normX: Float,
    val normY: Float,
    val answerValue: String = "",
    val isCorrect: Boolean = false,
    val isDestroyed: Boolean = false,
    val fireFlashProgress: Float = 0f,
    val wrongTried: Boolean = false
)

data class BossEntity(
    val id: Int,
    val archetype: BossArchetype,
    val skillLevelTier: Int,
    val components: List<BossComponent>,
    val hitsRequired: Int,
    val hitsLanded: Int = 0,
    val hitFlashProgress: Float = 0f
) {
    val intactComponents: List<BossComponent>
        get() = components.filter { !it.isDestroyed }

    val remainingHits: Int
        get() = (hitsRequired - hitsLanded).coerceAtLeast(0)

    val healthFraction: Float
        get() = if (hitsRequired <= 0) 0f else (remainingHits.toFloat() / hitsRequired.toFloat()).coerceIn(0f, 1f)
}

data class Projectile(
    val id: Int,
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float,
    val progress: Float = 0f,
    val isPlayerShot: Boolean,
    val isBossShot: Boolean = false,
    val colorArgb: Long
)

data class ExplosionParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float = 1f,
    val colorArgb: Long,
    val radius: Float
)

data class FloatingPopup(
    val id: Int,
    val text: String,
    val normX: Float,
    val normY: Float,
    val life: Float = 1f,
    val colorArgb: Long
)

enum class GameSoundEvent {
    CORRECT_FIRE,
    ENEMY_LASER,
    BOSS_LASER,
    SHIELD_HIT,
    SHIELD_BREAK,
    CASTLE_HIT,
    SHIELD_RESTORE,
    TIMER_TICK,
    BOSS_APPEAR,
    BOSS_COMPONENT_DESTROYED,
    BOSS_DEFEATED,
    PAUSE_DENIED,
    GAME_OVER
}

data class GameUiState(
    val screen: AppScreen = AppScreen.MAIN_MENU,
    val category: MathCategory = MathCategory.MIXTURE,
    val wave: Int = 1,
    val skillLevel: Int = 1,
    val skillLevelDescription: String = "",
    val nextBossWave: Int = 10,
    val bossesDefeated: Int = 0,
    val score: Int = 0,
    val highScore: Int = 0,
    val highScoresByCategory: Map<MathCategory, Int> = emptyMap(),
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val timeoutCount: Int = 0,
    val shieldHp: Int = 100,
    val maxShieldHp: Int = 100,
    val castleHp: Int = 100,
    val maxCastleHp: Int = 100,
    val pausesRemaining: Int = 3,
    val maxPauses: Int = 3,
    val currentProblem: MathProblem? = null,
    val attackers: List<AttackerEntity> = emptyList(),
    val activeBoss: BossEntity? = null,
    val answerDurationMs: Long = 10_000L,
    val remainingTimeMs: Long = 10_000L,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val isNewHighScore: Boolean = false,
    val soundEnabled: Boolean = true,
    val projectiles: List<Projectile> = emptyList(),
    val particles: List<ExplosionParticle> = emptyList(),
    val popups: List<FloatingPopup> = emptyList(),
    val shieldRipple: Float = 0f,
    val castleDamageFlash: Float = 0f,
    val screenShake: Float = 0f,
    val coreChargeFlash: Float = 0f,
    val statusBanner: String? = null,
    val statusBannerLife: Float = 0f,
    val elapsedAnimSec: Float = 0f
) {
    val isBossBattle: Boolean
        get() = activeBoss != null

    val wavesUntilBoss: Int
        get() = (nextBossWave - wave).coerceAtLeast(0)

    val timerFraction: Float
        get() = if (answerDurationMs <= 0L) 0f else (remainingTimeMs.toFloat() / answerDurationMs.toFloat()).coerceIn(0f, 1f)

    val shieldFraction: Float
        get() = if (maxShieldHp <= 0) 0f else (shieldHp.toFloat() / maxShieldHp.toFloat()).coerceIn(0f, 1f)

    val castleFraction: Float
        get() = if (maxCastleHp <= 0) 0f else (castleHp.toFloat() / maxCastleHp.toFloat()).coerceIn(0f, 1f)

    val accuracyPercent: Int
        get() {
            val total = correctCount + wrongCount
            return if (total == 0) 0 else ((correctCount * 100f) / total).toInt()
        }
}
