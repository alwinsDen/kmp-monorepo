package com.alwinsden.eightparams.health

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartAggregationTest {

    private val tz = TimeZone.of("America/New_York")

    // "now" = Saturday Nov 21, 2026, 12:00 local time (matches the screenshots).
    private val now = LocalDateTime(2026, 11, 21, 12, 0).toInstant(tz).toEpochMilliseconds()

    private fun entry(type: MetricType, date: String, hour: Int = 12, value1: Double, value2: Double? = null) =
        HealthEntry(
            id = 0,
            type = type,
            value1 = value1,
            value2 = value2,
            timestampMillis = LocalDate.parse(date).atTime(hour, 0).toInstant(tz).toEpochMilliseconds(),
        )

    @Test
    fun weekBucketsProduceDailyPointsWithSundayStart() {
        val entries = listOf(
            entry(MetricType.Weight, "2026-11-15", value1 = 88.0), // Sun
            entry(MetricType.Weight, "2026-11-16", value1 = 89.0),
            entry(MetricType.Weight, "2026-11-17", value1 = 90.0),
            entry(MetricType.Weight, "2026-11-18", value1 = 91.0),
            entry(MetricType.Weight, "2026-11-19", value1 = 92.0),
            entry(MetricType.Weight, "2026-11-20", value1 = 93.0),
            entry(MetricType.Weight, "2026-11-21", value1 = 94.0), // Sat
        )

        val data = ChartAggregation.build(entries, MetricType.Weight, ChartRange.Week, now, tz)

        assertEquals(7, data.points.size)
        val first = data.points.first()
        val firstStart = LocalDateTime(2026, 11, 15, 0, 0).toInstant(tz).toEpochMilliseconds()
        assertEquals(firstStart, first.bucketStartMillis)
        // Header mirrors the reference: the whole week's average + "Nov 15–21, 2026".
        assertEquals(91.0, data.headerValue) // mean of 88..94
        assertEquals("Nov 15–21, 2026", data.headerRangeLabel)
        // Day-of-week labels Sun..Sat in order.
        assertEquals(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"), data.xLabels.map { it.text })
    }

    @Test
    fun weekAveragesMultipleSamplesPerDay() {
        val entries = listOf(
            entry(MetricType.Weight, "2026-11-21", hour = 8, value1 = 90.0),
            entry(MetricType.Weight, "2026-11-21", hour = 20, value1 = 86.0),
        )
        val data = ChartAggregation.build(entries, MetricType.Weight, ChartRange.Week, now, tz)
        assertEquals(1, data.points.size)
        assertEquals(88.0, data.points.single().value)
    }

    @Test
    fun sixMonthsUsesWeeklyTicksAndMonthLabels() {
        // Two samples 8 days apart → they must land in different week buckets.
        val entries = listOf(
            entry(MetricType.Weight, "2026-09-01", value1 = 95.0),
            entry(MetricType.Weight, "2026-09-09", value1 = 93.0),
            entry(MetricType.Weight, "2026-11-21", value1 = 88.08),
        )
        val data = ChartAggregation.build(entries, MetricType.Weight, ChartRange.SixMonths, now, tz)

        // ~27 weekly buckets exist; only weeks with data become points.
        assertTrue(data.points.size >= 3, "expected ≥3 weekly points, got ${data.points.size}")
        val labels = data.xLabels.map { it.text }
        // Window starts mid-May (27 Sunday-aligned weeks), so month labels
        // begin with the first month start *inside* the window.
        assertTrue("Jun" in labels && "Nov" in labels, "month labels missing: $labels")
        // Header = latest week's average, labelled with its Sun–Sat range.
        assertEquals("Nov 15–21, 2026", data.headerRangeLabel)
    }

    @Test
    fun bloodPressureAveragesBothValues() {
        val entries = listOf(
            entry(MetricType.BloodPressure, "2026-11-21", hour = 8, value1 = 120.0, value2 = 80.0),
            entry(MetricType.BloodPressure, "2026-11-21", hour = 20, value1 = 130.0, value2 = 84.0),
        )
        val data = ChartAggregation.build(entries, MetricType.BloodPressure, ChartRange.Day, now, tz)
        // Day view buckets by hour → one point per logged hour.
        assertEquals(2, data.points.size)
        assertEquals(120.0, data.points.first().value)
        assertEquals(80.0, data.points.first().value2)
        // Header = today's overall average, labelled with today's date.
        assertEquals(125.0, data.headerValue)
        assertEquals(82.0, data.headerValue2)
        assertEquals("Nov 21, 2026", data.headerRangeLabel)
    }

    @Test
    fun niceTicksAreRoundNumbersSpanningTheData() {
        val scale = ChartAggregation.niceTicks(87.0, 90.0)
        assertTrue(scale.yMin <= 87.0 && scale.yMax >= 90.0)
        assertTrue(scale.ticks.size in 2..6, "ticks: ${scale.ticks}")
        assertEquals(scale.yMin, scale.ticks.first())
        // Every tick aligns with the axis bounds spacing.
        assertTrue(scale.ticks.all { it >= scale.yMin && it <= scale.yMax })
    }

    @Test
    fun niceTicksStayCoarseWhenVarianceIsTiny() {
        // Two weekly averages 0.07 apart must not produce 0.025-step axes.
        val scale = ChartAggregation.niceTicks(88.1, 88.17)
        assertTrue(scale.yMin <= 88.1 && scale.yMax >= 88.17, "bounds: ${scale.yMin}..${scale.yMax}")
        assertTrue(scale.ticks.size in 2..6, "ticks: ${scale.ticks}")
        // Steps are at least 0.5 → every tick is a multiple of 0.5.
        assertTrue(scale.ticks.all { val doubled = it * 2; kotlin.math.abs(doubled - doubled.roundToLong()) < 1e-9 }, "ticks: ${scale.ticks}")
    }

    @Test
    fun panBackSlidesSixMonthWindowToTheFirstEntry() {
        val entries = listOf(
            entry(MetricType.Weight, "2026-09-07", value1 = 94.0),
            entry(MetricType.Weight, "2026-11-21", value1 = 88.08),
        )
        val base = ChartAggregation.build(entries, MetricType.Weight, ChartRange.SixMonths, now, tz)
        assertTrue(base.maxBackSteps > 0, "expected room to pan back, got ${base.maxBackSteps}")

        val panned = ChartAggregation.build(entries, MetricType.Weight, ChartRange.SixMonths, now, tz, backSteps = base.maxBackSteps)
        // The first entry stays inside the window, at its right edge.
        assertEquals(94.0, panned.points.first().value)
        assertTrue(panned.windowEndMillis > entries.first().timestampMillis)

        // Over-panning clamps to the same window instead of losing the entry.
        val over = ChartAggregation.build(entries, MetricType.Weight, ChartRange.SixMonths, now, tz, backSteps = base.maxBackSteps + 999)
        assertEquals(panned.windowStartMillis, over.windowStartMillis)
    }

    @Test
    fun panBackWeekShowsThePreviousWeek() {
        val entries = listOf(
            entry(MetricType.Weight, "2026-11-09", value1 = 90.0), // Mon, previous week
            entry(MetricType.Weight, "2026-11-21", value1 = 88.0),
        )
        val base = ChartAggregation.build(entries, MetricType.Weight, ChartRange.Week, now, tz)
        assertEquals(1, base.maxBackSteps, "one week back must be pannable")

        val panned = ChartAggregation.build(entries, MetricType.Weight, ChartRange.Week, now, tz, backSteps = 1)
        val prevSunday = LocalDateTime(2026, 11, 8, 0, 0).toInstant(tz).toEpochMilliseconds()
        assertEquals(prevSunday, panned.windowStartMillis)
        assertEquals(1, panned.points.size)
        assertEquals(90.0, panned.points.single().value)
        assertEquals("Nov 8–14, 2026", panned.headerRangeLabel)
        assertEquals(90.0, panned.headerValue)
    }

    @Test
    fun pointRangeLabelDescribesTheTappedBucket() {
        val weekPoint = ChartPoint(
            bucketStartMillis = LocalDateTime(2026, 11, 15, 0, 0).toInstant(tz).toEpochMilliseconds(),
            bucketEndMillis = LocalDateTime(2026, 11, 22, 0, 0).toInstant(tz).toEpochMilliseconds(),
            value = 88.17,
        )
        assertEquals("Nov 15–21, 2026", ChartAggregation.pointRangeLabel(ChartRange.SixMonths, weekPoint, tz))

        val monthPoint = ChartPoint(
            bucketStartMillis = LocalDateTime(2026, 9, 1, 0, 0).toInstant(tz).toEpochMilliseconds(),
            bucketEndMillis = LocalDateTime(2026, 10, 1, 0, 0).toInstant(tz).toEpochMilliseconds(),
            value = 94.0,
        )
        assertEquals("Sep 2026", ChartAggregation.pointRangeLabel(ChartRange.Year, monthPoint, tz))
    }

    @Test
    fun formattingHelpersMatchScreenshots() {
        assertEquals("88.08", formatValue(88.08))
        assertEquals("90", formatValue(90.0))
        assertEquals("177.5", formatValue(177.5))
        assertEquals("118/78", formatMetricValue(MetricType.BloodPressure, 118.4, 78.6))
        assertEquals("Nov 21, 2026", formatDateLong(now, tz))
        assertEquals("12:00 PM", formatTime(now, tz))
    }
}
