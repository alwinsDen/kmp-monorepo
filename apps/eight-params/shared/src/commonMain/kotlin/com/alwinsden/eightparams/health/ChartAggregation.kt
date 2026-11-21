package com.alwinsden.eightparams.health

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/** Chart time ranges, mirroring Apple Health's D / W / M / 6M / Y selector. */
enum class ChartRange(val label: String) {
    Day("D"), Week("W"), Month("M"), SixMonths("6M"), Year("Y")
}

/** One averaged bucket on the x axis (positioned by time, not by index). */
data class ChartPoint(
    val bucketStartMillis: Long,
    val bucketEndMillis: Long,
    val value: Double,
    val value2: Double? = null,
)

/** A labelled tick on the x axis (gridline position + text). */
data class ChartXLabel(val atMillis: Long, val text: String)

/** Everything a chart needs to render, plus the AVERAGE header values. */
data class ChartData(
    val points: List<ChartPoint>,
    val windowStartMillis: Long,
    val windowEndMillis: Long,
    val yMin: Double,
    val yMax: Double,
    val yTicks: List<Double>,
    val xLabels: List<ChartXLabel>,
    val hasData: Boolean,
    /** Average shown in the AVERAGE header / floating bubble. */
    val headerValue: Double,
    val headerValue2: Double?,
    /** e.g. "Nov 15–21, 2026" for weeks, "Nov 21, 2026" for days. */
    val headerRangeLabel: String,
    /**
     * How many whole periods the window can still slide into the past before
     * the first-ever entry would fall out of view. 0 = nothing earlier to show.
     */
    val maxBackSteps: Int,
)

/**
 * Pure, timezone-aware aggregation — shareable with the future iOS UI.
 *
 * Buckets mirror Apple Health:
 *  - D  → hourly buckets over one day
 *  - W  → daily buckets over one Sunday-based week
 *  - M  → daily buckets over one calendar month
 *  - 6M → weekly buckets (one tick per week average) over ~6 months
 *  - Y  → monthly buckets over 12 months
 * Only buckets that actually contain samples become chart points; points are
 * positioned proportionally in time, exactly like the reference screenshots.
 *
 * [backSteps] slides the visible window into the past by whole periods
 * (day / week / month / week / month), so the user can pan back to their
 * first entries. It is clamped so the earliest entry always stays inside
 * the window when possible.
 */
object ChartAggregation {

    fun build(
        entries: List<HealthEntry>,
        type: MetricType,
        range: ChartRange,
        nowMillis: Long,
        tz: TimeZone = TimeZone.currentSystemDefault(),
        backSteps: Int = 0,
    ): ChartData {
        val typed = entries.filter { it.type == type }
        val maxBack = maxBackSteps(range, typed, nowMillis, tz)
        val steps = backSteps.coerceIn(0, maxBack)
        val buckets = buckets(range, Instant.fromEpochMilliseconds(nowMillis), tz, steps)
        val windowStart = buckets.first().startMillis
        val windowEnd = buckets.last().endMillis

        val inWindow = typed
            .filter { it.timestampMillis >= windowStart && it.timestampMillis < windowEnd }

        val points = inWindow
            .groupBy { bucketIndexFor(it.timestampMillis, buckets) }
            .map { (index, samples) ->
                val bucket = buckets[index]
                ChartPoint(
                    bucketStartMillis = bucket.startMillis,
                    bucketEndMillis = bucket.endMillis,
                    value = samples.map { it.value1 }.average(),
                    value2 = samples.mapNotNull { it.value2 }.takeIf { it.isNotEmpty() }?.average(),
                )
            }
            .sortedBy { it.bucketStartMillis }

        val values = points.flatMap { listOfNotNull(it.value, it.value2) }
        val scale = niceTicks(values.minOrNull() ?: 0.0, values.maxOrNull() ?: 0.0)

        // The AVERAGE header covers the last natural period of the window:
        // D → that day, W → that whole week, M → that whole month,
        // 6M → the latest week, Y → the latest month. This matches the
        // reference screenshots (W and 6M both showing "Nov 15–21, 2026").
        val headerPeriod = when (range) {
            ChartRange.Day, ChartRange.Week, ChartRange.Month -> windowStart to windowEnd
            ChartRange.SixMonths, ChartRange.Year -> buckets.last().let { it.startMillis to it.endMillis }
        }
        val headerSamples = inWindow.filter {
            it.timestampMillis >= headerPeriod.first && it.timestampMillis < headerPeriod.second
        }
        val headerValue = headerSamples.map { it.value1 }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        val headerValue2 = headerSamples.mapNotNull { it.value2 }.takeIf { it.isNotEmpty() }?.average()

        return ChartData(
            points = points,
            windowStartMillis = windowStart,
            windowEndMillis = windowEnd,
            yMin = scale.yMin,
            yMax = scale.yMax,
            yTicks = scale.ticks,
            xLabels = xLabels(range, buckets, windowEnd, tz),
            hasData = points.isNotEmpty(),
            headerValue = headerValue,
            headerValue2 = headerValue2,
            headerRangeLabel = if (headerSamples.isEmpty()) "" else headerLabel(range, headerPeriod.first, headerPeriod.second, tz),
            maxBackSteps = maxBack,
        )
    }

    /**
     * Date-range label for a single bucket (used when the user taps a point):
     * "Nov 21, 2026" for day/hour/day buckets, "Nov 15–21, 2026" for weeks,
     * "Sep 2026" for monthly buckets in the Y view.
     */
    fun pointRangeLabel(range: ChartRange, point: ChartPoint, tz: TimeZone): String {
        val start = Instant.fromEpochMilliseconds(point.bucketStartMillis).toLocalDateTime(tz).date
        val end = Instant.fromEpochMilliseconds(point.bucketEndMillis - 1).toLocalDateTime(tz).date
        return when (range) {
            ChartRange.Day, ChartRange.Month -> formatDateLong(point.bucketEndMillis - 1, tz)
            ChartRange.Week, ChartRange.SixMonths -> formatWeekRange(start, end)
            ChartRange.Year -> "${monthShort(end.monthNumber)} ${end.year}"
        }
    }

    // ---- buckets -----------------------------------------------------------

    private data class Bucket(val startMillis: Long, val endMillis: Long)

    private fun buckets(range: ChartRange, now: Instant, tz: TimeZone, backSteps: Int): List<Bucket> {
        val today = now.toLocalDateTime(tz).date
        return when (range) {
            ChartRange.Day -> {
                val day = today.minusDays(backSteps)
                val nextDay = day.plusDays(1)
                (0..23).map { h ->
                    val start = day.atTime(h, 0).toInstant(tz)
                    val end = if (h < 23) day.atTime(h + 1, 0).toInstant(tz) else nextDay.atStartOfDayIn(tz)
                    Bucket(start.toEpochMilliseconds(), end.toEpochMilliseconds())
                }
            }
            ChartRange.Week -> {
                val sunday = today.atStartOfWeek().minusDays(backSteps * 7)
                (0..6).map { d ->
                    val day = sunday.plusDays(d)
                    Bucket(day.startOfDayMillis(tz), day.plusDays(1).startOfDayMillis(tz))
                }
            }
            ChartRange.Month -> {
                val first = LocalDate(today.year, today.monthNumber, 1).plusMonths(-backSteps)
                val nextFirst = first.plusMonths(1)
                generateSequence(first) { it.plusDays(1) }
                    .takeWhile { it < nextFirst }
                    .map { day -> Bucket(day.startOfDayMillis(tz), day.plusDays(1).startOfDayMillis(tz)) }
                    .toList()
            }
            ChartRange.SixMonths -> {
                // 27 Sunday-aligned weekly buckets ending at the anchor week.
                val anchorWeekStart = today.atStartOfWeek().minusDays(backSteps * 7)
                val startDay = anchorWeekStart.minusDays(26 * 7)
                val endExclusive = anchorWeekStart.plusDays(7)
                generateSequence(startDay) { it.plusDays(7) }
                    .takeWhile { it < endExclusive }
                    .map { day -> Bucket(day.startOfDayMillis(tz), day.plusDays(7).startOfDayMillis(tz)) }
                    .toList()
            }
            ChartRange.Year -> {
                val thisMonth = LocalDate(today.year, today.monthNumber, 1)
                val startMonth = thisMonth.plusMonths(-(11 + backSteps))
                val endExclusive = thisMonth.plusMonths(1 - backSteps)
                generateSequence(startMonth) { it.plusMonths(1) }
                    .takeWhile { it < endExclusive }
                    .map { month -> Bucket(month.startOfDayMillis(tz), month.plusMonths(1).startOfDayMillis(tz)) }
                    .toList()
            }
        }
    }

    /**
     * How far the window may slide back (in whole periods) while keeping the
     * first-ever entry inside it. Panning back moves the window's right edge
     * toward the oldest entry, so the limit is where the first entry would
     * fall out of the window's right side. The rough millisecond estimate is
     * corrected against the real (DST-aware) calendar buckets.
     */
    private fun maxBackSteps(range: ChartRange, typed: List<HealthEntry>, nowMillis: Long, tz: TimeZone): Int {
        val firstMillis = typed.minOfOrNull { it.timestampMillis } ?: return 0
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(tz).date
        val firstDate = Instant.fromEpochMilliseconds(firstMillis).toLocalDateTime(tz).date

        val rough = when (range) {
            ChartRange.Day -> floorDivLong(
                today.startOfDayMillis(tz) - firstDate.startOfDayMillis(tz),
                86_400_000L,
            )
            ChartRange.Week -> floorDivLong(
                today.atStartOfWeek().startOfDayMillis(tz) - firstDate.atStartOfWeek().startOfDayMillis(tz),
                604_800_000L,
            )
            ChartRange.SixMonths -> floorDivLong(
                today.atStartOfWeek().startOfDayMillis(tz) - firstDate.atStartOfWeek().startOfDayMillis(tz),
                604_800_000L,
            )
            ChartRange.Month, ChartRange.Year -> {
                val firstMonth = LocalDate(firstDate.year, firstDate.monthNumber, 1)
                val limitMonth = if (range == ChartRange.Month) {
                    LocalDate(today.year, today.monthNumber, 1)
                } else {
                    LocalDate(today.year, today.monthNumber, 1).plusMonths(-11)
                }
                var count = 0
                var cursor = firstMonth
                while (cursor < limitMonth) {
                    cursor = cursor.plusMonths(1)
                    count++
                }
                count.toLong()
            }
        }.toInt().coerceAtLeast(0)

        if (rough == 0) return 0

        // Correction: step back until the oldest entry is actually inside the
        // window produced for that step count (ms math can drift ±1h over DST).
        var steps = rough
        var window = buckets(range, Instant.fromEpochMilliseconds(nowMillis), tz, steps)
        fun containsFirst(): Boolean =
            firstMillis >= window.first().startMillis && firstMillis < window.last().endMillis
        while (steps > 0 && !containsFirst()) {
            steps--
            window = buckets(range, Instant.fromEpochMilliseconds(nowMillis), tz, steps)
        }
        return if (containsFirst()) steps else 0
    }

    /** Sunday-start week, matching Apple Health. */
    private fun LocalDate.atStartOfWeek(): LocalDate =
        minusDays(dayOfWeek.isoDayNumber % 7)

    private fun LocalDate.minusDays(days: Int): LocalDate =
        LocalDate.fromEpochDays(toEpochDays() - days)

    private fun LocalDate.plusDays(days: Int): LocalDate =
        LocalDate.fromEpochDays(toEpochDays() + days)

    private fun LocalDate.plusMonths(months: Int): LocalDate {
        val total = (monthNumber - 1) + months
        return LocalDate(year + floorDiv(total, 12), floorMod(total, 12) + 1, 1)
    }

    private fun LocalDate.startOfDayMillis(tz: TimeZone): Long =
        atStartOfDayIn(tz).toEpochMilliseconds()

    private fun floorDiv(a: Int, b: Int): Int {
        val q = a / b
        return if (a % b != 0 && (a < 0) != (b < 0)) q - 1 else q
    }

    private fun floorMod(a: Int, b: Int): Int = a - floorDiv(a, b) * b

    private fun floorDivLong(a: Long, b: Long): Long {
        val q = a / b
        return if (a % b != 0L && (a < 0L) != (b < 0L)) q - 1 else q
    }

    private fun bucketIndexFor(timestampMillis: Long, buckets: List<Bucket>): Int {
        var lo = 0
        var hi = buckets.size - 1
        var result = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (buckets[mid].startMillis <= timestampMillis) {
                result = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return result
    }

    // ---- x axis labels -----------------------------------------------------

    private fun xLabels(
        range: ChartRange,
        buckets: List<Bucket>,
        windowEnd: Long,
        tz: TimeZone,
    ): List<ChartXLabel> = when (range) {
        ChartRange.Day -> listOf(0 to "12AM", 6 to "6AM", 12 to "12PM", 18 to "6PM")
            .map { (h, text) -> ChartXLabel(buckets[h].startMillis, text) }

        ChartRange.Week -> buckets.map { b ->
            val date = Instant.fromEpochMilliseconds(b.startMillis).toLocalDateTime(tz).date
            ChartXLabel(b.startMillis, weekdayShort(date.dayOfWeek))
        }

        ChartRange.Month -> {
            // Label the 1st and every Sunday inside the month: "Nov 1", "Nov 8", …
            var day = Instant.fromEpochMilliseconds(buckets.first().startMillis).toLocalDateTime(tz).date
            val labels = mutableListOf<ChartXLabel>()
            while (day.startOfDayMillis(tz) < windowEnd) {
                if (day.dayOfMonth == 1 || day.dayOfWeek == DayOfWeek.SUNDAY) {
                    labels += ChartXLabel(day.startOfDayMillis(tz), "${monthShort(day.monthNumber)} ${day.dayOfMonth}")
                }
                day = day.plusDays(1)
            }
            labels
        }

        ChartRange.SixMonths, ChartRange.Year -> {
            // One label per month start inside the window: "Jul", "Aug", …
            val firstMonth = Instant.fromEpochMilliseconds(buckets.first().startMillis).toLocalDateTime(tz).date
            var month = LocalDate(firstMonth.year, firstMonth.monthNumber, 1)
            // Skip month starts that fall before the window's first bucket,
            // otherwise they clamp to the left edge and collide with the next label.
            while (month.startOfDayMillis(tz) < buckets.first().startMillis) {
                month = month.plusMonths(1)
            }
            val labels = mutableListOf<ChartXLabel>()
            while (month.startOfDayMillis(tz) < windowEnd) {
                labels += ChartXLabel(month.startOfDayMillis(tz), monthShort(month.monthNumber))
                month = month.plusMonths(1)
            }
            labels
        }
    }

    // ---- AVERAGE header ----------------------------------------------------

    private fun headerLabel(range: ChartRange, periodStartMillis: Long, periodEndMillis: Long, tz: TimeZone): String {
        val start = Instant.fromEpochMilliseconds(periodStartMillis).toLocalDateTime(tz).date
        return when (range) {
            ChartRange.Day -> formatDateLong(periodEndMillis - 1, tz)
            ChartRange.Week, ChartRange.SixMonths -> {
                val end = Instant.fromEpochMilliseconds(periodEndMillis - 1).toLocalDateTime(tz).date
                formatWeekRange(start, end)
            }
            ChartRange.Month -> "${monthShort(start.monthNumber)} ${start.year}"
            ChartRange.Year -> {
                val end = Instant.fromEpochMilliseconds(periodEndMillis - 1).toLocalDateTime(tz).date
                "${monthShort(end.monthNumber)} ${end.year}"
            }
        }
    }

    // ---- nice y-axis bounds (Apple-style round numbers) --------------------

    data class NiceScale(val yMin: Double, val yMax: Double, val ticks: List<Double>)

    fun niceTicks(min: Double, max: Double, targetCount: Int = 4): NiceScale {
        var lo = min
        var hi = max
        if (lo == hi) {
            val pad = if (lo == 0.0) 1.0 else maxOf(1.0, kotlin.math.abs(lo) * 0.02)
            lo -= pad
            hi += pad
        }

        val rawStep = (hi - lo) / targetCount
        val magnitude = 10.0.pow(floor(ln(rawStep) / ln(10.0)))
        val normalized = rawStep / magnitude
        val nice = when {
            normalized <= 1 -> 1.0
            normalized <= 2 -> 2.0
            normalized <= 2.5 -> 2.5
            normalized <= 5 -> 5.0
            else -> 10.0
        } * magnitude
        // Never go finer than 0.5 — tiny-variance data (e.g. two weekly
        // averages 0.07 apart) must still get coarse round ticks like Apple.
        val step = maxOf(nice, 0.5)

        // Give the data breathing room of ~0.6 steps on each side.
        val yMin = floor((lo - step * 0.6) / step) * step
        val yMaxCandidate = ceil((hi + step * 0.6) / step) * step
        val yMax = if (yMaxCandidate <= yMin) yMin + step else yMaxCandidate

        var ticks = generateSequence(yMin) { it + step }
            .takeWhile { it <= yMax + 1e-9 }
            .toList()
        // Keep the axis readable if the step turned out too fine.
        while (ticks.size > 6) {
            ticks = ticks.filterIndexed { index, _ -> index % 2 == 0 }
        }
        return NiceScale(yMin, yMax, ticks)
    }
}
