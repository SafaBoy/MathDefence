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
        subtitle = "Rapid sums & mental combos",
        accentHex = 0xFF34D399
    ),
    SUBTRACTION(
        title = "Subtraction",
        symbol = "−",
        subtitle = "Sharp differences under fire",
        accentHex = 0xFF38BDF8
    ),
    MULTIPLICATION(
        title = "Multiplication",
        symbol = "×",
        subtitle = "Times tables & power strikes",
        accentHex = 0xFFFBBF24
    ),
    DIVISION(
        title = "Division",
        symbol = "÷",
        subtitle = "Exact integer quotients",
        accentHex = 0xFFC084FC
    ),
    MIXTURE(
        title = "Mixture",
        symbol = "±×÷",
        subtitle = "All four operations combined",
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
    val operandA: Int,
    val operandB: Int,
    val operation: MathOperation,
    val correctAnswer: Int,
    val choices: List<Int>
) {
    val displayEquation: String
        get() = "$operandA ${operation.symbol} $operandB = ?"
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
    val answerValue: Int,
    val isCorrect: Boolean,
    val normX: Float,
    val normY: Float,
    val bobPhase: Float,
    val fireFlashProgress: Float = 0f,
    val wrongTried: Boolean = false
)

data class Projectile(
    val id: Int,
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float,
    val progress: Float = 0f,
    val isPlayerShot: Boolean,
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
    SHIELD_HIT,
    SHIELD_BREAK,
    CASTLE_HIT,
    SHIELD_RESTORE,
    TIMER_TICK,
    GAME_OVER
}

data class GameUiState(
    val screen: AppScreen = AppScreen.MAIN_MENU,
    val category: MathCategory = MathCategory.MIXTURE,
    val wave: Int = 1,
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
    val currentProblem: MathProblem? = null,
    val attackers: List<AttackerEntity> = emptyList(),
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
