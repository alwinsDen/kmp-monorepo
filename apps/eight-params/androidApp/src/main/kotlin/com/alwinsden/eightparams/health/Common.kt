package com.alwinsden.eightparams.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Apple Health's white circular toolbar button. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconTint: Color = Color(0xFF3C3C43),
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = modifier.size(42.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (enabled) iconTint else Color(0xFFC7C7CC),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** The D / W / M / 6M / Y pill selector from the screenshots. */
@Composable
fun RangeSelector(
    selected: ChartRange,
    onSelect: (ChartRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(HealthColors.Pill)
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ChartRange.entries.forEach { range ->
            val isSelected = range == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(elevation = 2.dp, shape = RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(range) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = range.label,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) HealthColors.TextPrimary else HealthColors.TextSecondary,
                )
            }
        }
    }
}

/** Inline AVERAGE header (D and W views); shows the selected point while one is tapped. */
@Composable
fun AverageHeader(
    type: MetricType,
    value: Double,
    value2: Double?,
    rangeLabel: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Text(
            text = "AVERAGE",
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            color = HealthColors.TextSecondary,
            fontWeight = FontWeight.Medium,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatMetricValue(type, value, value2),
                fontSize = 40.sp,
                fontWeight = FontWeight.Medium,
                color = HealthColors.TextPrimary,
            )
            Box(modifier = Modifier.padding(start = 5.dp, bottom = 7.dp)) {
                Text(
                    text = if (type == MetricType.BloodPressure) "mmHg" else type.unit,
                    fontSize = 16.sp,
                    color = HealthColors.TextSecondary,
                )
            }
        }
        Text(
            text = rangeLabel,
            fontSize = 15.sp,
            color = HealthColors.TextSecondary,
        )
    }
}
