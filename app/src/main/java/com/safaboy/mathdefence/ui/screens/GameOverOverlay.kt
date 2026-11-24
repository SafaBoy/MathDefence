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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safaboy.mathdefence.game.GameUiState
import com.safaboy.mathdefence.ui.theme.AmberWarn
import com.safaboy.mathdefence.ui.theme.CardSlate
import com.safaboy.mathdefence.ui.theme.CrimsonDanger
import com.safaboy.mathdefence.ui.theme.DeepSlate
import com.safaboy.mathdefence.ui.theme.EmeraldSafe
import com.safaboy.mathdefence.ui.theme.ShieldCyan
import com.safaboy.mathdefence.ui.theme.TextMuted
import com.safaboy.mathdefence.ui.theme.TextPrimary

@Composable
fun GameOverOverlay(
    state: GameUiState,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlate),
            border = BorderStroke(1.5.dp, CrimsonDanger.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CASTLE FALLEN",
                    color = CrimsonDanger,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.6.sp
                )
                Text(
                    text = "${state.category.title} Mode",
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (state.isNewHighScore && state.score > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AmberWarn.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, AmberWarn)
                    ) {
                        Text(
                            text = "NEW HIGH SCORE!",
                            color = AmberWarn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${state.score}",
                    color = TextPrimary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "FINAL SCORE  •  BEST ${state.highScore}",
                    color = ShieldCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(label = "WAVE", value = "${state.wave}", accent = ShieldCyan, modifier = Modifier.weight(1f))
                    StatTile(label = "SOLVED", value = "${state.correctCount}", accent = EmeraldSafe, modifier = Modifier.weight(1f))
                    StatTile(label = "ACCURACY", value = "${state.accuracyPercent}%", accent = AmberWarn, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(label = "BEST STREAK", value = "x${state.bestStreak}", accent = AmberWarn, modifier = Modifier.weight(1f))
                    StatTile(label = "WRONG", value = "${state.wrongCount}", accent = CrimsonDanger, modifier = Modifier.weight(1f))
                    StatTile(label = "TIMEOUTS", value = "${state.timeoutCount}", accent = Color(0xFFFB7185), modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShieldCyan,
                        contentColor = Color(0xFF04131C)
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DEFEND AGAIN", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onMainMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.45f))
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MAIN MENU", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CardSlate,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}
