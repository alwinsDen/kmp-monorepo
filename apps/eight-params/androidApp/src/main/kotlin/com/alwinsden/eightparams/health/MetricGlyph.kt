package com.alwinsden.eightparams.health

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn metric glyphs (no icon dependency needed): person for weight,
 * ruler for height, heart for blood pressure, crescent moon for sleep.
 */
@Composable
fun MetricGlyph(type: MetricType, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        fun d(value: Float) = value * size.width / 24f

        when (type) {
            MetricType.Weight -> {
                val stroke = Stroke(width = d(2.2f), cap = StrokeCap.Round)
                drawCircle(color = tint, radius = d(2.7f), center = Offset(cx, cy - d(6.1f)), style = stroke)
                drawLine(tint, Offset(cx, cy - d(2.6f)), Offset(cx, cy + d(5.0f)), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(cx - d(6.6f), cy - d(0.6f)), Offset(cx + d(6.6f), cy - d(0.6f)), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(cx, cy + d(5.0f)), Offset(cx - d(3.6f), cy + d(9.6f)), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(cx, cy + d(5.0f)), Offset(cx + d(3.6f), cy + d(9.6f)), stroke.width, StrokeCap.Round)
            }

            MetricType.Height -> {
                val stroke = Stroke(width = d(2.0f), cap = StrokeCap.Round)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - d(3.4f), cy - d(9.6f)),
                    size = Size(d(6.8f), d(19.2f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(d(2.4f)),
                    style = stroke,
                )
                listOf(-5.6f, -1.9f, 1.8f, 5.5f).forEach { tickY ->
                    drawLine(
                        tint,
                        Offset(cx - d(3.4f), cy + d(tickY)),
                        Offset(cx - d(0.2f), cy + d(tickY)),
                        stroke.width,
                        StrokeCap.Round,
                    )
                }
            }

            MetricType.BloodPressure -> {
                // Filled heart.
                val heart = Path().apply {
                    moveTo(cx, cy + d(8.6f))
                    cubicTo(cx - d(10.2f), cy + d(1.4f), cx - d(9.4f), cy - d(8.2f), cx, cy - d(3.2f))
                    cubicTo(cx + d(9.4f), cy - d(8.2f), cx + d(10.2f), cy + d(1.4f), cx, cy + d(8.6f))
                    close()
                }
                drawPath(heart, tint)
            }

            MetricType.Sleep -> {
                // Crescent moon: a wide open arc.
                drawArc(
                    color = tint,
                    startAngle = -50f,
                    sweepAngle = 255f,
                    useCenter = false,
                    topLeft = Offset(cx - d(7.6f), cy - d(7.6f)),
                    size = Size(d(15.2f), d(15.2f)),
                    style = Stroke(width = d(2.4f), cap = StrokeCap.Round),
                )
            }
        }
    }
}
