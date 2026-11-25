package com.safaboy.mathdefence.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safaboy.mathdefence.game.BossEntity
import com.safaboy.mathdefence.game.GameUiState
import com.safaboy.mathdefence.ui.components.BattlefieldCanvas
import com.safaboy.mathdefence.ui.theme.AmberWarn
import com.safaboy.mathdefence.ui.theme.CardSlate
import com.safaboy.mathdefence.ui.theme.CrimsonDanger
import com.safaboy.mathdefence.ui.theme.DeepSlate
import com.safaboy.mathdefence.ui.theme.EmeraldSafe
import com.safaboy.mathdefence.ui.theme.MidnightBg
import com.safaboy.mathdefence.ui.theme.ShieldCyan
import com.safaboy.mathdefence.ui.theme.TextMuted
import com.safaboy.mathdefence.ui.theme.TextPrimary

@Composable
fun GameScreen(
    state: GameUiState,
    onTargetTapped: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onToggleSound: () -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top HUD bar
            TopHudBar(
                state = state,
                onToggleSound = onToggleSound,
                onTogglePause = onTogglePause
            )

            // Main Interactive Battlefield Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                BattlefieldCanvas(
                    state = state,
                    onTargetTapped = onTargetTapped,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-of-battlefield overlays: Boss HP Bar + Event Banner
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val activeBoss = state.activeBoss
                    if (activeBoss != null) {
                        BossHealthHud(boss = activeBoss)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    val bannerText = state.statusBanner
                    if (bannerText != null && state.statusBannerLife > 0.05f) {
                        val isWarning = bannerText.contains("DEPLETED", ignoreCase = true) ||
                            bannerText.contains("DESTROYED", ignoreCase = true) ||
                            bannerText.contains("NO PAUSES", ignoreCase = true) ||
                            bannerText.contains("BOSS APPROACHING", ignoreCase = true)
                        Surface(
                            modifier = Modifier.alpha(state.statusBannerLife.coerceIn(0f, 1f)),
                            shape = RoundedCornerShape(20.dp),
                            color = if (isWarning) CrimsonDanger.copy(alpha = 0.92f) else CardSlate.copy(alpha = 0.92f),
                            border = BorderStroke(
                                1.dp,
                                if (isWarning) Color(0xFFFCA5A5) else ShieldCyan.copy(alpha = 0.7f)
                            )
                        ) {
                            Text(
                                text = bannerText,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.9.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Shield & Castle Integrity Bars
            DefenseStatusFooter(state = state)
        }

        // Opaque Pause Overlay (hides battlefield & enforces 3-pause limit)
        if (state.isPaused && !state.isGameOver) {
            PauseOverlay(
                categoryTitle = state.category.title,
                skillLevel = state.skillLevel,
                skillDesc = state.skillLevelDescription,
                wave = state.wave,
                score = state.score,
                pausesRemaining = state.pausesRemaining,
                maxPauses = state.maxPauses,
                onResume = onTogglePause,
                onRestart = onRestart,
                onExitToMenu = onExitToMenu
            )
        }

        // Game Over Overlay
        if (state.isGameOver) {
            GameOverOverlay(
                state = state,
                onPlayAgain = onRestart,
                onMainMenu = onExitToMenu
            )
        }
    }
}

@Composable
private fun BossHealthHud(boss: BossEntity) {
    val accent = Color(boss.archetype.accentColor)
    val laser = Color(boss.archetype.laserColor)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF140914).copy(alpha = 0.90f),
        border = BorderStroke(1.2.dp, laser.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = laser,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${boss.archetype.title.uppercase()}  •  LV ${boss.skillLevelTier} BOSS",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.9.sp
                    )
                }
                Text(
                    text = "${boss.remainingHits}/${boss.hitsRequired} HITS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { boss.healthFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = laser,
                trackColor = Color(0xFF2B1222)
            )
        }
    }
}

@Composable
private fun TopHudBar(
    state: GameUiState,
    onToggleSound: () -> Unit,
    onTogglePause: () -> Unit
) {
    Surface(
        color = DeepSlate.copy(alpha = 0.96f),
        border = BorderStroke(0.5.dp, Color(0xFF1F2D4D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Score, Wave & Skill Level
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = "SCORE",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${state.score}",
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CardSlate,
                        border = BorderStroke(1.dp, Color(state.category.accentHex).copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "W${state.wave} • LV ${state.skillLevel}",
                                color = Color(state.category.accentHex),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = state.skillLevelDescription,
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (state.streak >= 2) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AmberWarn.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, AmberWarn.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = AmberWarn,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "x${state.streak}",
                                    color = AmberWarn,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // Audio & Limited Pause button (with remaining count)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (state.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Toggle sound",
                            tint = if (state.soundEnabled) ShieldCyan else TextMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    val canPause = state.pausesRemaining > 0 || state.isPaused
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (canPause) CardSlate else Color(0xFF1A1420),
                        border = BorderStroke(
                            1.dp,
                            if (canPause) ShieldCyan.copy(alpha = 0.45f) else CrimsonDanger.copy(alpha = 0.45f)
                        ),
                        onClick = onTogglePause
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Pause (${state.pausesRemaining} left)",
                                tint = if (canPause) TextPrimary else TextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${state.pausesRemaining}/${state.maxPauses}",
                                color = if (state.pausesRemaining > 0) ShieldCyan else CrimsonDanger,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Boss countdown telemetry strip
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.category.title.uppercase(),
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (state.isBossBattle) {
                        "BOSS BATTLE ACTIVE • HEAVY WEAPONS DETECTED"
                    } else {
                        "NEXT BOSS: WAVE ${state.nextBossWave} (${state.wavesUntilBoss} LEFT)"
                    },
                    color = if (state.isBossBattle || state.wavesUntilBoss <= 2) AmberWarn else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.7.sp
                )
            }
        }
    }
}

@Composable
private fun DefenseStatusFooter(state: GameUiState) {
    val shieldColor = when {
        state.shieldHp <= 0 -> CrimsonDanger
        state.shieldFraction > 0.5f -> ShieldCyan
        state.shieldFraction > 0.25f -> AmberWarn
        else -> CrimsonDanger
    }

    val castleColor = when {
        state.castleFraction > 0.55f -> EmeraldSafe
        state.castleFraction > 0.25f -> AmberWarn
        else -> CrimsonDanger
    }

    Surface(
        color = DeepSlate.copy(alpha = 0.98f),
        border = BorderStroke(0.5.dp, Color(0xFF1F2D4D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusMeter(
                label = if (state.shieldHp > 0) "SHIELD" else "SHIELD DOWN",
                current = state.shieldHp,
                max = state.maxShieldHp,
                fraction = state.shieldFraction,
                color = shieldColor,
                icon = Icons.Default.Shield,
                modifier = Modifier.weight(1f)
            )
            StatusMeter(
                label = "CASTLE HP",
                current = state.castleHp,
                max = state.maxCastleHp,
                fraction = state.castleFraction,
                color = castleColor,
                icon = Icons.Default.Castle,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatusMeter(
    label: String,
    current: Int,
    max: Int,
    fraction: Float,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = "$current/$max",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = color,
            trackColor = Color(0xFF0A1022)
        )
    }
}

@Composable
private fun PauseOverlay(
    categoryTitle: String,
    skillLevel: Int,
    skillDesc: String,
    wave: Int,
    score: Int,
    pausesRemaining: Int,
    maxPauses: Int,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    // Fully opaque backdrop so players cannot inspect equations while paused
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBg.copy(alpha = 0.98f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlate),
            border = BorderStroke(1.5.dp, ShieldCyan.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DEFENCE PAUSED",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$categoryTitle • Wave $wave • Score $score",
                    color = TextMuted,
                    fontSize = 13.sp
                )
                Text(
                    text = "Skill Level $skillLevel ($skillDesc)",
                    color = ShieldCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (pausesRemaining > 0) CardSlate else CrimsonDanger.copy(alpha = 0.16f),
                    border = BorderStroke(
                        1.dp,
                        if (pausesRemaining > 0) AmberWarn.copy(alpha = 0.55f) else CrimsonDanger
                    )
                ) {
                    Text(
                        text = if (pausesRemaining > 0) {
                            "PAUSES REMAINING THIS GAME: $pausesRemaining / $maxPauses"
                        } else {
                            "LAST PAUSE USED (0 / $maxPauses REMAINING)"
                        },
                        color = if (pausesRemaining > 0) AmberWarn else Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShieldCyan,
                        contentColor = Color(0xFF04131C)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESUME", fontWeight = FontWeight.ExtraBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ShieldCyan.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = ShieldCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESTART GAME", color = TextPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onExitToMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f))
                ) {
                    Text("MAIN MENU", color = TextMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
