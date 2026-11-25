package com.safaboy.mathdefence.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safaboy.mathdefence.game.MathCategory
import com.safaboy.mathdefence.ui.theme.CardSlate
import com.safaboy.mathdefence.ui.theme.DeepSlate
import com.safaboy.mathdefence.ui.theme.MidnightBg
import com.safaboy.mathdefence.ui.theme.ShieldCyan
import com.safaboy.mathdefence.ui.theme.TextMuted
import com.safaboy.mathdefence.ui.theme.TextPrimary

@Composable
fun MainMenuScreen(
    highScores: Map<MathCategory, Int>,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onSelectCategory: (MathCategory) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF060A16), Color(0xFF0F1B36), MidnightBg)
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with Sound toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = CardSlate.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, ShieldCyan.copy(alpha = 0.35f))
                ) {
                    IconButton(onClick = onToggleSound) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (soundEnabled) "Mute sound" else "Enable sound",
                            tint = if (soundEnabled) ShieldCyan else TextMuted
                        )
                    }
                }
            }

            // Animated Shield Emblem
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ShieldCyan.copy(alpha = 0.32f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = (size.minDimension / 2f) * pulse
                        ),
                        radius = (size.minDimension / 2f) * pulse,
                        center = center
                    )
                    drawCircle(
                        color = ShieldCyan.copy(alpha = 0.65f),
                        radius = size.minDimension * 0.36f,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = ShieldCyan,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "MATH DEFENCE",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Text(
                text = "Power your Shield Core. Blast the right attacker before time runs out.",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp, start = 12.dp, end = 12.dp)
            )

            // Category selection heading
            Text(
                text = "SELECT MATH CATEGORY",
                color = ShieldCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )

            MathCategory.entries.forEach { category ->
                CategoryCard(
                    category = category,
                    bestScore = highScores[category] ?: 0,
                    onClick = { onSelectCategory(category) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Brief rules card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSlate.copy(alpha = 0.9f)),
                border = BorderStroke(1.dp, Color(0xFF233252))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BATTLEFIELD BRIEFING",
                        color = ShieldCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Solve the equation inside your Shield Core and tap the attacker holding the right answer.\n" +
                            "• Pick wrong or run out of time and attackers blast your shields—then your castle hull!\n" +
                            "• Every 10–20 waves, a multi-component Boss Dreadnought arrives with heavy weapons! Destroy its components to advance to the next Skill Level.\n" +
                            "• Only 3 pauses allowed per game—no pausing to solve sums!",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategoryCard(
    category: MathCategory,
    bestScore: Int,
    onClick: () -> Unit
) {
    val accent = Color(category.accentHex)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.symbol,
                    color = accent,
                    fontSize = if (category.symbol.length > 1) 17.sp else 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = category.subtitle,
                    color = TextMuted,
                    fontSize = 12.sp
                )
                if (bestScore > 0) {
                    Text(
                        text = "BEST: $bestScore",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play ${category.title}",
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
