package com.safaboy.mathdefence.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.safaboy.mathdefence.game.AttackerEntity
import com.safaboy.mathdefence.game.AttackerType
import com.safaboy.mathdefence.game.BossComponent
import com.safaboy.mathdefence.game.BossEntity
import com.safaboy.mathdefence.game.GameEngine
import com.safaboy.mathdefence.game.GameUiState
import com.safaboy.mathdefence.game.Projectile
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private data class StarPoint(val x: Float, val y: Float, val r: Float, val phase: Float)

@Composable
fun BattlefieldCanvas(
    state: GameUiState,
    onTargetTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentState by rememberUpdatedState(state)
    val currentOnTap by rememberUpdatedState(onTargetTapped)

    val stars = remember {
        List(52) { idx ->
            val sx = ((idx * 73 + 19) % 100) / 100f
            val sy = ((idx * 41 + 7) % 68) / 100f
            val r = 1.2f + (idx % 3) * 0.9f
            StarPoint(sx, sy, r, idx * 0.7f)
        }
    }

    val answerPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(215, 226, 232, 240)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val equationPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val timerPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(235, 186, 230, 253)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    val popupPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val st = currentState
                    if (st.isPaused || st.isGameOver) return@detectTapGestures
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)

                    val boss = st.activeBoss
                    if (boss != null) {
                        val bossBobY = sin(st.elapsedAnimSec * 1.5f) * h * 0.008f
                        val hitRadius = min(w, h) * 0.145f
                        val hitComponent = boss.components
                            .filter { !it.isDestroyed }
                            .map { comp ->
                                val cx = comp.normX * w
                                val cy = comp.normY * h + bossBobY
                                comp to hypot(tapOffset.x - cx, tapOffset.y - cy)
                            }
                            .filter { it.second <= hitRadius }
                            .minByOrNull { it.second }
                            ?.first

                        if (hitComponent != null) {
                            currentOnTap(hitComponent.id)
                        }
                    } else {
                        val hitRadius = min(w, h) * 0.165f
                        val hit = st.attackers
                            .map { attacker ->
                                val bobY = sin(st.elapsedAnimSec * 2.4f + attacker.bobPhase) * h * 0.011f
                                val cx = attacker.normX * w
                                val cy = attacker.normY * h + bobY
                                attacker to hypot(tapOffset.x - cx, tapOffset.y - cy)
                            }
                            .filter { it.second <= hitRadius }
                            .minByOrNull { it.second }
                            ?.first

                        if (hit != null) {
                            currentOnTap(hit.id)
                        }
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val shakeMag = state.screenShake * 14f
        val shakeX = if (shakeMag > 0.1f) sin(state.elapsedAnimSec * 65f) * shakeMag else 0f
        val shakeY = if (shakeMag > 0.1f) cos(state.elapsedAnimSec * 58f) * shakeMag * 0.7f else 0f

        withTransform({
            translate(left = shakeX, top = shakeY)
        }) {
            drawSkyAndTerrain(w, h, stars, state.elapsedAnimSec, state.isBossBattle)
            drawCastle(w, h, state)
            drawShieldDome(w, h, state)
            drawShieldCore(w, h, state, equationPaint, timerPaint)

            val activeBoss = state.activeBoss
            if (activeBoss != null) {
                drawBossShip(
                    w = w,
                    h = h,
                    boss = activeBoss,
                    timeSec = state.elapsedAnimSec,
                    isPaused = state.isPaused,
                    answerPaint = answerPaint,
                    labelPaint = labelPaint
                )
            } else {
                for (attacker in state.attackers) {
                    drawAttacker(
                        w = w,
                        h = h,
                        attacker = attacker,
                        timeSec = state.elapsedAnimSec,
                        isPaused = state.isPaused,
                        answerPaint = answerPaint,
                        labelPaint = labelPaint
                    )
                }
            }

            // Draw projectiles
            for (proj in state.projectiles) {
                drawProjectile(w, h, proj)
            }

            // Draw particles
            for (p in state.particles) {
                val alpha = p.life.coerceIn(0f, 1f)
                drawCircle(
                    color = Color(p.colorArgb).copy(alpha = alpha),
                    radius = p.radius * (0.4f + 0.6f * alpha),
                    center = Offset(p.x * w, p.y * h)
                )
            }

            // Draw floating popups
            for (popup in state.popups) {
                val alpha = (popup.life.coerceIn(0f, 1f) * 255).toInt()
                popupPaint.textSize = (w * 0.042f).coerceIn(26f, 48f)
                popupPaint.color = android.graphics.Color.argb(
                    alpha,
                    ((popup.colorArgb shr 16) and 0xFF).toInt(),
                    ((popup.colorArgb shr 8) and 0xFF).toInt(),
                    (popup.colorArgb and 0xFF).toInt()
                )
                popupPaint.setShadowLayer(8f, 0f, 2f, android.graphics.Color.argb(alpha, 0, 0, 0))
                drawContext.canvas.nativeCanvas.drawText(
                    popup.text,
                    popup.normX * w,
                    popup.normY * h,
                    popupPaint
                )
            }
        }
    }
}

private fun DrawScope.drawSkyAndTerrain(
    w: Float,
    h: Float,
    stars: List<StarPoint>,
    timeSec: Float,
    isBossBattle: Boolean
) {
    val skyColors = if (isBossBattle) {
        listOf(
            Color(0xFF14060E),
            Color(0xFF240B1C),
            Color(0xFF1C1030),
            Color(0xFF0B1120)
        )
    } else {
        listOf(
            Color(0xFF060914),
            Color(0xFF0E1730),
            Color(0xFF162242),
            Color(0xFF0B1120)
        )
    }
    drawRect(brush = Brush.verticalGradient(colors = skyColors))

    // Twinkling stars
    for (star in stars) {
        val twinkle = 0.35f + 0.65f * ((sin(timeSec * 2.2f + star.phase) + 1f) * 0.5f)
        drawCircle(
            color = Color(0xFFE0F2FE).copy(alpha = twinkle * 0.75f),
            radius = star.r,
            center = Offset(star.x * w, star.y * h)
        )
    }

    // Subtle distant mountains behind the castle
    val mountainPath = Path().apply {
        moveTo(0f, h * 0.84f)
        lineTo(w * 0.14f, h * 0.71f)
        lineTo(w * 0.28f, h * 0.79f)
        lineTo(w * 0.44f, h * 0.69f)
        lineTo(w * 0.62f, h * 0.78f)
        lineTo(w * 0.80f, h * 0.68f)
        lineTo(w, h * 0.81f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(mountainPath, color = Color(0xFF0B1426))

    // Citadel ground plateau
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF152038), Color(0xFF0A0F1D)),
            startY = h * 0.86f,
            endY = h
        ),
        topLeft = Offset(0f, h * 0.86f),
        size = Size(w, h * 0.14f)
    )
}

private fun DrawScope.drawBossShip(
    w: Float,
    h: Float,
    boss: BossEntity,
    timeSec: Float,
    isPaused: Boolean,
    answerPaint: Paint,
    labelPaint: Paint
) {
    val bobY = sin(timeSec * 1.5f) * h * 0.008f
    val hullPrimary = Color(boss.archetype.hullPrimary)
    val hullSecondary = Color(boss.archetype.hullSecondary)
    val accent = Color(boss.archetype.accentColor)
    val laser = Color(boss.archetype.laserColor)

    val centerX = w * 0.50f
    val centerY = h * 0.285f + bobY

    // Menacing boss aura glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(hullPrimary.copy(alpha = 0.28f), Color.Transparent),
            center = Offset(centerX, centerY),
            radius = w * 0.56f
        ),
        radius = w * 0.56f,
        center = Offset(centerX, centerY)
    )

    // Thruster flames at top of dreadnought
    val flameFlicker = 0.82f + 0.18f * sin(timeSec * 22f)
    for (tx in listOf(0.26f, 0.74f)) {
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, laser.copy(alpha = 0.75f), Color.White),
                startY = h * 0.04f + bobY,
                endY = h * 0.13f + bobY
            ),
            topLeft = Offset(w * tx - w * 0.045f, h * 0.055f + bobY),
            size = Size(w * 0.09f, h * 0.075f * flameFlicker)
        )
    }

    // Massive Dreadnought Main Hull Silhouette
    val hullPath = Path().apply {
        moveTo(w * 0.50f, h * 0.08f + bobY)
        lineTo(w * 0.70f, h * 0.12f + bobY)
        lineTo(w * 0.94f, h * 0.20f + bobY)
        lineTo(w * 0.90f, h * 0.44f + bobY)
        lineTo(w * 0.68f, h * 0.47f + bobY)
        lineTo(w * 0.50f, h * 0.43f + bobY)
        lineTo(w * 0.32f, h * 0.47f + bobY)
        lineTo(w * 0.10f, h * 0.44f + bobY)
        lineTo(w * 0.06f, h * 0.20f + bobY)
        lineTo(w * 0.30f, h * 0.12f + bobY)
        close()
    }
    drawPath(
        path = hullPath,
        brush = Brush.verticalGradient(
            colors = listOf(hullPrimary, hullSecondary),
            startY = h * 0.08f + bobY,
            endY = h * 0.48f + bobY
        )
    )
    drawPath(
        path = hullPath,
        color = if (boss.hitFlashProgress > 0.1f) Color.White else accent.copy(alpha = 0.75f),
        style = Stroke(width = 4.5f)
    )

    // Structural armored struts connecting components to the boss spine
    for (comp in boss.components) {
        val cx = comp.normX * w
        val cy = comp.normY * h + bobY
        drawLine(
            color = if (comp.isDestroyed) Color(0xFF1E293B) else accent.copy(alpha = 0.35f),
            start = Offset(centerX, centerY),
            end = Offset(cx, cy),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
    }

    // Draw each modular boss component
    for (comp in boss.components) {
        drawBossComponent(
            w = w,
            h = h,
            boss = boss,
            component = comp,
            bobY = bobY,
            timeSec = timeSec,
            isPaused = isPaused,
            answerPaint = answerPaint,
            labelPaint = labelPaint
        )
    }
}

private fun DrawScope.drawBossComponent(
    w: Float,
    h: Float,
    boss: BossEntity,
    component: BossComponent,
    bobY: Float,
    timeSec: Float,
    isPaused: Boolean,
    answerPaint: Paint,
    labelPaint: Paint
) {
    val recoilY = -component.fireFlashProgress * h * 0.016f
    val cx = component.normX * w
    val cy = component.normY * h + bobY + recoilY
    val r = min(w, h) * 0.108f

    val accent = Color(boss.archetype.accentColor)
    val laser = Color(boss.archetype.laserColor)

    if (component.isDestroyed) {
        // Charred, destroyed component crater (unusable by boss)
        drawCircle(
            color = Color(0xFF0A0F1D).copy(alpha = 0.92f),
            radius = r * 0.88f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFF334155),
            radius = r * 0.88f,
            center = Offset(cx, cy),
            style = Stroke(width = 3.5f)
        )
        // Crossed-out wreckage X
        val xSpan = r * 0.42f
        drawLine(
            color = Color(0xFFEF4444).copy(alpha = 0.65f),
            start = Offset(cx - xSpan, cy - xSpan),
            end = Offset(cx + xSpan, cy + xSpan),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFEF4444).copy(alpha = 0.65f),
            start = Offset(cx + xSpan, cy - xSpan),
            end = Offset(cx - xSpan, cy + xSpan),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
        // Flickering damage spark
        val sparkAlpha = 0.35f + 0.65f * ((sin(timeSec * 14f + component.id * 2f) + 1f) * 0.5f)
        drawCircle(
            color = Color(0xFFF97316).copy(alpha = sparkAlpha * 0.7f),
            radius = r * 0.14f,
            center = Offset(cx + r * 0.25f, cy - r * 0.22f)
        )
        labelPaint.textSize = (r * 0.22f).coerceIn(16f, 22f)
        labelPaint.color = android.graphics.Color.argb(150, 148, 163, 184)
        drawContext.canvas.nativeCanvas.drawText(
            "OFFLINE",
            cx,
            cy + r * 1.15f,
            labelPaint
        )
        return
    }

    // Aim heavy weapon barrel at castle
    val targetX = w * GameEngine.CASTLE_CORE_X
    val targetY = h * GameEngine.SHIELD_IMPACT_Y
    val angleRad = atan2((targetY - cy).toDouble(), (targetX - cx).toDouble()).toFloat()
    val angleDeg = (angleRad * 180f / PI.toFloat()) - 90f

    withTransform({
        rotate(degrees = angleDeg, pivot = Offset(cx, cy))
    }) {
        // Heavy twin/thick boss weapon barrel
        val barrelW = r * 0.30f
        val barrelLen = r * 1.05f
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(cx - barrelW / 2f, cy + r * 0.30f),
            size = Size(barrelW, barrelLen * 0.78f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawCircle(
            color = if (component.fireFlashProgress > 0f) Color.White else laser,
            radius = barrelW * (0.50f + component.fireFlashProgress * 0.85f),
            center = Offset(cx, cy + r * 0.30f + barrelLen * 0.76f)
        )
        if (component.fireFlashProgress > 0.05f) {
            drawCircle(
                color = laser.copy(alpha = component.fireFlashProgress * 0.85f),
                radius = r * 0.72f * component.fireFlashProgress,
                center = Offset(cx, cy + r * 0.30f + barrelLen * 0.80f)
            )
        }
    }

    // Armored hexagonal component pod
    val hex = Path().apply {
        for (i in 0 until 6) {
            val a = (i * 60f - 30f) * PI.toFloat() / 180f
            val px = cx + cos(a) * r * 0.98f
            val py = cy + sin(a) * r * 0.94f
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(hex, color = Color(0xFF1E293B))
    drawPath(
        hex,
        color = if (component.wrongTried) Color(0xFFEF4444) else accent,
        style = Stroke(width = if (component.wrongTried) 6f else 4f)
    )

    // Inner answer core circle
    val badgeRadius = r * 0.68f
    drawCircle(
        color = Color(0xFF070C1B),
        radius = badgeRadius,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = if (component.wrongTried) Color(0xFFEF4444) else laser.copy(alpha = 0.85f),
        radius = badgeRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 3.5f)
    )

    // Answer text inside component (masked while paused)
    val ansStr = if (isPaused) "?" else component.answerValue
    answerPaint.textSize = when {
        ansStr.length >= 5 -> badgeRadius * 0.60f
        ansStr.length == 4 -> badgeRadius * 0.72f
        ansStr.length == 3 -> badgeRadius * 0.84f
        else -> badgeRadius * 0.98f
    }
    answerPaint.color = if (component.wrongTried) {
        android.graphics.Color.rgb(252, 165, 165)
    } else {
        android.graphics.Color.WHITE
    }
    drawContext.canvas.nativeCanvas.drawText(
        ansStr,
        cx,
        cy + answerPaint.textSize * 0.35f,
        answerPaint
    )

    // Weapon label below component
    labelPaint.textSize = (r * 0.22f).coerceIn(16f, 22f)
    labelPaint.color = android.graphics.Color.argb(225, 254, 240, 138)
    drawContext.canvas.nativeCanvas.drawText(
        component.componentType.shortLabel.uppercase(),
        cx,
        cy - r * 1.03f,
        labelPaint
    )
}

private fun DrawScope.drawCastle(w: Float, h: Float, state: GameUiState) {
    val castleLeft = w * 0.11f
    val castleRight = w * 0.89f
    val castleWidth = castleRight - castleLeft
    val wallTop = h * 0.76f
    val groundY = h * 0.94f

    val hitTint = state.castleDamageFlash
    val stoneLight = Color(
        red = (0.28f + hitTint * 0.55f).coerceAtMost(1f),
        green = (0.34f * (1f - hitTint * 0.5f)),
        blue = (0.46f * (1f - hitTint * 0.5f))
    )
    val stoneDark = Color(
        red = (0.16f + hitTint * 0.45f).coerceAtMost(1f),
        green = (0.21f * (1f - hitTint * 0.5f)),
        blue = (0.31f * (1f - hitTint * 0.5f))
    )

    // Left & Right Watchtowers
    val towerW = castleWidth * 0.17f
    val towerTop = h * 0.69f
    drawRect(
        brush = Brush.verticalGradient(listOf(stoneLight, stoneDark), towerTop, groundY),
        topLeft = Offset(castleLeft, towerTop),
        size = Size(towerW, groundY - towerTop)
    )
    drawRect(
        brush = Brush.verticalGradient(listOf(stoneLight, stoneDark), towerTop, groundY),
        topLeft = Offset(castleRight - towerW, towerTop),
        size = Size(towerW, groundY - towerTop)
    )

    // Tower spires / battlements
    for (tx in listOf(castleLeft, castleRight - towerW)) {
        val merlonW = towerW / 3.5f
        for (m in 0..2) {
            drawRect(
                color = stoneLight,
                topLeft = Offset(tx + m * (merlonW * 1.25f), towerTop - h * 0.022f),
                size = Size(merlonW, h * 0.024f)
            )
        }
        drawRoundRect(
            color = if (state.shieldHp > 0) Color(0xFF38BDF8) else Color(0xFFF97316),
            topLeft = Offset(tx + towerW * 0.36f, towerTop + h * 0.045f),
            size = Size(towerW * 0.28f, h * 0.045f),
            cornerRadius = CornerRadius(8f, 8f)
        )
    }

    // Main Fortress Curtain Wall
    drawRect(
        brush = Brush.verticalGradient(listOf(stoneLight, stoneDark), wallTop, groundY),
        topLeft = Offset(castleLeft + towerW * 0.85f, wallTop),
        size = Size(castleWidth - towerW * 1.7f, groundY - wallTop)
    )

    // Curtain wall crenellations
    val wallInnerLeft = castleLeft + towerW
    val wallInnerWidth = castleWidth - towerW * 2f
    val crenelCount = 7
    val crenelStep = wallInnerWidth / crenelCount
    for (i in 0 until crenelCount) {
        drawRect(
            color = stoneLight,
            topLeft = Offset(wallInnerLeft + i * crenelStep + crenelStep * 0.18f, wallTop - h * 0.018f),
            size = Size(crenelStep * 0.64f, h * 0.02f)
        )
    }

    // Castle damage cracks when castle HP is worn down
    if (state.castleFraction < 0.85f) {
        val crackAlpha = ((1f - state.castleFraction) * 1.15f).coerceIn(0.25f, 0.95f)
        val crackColor = Color(0xFFFF6B35).copy(alpha = crackAlpha)
        val crack1 = Path().apply {
            moveTo(w * 0.26f, wallTop + h * 0.02f)
            lineTo(w * 0.29f, wallTop + h * 0.06f)
            lineTo(w * 0.27f, wallTop + h * 0.10f)
        }
        drawPath(crack1, color = crackColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
    if (state.castleFraction < 0.50f) {
        val crack2 = Path().apply {
            moveTo(w * 0.74f, wallTop + h * 0.01f)
            lineTo(w * 0.71f, wallTop + h * 0.07f)
            lineTo(w * 0.75f, wallTop + h * 0.12f)
        }
        drawPath(crack2, color = Color(0xFFEF4444), style = Stroke(width = 5f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawShieldDome(w: Float, h: Float, state: GameUiState) {
    val domeLeft = w * 0.05f
    val domeTop = h * 0.60f
    val domeWidth = w * 0.90f
    val domeHeight = h * 0.44f

    if (state.shieldHp > 0) {
        val baseColor = when {
            state.shieldFraction > 0.5f -> Color(0xFF22D3EE)
            state.shieldFraction > 0.25f -> Color(0xFFFBBF24)
            else -> Color(0xFFFB7185)
        }
        val pulse = 0.08f * sin(state.elapsedAnimSec * 4.2f)
        val rippleBoost = state.shieldRipple * 0.45f

        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = (0.04f + rippleBoost * 0.35f).coerceAtMost(0.45f)),
                    baseColor.copy(alpha = (0.20f + pulse + rippleBoost * 0.45f).coerceAtMost(0.65f))
                ),
                center = Offset(w * 0.5f, h * 0.84f),
                radius = w * 0.52f
            ),
            startAngle = 185f,
            sweepAngle = 170f,
            useCenter = false,
            topLeft = Offset(domeLeft, domeTop),
            size = Size(domeWidth, domeHeight)
        )

        drawArc(
            color = baseColor.copy(alpha = (0.78f + rippleBoost * 0.22f).coerceAtMost(1f)),
            startAngle = 185f,
            sweepAngle = 170f,
            useCenter = false,
            topLeft = Offset(domeLeft, domeTop),
            size = Size(domeWidth, domeHeight),
            style = Stroke(width = 6f + state.shieldRipple * 7f, cap = StrokeCap.Round)
        )

        drawArc(
            color = Color.White.copy(alpha = 0.35f + rippleBoost * 0.45f),
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(domeLeft + 10f, domeTop + 10f),
            size = Size(domeWidth - 20f, domeHeight - 20f),
            style = Stroke(width = 2f)
        )

        if (state.shieldRipple > 0.02f) {
            val expand = (1f - state.shieldRipple) * w * 0.32f
            drawCircle(
                color = baseColor.copy(alpha = state.shieldRipple * 0.85f),
                radius = 18f + expand,
                center = Offset(w * GameEngine.CASTLE_CORE_X, h * GameEngine.SHIELD_IMPACT_Y),
                style = Stroke(width = 4f * state.shieldRipple)
            )
        }
    } else {
        val flicker = if (sin(state.elapsedAnimSec * 18f) > 0.2f) 0.35f else 0.12f
        drawArc(
            color = Color(0xFFEF4444).copy(alpha = flicker),
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(domeLeft, domeTop),
            size = Size(domeWidth, domeHeight),
            style = Stroke(width = 3f)
        )
    }
}

private fun DrawScope.drawShieldCore(
    w: Float,
    h: Float,
    state: GameUiState,
    equationPaint: Paint,
    timerPaint: Paint
) {
    val cx = w * GameEngine.CASTLE_CORE_X
    val cy = h * GameEngine.CASTLE_CORE_Y
    val coreRadius = min(w * 0.28f, h * 0.138f)

    val timerColor = when {
        state.timerFraction > 0.50f -> Color(0xFF22D3EE)
        state.timerFraction > 0.25f -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                timerColor.copy(alpha = 0.35f + state.coreChargeFlash * 0.35f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = coreRadius * 1.45f
        ),
        radius = coreRadius * 1.45f,
        center = Offset(cx, cy)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF14284B), Color(0xFF0A1429)),
            center = Offset(cx, cy),
            radius = coreRadius
        ),
        radius = coreRadius,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = Color(0xFF1E3A5F),
        radius = coreRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 10f)
    )

    drawArc(
        color = timerColor,
        startAngle = -90f,
        sweepAngle = 360f * state.timerFraction,
        useCenter = false,
        topLeft = Offset(cx - coreRadius, cy - coreRadius),
        size = Size(coreRadius * 2f, coreRadius * 2f),
        style = Stroke(width = 11f, cap = StrokeCap.Round)
    )

    timerPaint.textSize = (coreRadius * 0.18f).coerceIn(19f, 30f)
    timerPaint.color = android.graphics.Color.argb(210, 125, 211, 252)
    drawContext.canvas.nativeCanvas.drawText(
        if (state.isBossBattle) "BOSS LOCK-ON" else "SHIELD CORE",
        cx,
        cy - coreRadius * 0.44f,
        timerPaint
    )

    // Equation inside Shield Core (hidden while paused to prevent pause-cheating)
    val equation = if (state.isPaused) "PAUSED" else (state.currentProblem?.displayEquation ?: "")
    val baseSize = (coreRadius * 0.40f).coerceIn(30f, 60f)
    equationPaint.textSize = when {
        equation.length >= 14 -> baseSize * 0.66f
        equation.length >= 11 -> baseSize * 0.78f
        else -> baseSize
    }
    equationPaint.setShadowLayer(10f, 0f, 0f, android.graphics.Color.argb(180, 34, 211, 238))
    drawContext.canvas.nativeCanvas.drawText(
        equation,
        cx,
        cy + equationPaint.textSize * 0.34f,
        equationPaint
    )

    val secText = String.format("%.1fs", state.remainingTimeMs / 1000f)
    timerPaint.textSize = (coreRadius * 0.23f).coerceIn(22f, 36f)
    timerPaint.color = when {
        state.timerFraction > 0.50f -> android.graphics.Color.rgb(186, 230, 253)
        state.timerFraction > 0.25f -> android.graphics.Color.rgb(253, 224, 71)
        else -> android.graphics.Color.rgb(252, 165, 165)
    }
    drawContext.canvas.nativeCanvas.drawText(
        secText,
        cx,
        cy + coreRadius * 0.66f,
        timerPaint
    )
}

private fun DrawScope.drawAttacker(
    w: Float,
    h: Float,
    attacker: AttackerEntity,
    timeSec: Float,
    isPaused: Boolean,
    answerPaint: Paint,
    labelPaint: Paint
) {
    val bobOffset = sin(timeSec * 2.4f + attacker.bobPhase) * h * 0.011f
    val recoilY = -attacker.fireFlashProgress * h * 0.018f
    val cx = attacker.normX * w
    val cy = attacker.normY * h + bobOffset + recoilY
    val r = min(w, h) * 0.115f

    val primary = Color(attacker.type.primaryColor)
    val accent = Color(attacker.type.accentColor)
    val laser = Color(attacker.type.laserColor)

    val targetX = w * GameEngine.CASTLE_CORE_X
    val targetY = h * GameEngine.SHIELD_IMPACT_Y
    val angleRad = atan2((targetY - cy).toDouble(), (targetX - cx).toDouble()).toFloat()
    val angleDeg = (angleRad * 180f / PI.toFloat()) - 90f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.28f), Color.Transparent),
            center = Offset(cx, cy),
            radius = r * 1.45f
        ),
        radius = r * 1.45f,
        center = Offset(cx, cy)
    )

    withTransform({
        rotate(degrees = angleDeg, pivot = Offset(cx, cy))
    }) {
        val barrelW = r * 0.24f
        val barrelLen = r * 0.95f
        drawRoundRect(
            color = Color(0xFF334155),
            topLeft = Offset(cx - barrelW / 2f, cy + r * 0.35f),
            size = Size(barrelW, barrelLen * 0.72f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawCircle(
            color = if (attacker.fireFlashProgress > 0f) Color.White else laser,
            radius = barrelW * (0.42f + attacker.fireFlashProgress * 0.65f),
            center = Offset(cx, cy + r * 0.35f + barrelLen * 0.70f)
        )
        if (attacker.fireFlashProgress > 0.05f) {
            drawCircle(
                color = laser.copy(alpha = attacker.fireFlashProgress * 0.8f),
                radius = r * 0.55f * attacker.fireFlashProgress,
                center = Offset(cx, cy + r * 0.35f + barrelLen * 0.75f)
            )
        }
    }

    when (attacker.type) {
        AttackerType.BLASTER_BOT -> {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.65f)), cy - r, cy + r),
                topLeft = Offset(cx - r * 0.92f, cy - r * 0.78f),
                size = Size(r * 1.84f, r * 1.56f),
                cornerRadius = CornerRadius(r * 0.28f, r * 0.28f)
            )
            drawRect(color = accent, topLeft = Offset(cx - r * 1.08f, cy - r * 0.38f), size = Size(r * 0.22f, r * 0.72f))
            drawRect(color = accent, topLeft = Offset(cx + r * 0.86f, cy - r * 0.38f), size = Size(r * 0.22f, r * 0.72f))
            drawLine(accent, Offset(cx - r * 0.45f, cy - r * 0.78f), Offset(cx - r * 0.6f, cy - r * 1.08f), strokeWidth = 5f)
            drawLine(accent, Offset(cx + r * 0.45f, cy - r * 0.78f), Offset(cx + r * 0.6f, cy - r * 1.08f), strokeWidth = 5f)
        }
        AttackerType.HOVER_DRONE -> {
            drawOval(
                color = primary.copy(alpha = 0.88f),
                topLeft = Offset(cx - r * 1.25f, cy - r * 0.42f),
                size = Size(r * 2.5f, r * 0.95f)
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(accent, primary), Offset(cx, cy - r * 0.2f), r * 0.9f),
                radius = r * 0.78f,
                center = Offset(cx, cy)
            )
        }
        AttackerType.SIEGE_MECH -> {
            val hex = Path().apply {
                for (i in 0 until 6) {
                    val a = (i * 60f - 30f) * PI.toFloat() / 180f
                    val px = cx + cos(a) * r * 1.02f
                    val py = cy + sin(a) * r * 0.92f
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(hex, color = primary)
            drawPath(hex, color = accent, style = Stroke(width = 4f))
        }
        AttackerType.VOID_STRIKER -> {
            val ship = Path().apply {
                moveTo(cx, cy + r * 0.95f)
                lineTo(cx - r * 1.22f, cy - r * 0.45f)
                lineTo(cx - r * 0.48f, cy - r * 0.92f)
                lineTo(cx + r * 0.48f, cy - r * 0.92f)
                lineTo(cx + r * 1.22f, cy - r * 0.45f)
                close()
            }
            drawPath(ship, color = primary)
            drawPath(ship, color = accent, style = Stroke(width = 3.5f))
        }
        AttackerType.PLASMA_TANK -> {
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(cx - r * 1.15f, cy - r * 0.65f),
                size = Size(r * 2.3f, r * 1.3f),
                cornerRadius = CornerRadius(r * 0.35f, r * 0.35f)
            )
            drawRoundRect(
                color = primary,
                topLeft = Offset(cx - r * 0.92f, cy - r * 0.76f),
                size = Size(r * 1.84f, r * 1.52f),
                cornerRadius = CornerRadius(r * 0.30f, r * 0.30f)
            )
            drawLine(accent, Offset(cx - r * 1.02f, cy - r * 0.4f), Offset(cx - r * 1.02f, cy + r * 0.4f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(accent, Offset(cx + r * 1.02f, cy - r * 0.4f), Offset(cx + r * 1.02f, cy + r * 0.4f), strokeWidth = 6f, cap = StrokeCap.Round)
        }
        AttackerType.ARTILLERY_ORB -> {
            drawCircle(
                color = accent.copy(alpha = 0.65f),
                radius = r * 1.08f,
                center = Offset(cx, cy),
                style = Stroke(width = 4f)
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(accent, primary), Offset(cx - r * 0.25f, cy - r * 0.25f), r),
                radius = r * 0.88f,
                center = Offset(cx, cy)
            )
        }
    }

    val badgeRadius = r * 0.64f
    drawCircle(
        color = Color(0xFF0A1022).copy(alpha = 0.93f),
        radius = badgeRadius,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = if (attacker.wrongTried) Color(0xFFEF4444) else accent,
        radius = badgeRadius,
        center = Offset(cx, cy),
        style = Stroke(width = if (attacker.wrongTried) 6f else 4f)
    )

    val ansStr = if (isPaused) "?" else attacker.answerValue
    answerPaint.textSize = when {
        ansStr.length >= 5 -> badgeRadius * 0.60f
        ansStr.length == 4 -> badgeRadius * 0.72f
        ansStr.length == 3 -> badgeRadius * 0.84f
        else -> badgeRadius * 0.98f
    }
    answerPaint.color = if (attacker.wrongTried) {
        android.graphics.Color.rgb(252, 165, 165)
    } else {
        android.graphics.Color.WHITE
    }
    drawContext.canvas.nativeCanvas.drawText(
        ansStr,
        cx,
        cy + answerPaint.textSize * 0.35f,
        answerPaint
    )

    labelPaint.textSize = (r * 0.22f).coerceIn(18f, 26f)
    labelPaint.color = android.graphics.Color.argb(215, 226, 232, 240)
    drawContext.canvas.nativeCanvas.drawText(
        attacker.type.displayName.uppercase(),
        cx,
        cy - r * 1.05f,
        labelPaint
    )
}

private fun DrawScope.drawProjectile(w: Float, h: Float, proj: Projectile) {
    val start = Offset(proj.fromX * w, proj.fromY * h)
    val end = Offset(proj.toX * w, proj.toY * h)
    val headT = proj.progress.coerceIn(0f, 1f)
    val tailT = (headT - if (proj.isPlayerShot) 0.45f else if (proj.isBossShot) 0.36f else 0.26f).coerceAtLeast(0f)

    val head = Offset(
        start.x + (end.x - start.x) * headT,
        start.y + (end.y - start.y) * headT
    )
    val tail = Offset(
        start.x + (end.x - start.x) * tailT,
        start.y + (end.y - start.y) * tailT
    )

    val color = Color(proj.colorArgb)
    val outerWidth = when {
        proj.isBossShot -> 28f
        proj.isPlayerShot -> 20f
        else -> 15f
    }
    val innerWidth = when {
        proj.isBossShot -> 11f
        proj.isPlayerShot -> 7f
        else -> 5f
    }
    drawLine(
        color = color.copy(alpha = 0.60f),
        start = tail,
        end = head,
        strokeWidth = outerWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White,
        start = tail,
        end = head,
        strokeWidth = innerWidth,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = color,
        radius = if (proj.isBossShot) 18f else if (proj.isPlayerShot) 14f else 11f,
        center = head
    )
}
