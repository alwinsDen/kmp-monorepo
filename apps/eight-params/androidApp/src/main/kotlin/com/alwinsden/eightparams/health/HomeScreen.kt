package com.alwinsden.eightparams.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.TimeZone

/** Apple Health "Summary"-style home: one card per tracked metric. */
@Composable
fun HomeScreen(
    entries: Map<MetricType, List<HealthEntry>>,
    onOpen: (MetricType) -> Unit,
    onAdd: (MetricType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(10.dp))
            Text("Summary", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = HealthColors.TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                formatDateLong(System.currentTimeMillis(), TimeZone.currentSystemDefault()),
                fontSize = 14.sp,
                color = HealthColors.TextSecondary,
            )
            Spacer(Modifier.height(14.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp, end = 18.dp, bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(MetricType.entries) { type ->
                MetricCard(
                    type = type,
                    latest = entries[type]?.lastOrNull(),
                    onOpen = { onOpen(type) },
                    onAdd = { onAdd(type) },
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    type: MetricType,
    latest: HealthEntry?,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    val color = metricColor(type)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HealthColors.Card,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                MetricGlyph(type = type, tint = color, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.size(14.dp))

            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(type.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
                Spacer(Modifier.height(3.dp))
                if (latest == null) {
                    Text("No Data", fontSize = 13.sp, color = HealthColors.TextSecondary)
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            formatMetricValue(type, latest.value1, latest.value2),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HealthColors.TextPrimary,
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            if (type == MetricType.BloodPressure) "mmHg" else type.unit,
                            fontSize = 13.sp,
                            color = HealthColors.TextSecondary,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    val tz = TimeZone.currentSystemDefault()
                    Text(
                        "Last · ${formatDateLong(latest.timestampMillis, tz)}, ${formatTime(latest.timestampMillis, tz)}",
                        fontSize = 12.sp,
                        color = HealthColors.TextSecondary,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(HealthColors.Pill, CircleShape)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Log ${type.title}",
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
