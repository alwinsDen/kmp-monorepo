package com.alwinsden.eightparams.health

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Apple Health–like light palette. The reference screenshots are light-themed,
 * so the app always renders light for now.
 */
object HealthColors {
    val Background = Color(0xFFF2F2F7)
    val Card = Color(0xFFFFFFFF)
    val Pill = Color(0xFFEDEDF2)
    val Divider = Color(0xFFE4E4E9)
    val Grid = Color(0xFFE3E3E8)
    val Cursor = Color(0xFFD8D8DD)
    val TextPrimary = Color(0xFF17171A)
    val TextSecondary = Color(0xFF8E8E93)
    val TextTertiary = Color(0xFFAEAEB2)
}

fun metricColor(type: MetricType): Color = Color(type.colorArgb)

@Composable
fun HealthTheme(content: @Composable () -> Unit) {
    // Force light to match the Apple Health reference design.
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = metricColor(MetricType.Weight),
            background = HealthColors.Background,
            surface = HealthColors.Card,
        ),
        content = content,
    )
}
