package com.alwinsden.eightparams.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.TimeZone

/**
 * Apple Health metric detail (screens 2 & 3): back / title / add header,
 * D-W-M-6M-Y selector, AVERAGE header, line chart, and the data list.
 */
@Composable
fun MetricDetailScreen(
    type: MetricType,
    entries: List<HealthEntry>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    var range by remember { mutableStateOf(ChartRange.Week) }
    // Pan state: how many whole periods the window slid into the past.
    var backSteps by remember(range) { mutableStateOf(0) }
    // Selection: the point highlighted by the chart's tap marker (drives the header).
    var markerPoint by remember { mutableStateOf<ChartPoint?>(null) }

    val tz = TimeZone.currentSystemDefault()
    val chartData = remember(entries, range, backSteps) {
        ChartAggregation.build(entries, type, range, System.currentTimeMillis(), tz, backSteps)
    }

    // The header shows the marker-highlighted point's value/date, else the default.
    val headerValue = markerPoint?.value ?: chartData.headerValue
    val headerValue2 = markerPoint?.value2 ?: chartData.headerValue2
    val headerLabel = markerPoint
        ?.let { ChartAggregation.pointRangeLabel(range, it, tz) }
        ?: chartData.headerRangeLabel

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ---- header -----------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onClick = onBack)
            Text(
                type.title,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = HealthColors.TextPrimary,
            )
            CircleIconButton(Icons.Default.Add, "Add entry", onClick = onAdd)
        }

        Spacer(Modifier.height(8.dp))

        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            RangeSelector(selected = range, onSelect = { range = it })
            Spacer(Modifier.height(16.dp))

            // D and W get the inline AVERAGE header (screenshot 2);
            // M / 6M / Y get the floating bubble over the chart (screenshot 3).
            // Both reflect the tapped point while one is selected.
            if (range == ChartRange.Day || range == ChartRange.Week) {
                if (chartData.hasData) AverageHeader(type, headerValue, headerValue2, headerLabel)
                Spacer(Modifier.height(10.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .height(320.dp),
        ) {
            if (chartData.hasData) {
                MetricLineChart(
                    data = chartData,
                    type = type,
                    range = range,
                    selectedPoint = markerPoint,
                    onPointSelected = { markerPoint = it },
                    onPanSteps = { delta -> backSteps = (backSteps + delta).coerceAtLeast(0) },
                    panStepMillis = when (range) {
                        ChartRange.Day -> 86_400_000L
                        ChartRange.Week, ChartRange.SixMonths -> 604_800_000L
                        ChartRange.Month, ChartRange.Year -> 2_592_000_000L
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Data", fontSize = 17.sp, color = HealthColors.TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ---- all data ---------------------------------------------------
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            Text("Show All Data", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = metricColor(type))
            Spacer(Modifier.height(10.dp))
            val sorted = entries.sortedByDescending { it.timestampMillis }
            sorted.forEach { entry ->
                DataRow(
                    entry = entry,
                    onDelete = { onDelete(entry.id) },
                )
                Spacer(Modifier.height(8.dp))
            }
            if (sorted.isEmpty()) {
                Text("Nothing logged yet — tap + to add your first entry.", fontSize = 13.sp, color = HealthColors.TextSecondary)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DataRow(entry: HealthEntry, onDelete: () -> Unit) {
    val tz = TimeZone.currentSystemDefault()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = HealthColors.Card,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatDateLong(entry.timestampMillis, tz),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = HealthColors.TextPrimary,
                )
                Text(
                    formatTime(entry.timestampMillis, tz),
                    fontSize = 12.sp,
                    color = HealthColors.TextSecondary,
                )
            }
            Text(
                formatMetricValue(entry.type, entry.value1, entry.value2) +
                    if (entry.type == MetricType.BloodPressure) " mmHg" else " ${entry.type.unit}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = HealthColors.TextPrimary,
            )
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(HealthColors.Pill, CircleShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete entry",
                    tint = HealthColors.TextSecondary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}
