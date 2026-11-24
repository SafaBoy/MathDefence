package com.safaboy.mathdefence.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MidnightBg = Color(0xFF090D1A)
val DeepSlate = Color(0xFF11182C)
val CardSlate = Color(0xFF18223A)
val ShieldCyan = Color(0xFF22D3EE)
val ShieldBlue = Color(0xFF0EA5E9)
val EmeraldSafe = Color(0xFF34D399)
val AmberWarn = Color(0xFFFBBF24)
val CrimsonDanger = Color(0xFFEF4444)
val RoseAccent = Color(0xFFFB7185)
val TextPrimary = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)

private val MathDefenceColorScheme = darkColorScheme(
    primary = ShieldCyan,
    onPrimary = Color(0xFF04131C),
    secondary = EmeraldSafe,
    onSecondary = Color(0xFF052016),
    tertiary = AmberWarn,
    background = MidnightBg,
    onBackground = TextPrimary,
    surface = DeepSlate,
    onSurface = TextPrimary,
    surfaceVariant = CardSlate,
    onSurfaceVariant = TextMuted,
    error = CrimsonDanger,
    onError = Color.White
)

@Composable
fun MathDefenceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MathDefenceColorScheme,
        content = content
    )
}
