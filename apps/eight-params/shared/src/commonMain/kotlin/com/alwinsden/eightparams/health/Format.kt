package com.alwinsden.eightparams.health

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong

private val MONTHS_SHORT = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private val WEEKDAYS_SHORT = mapOf(
    DayOfWeek.SUNDAY to "Sun",
    DayOfWeek.MONDAY to "Mon",
    DayOfWeek.TUESDAY to "Tue",
    DayOfWeek.WEDNESDAY to "Wed",
    DayOfWeek.THURSDAY to "Thu",
    DayOfWeek.FRIDAY to "Fri",
    DayOfWeek.SATURDAY to "Sat",
)

fun monthShort(monthNumber: Int): String = MONTHS_SHORT[(monthNumber - 1).coerceIn(0, 11)]

fun weekdayShort(day: DayOfWeek): String = WEEKDAYS_SHORT.getValue(day)

/**
 * Locale-independent value formatting: up to 2 decimals, trailing zeros
 * trimmed. 88.08 → "88.08", 90.0 → "90", 177.5 → "177.5".
 */
fun formatValue(value: Double): String {
    val scaled = (value * 100).roundToLong()
    val negative = scaled < 0
    val abs = if (negative) -scaled else scaled
    val whole = abs / 100
    val frac = abs % 100
    val sign = if (negative) "-" else ""
    return when {
        frac == 0L -> "$sign$whole"
        frac % 10 == 0L -> "$sign$whole.${frac / 10}"
        else -> "$sign$whole.${frac.toString().padStart(2, '0')}"
    }
}

/** Blood pressure renders as "118/78". */
fun formatMetricValue(type: MetricType, value1: Double, value2: Double?): String =
    if (type == MetricType.BloodPressure) {
        val sys = value1.toLong()
        val dia = (value2 ?: 0.0).toLong()
        "$sys/$dia"
    } else {
        formatValue(value1)
    }

/** "Nov 21, 2026" */
fun formatDateLong(millis: Long, tz: TimeZone): String {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz).date
    return "${monthShort(date.monthNumber)} ${date.dayOfMonth}, ${date.year}"
}

/** "5:44 PM" */
fun formatTime(millis: Long, tz: TimeZone): String {
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz)
    val amPm = if (dt.hour < 12) "AM" else "PM"
    val hour12 = (dt.hour % 12).let { if (it == 0) 12 else it }
    return "$hour12:${dt.minute.toString().padStart(2, '0')} $amPm"
}

/**
 * Week range label like the screenshots: "Nov 15–21, 2026",
 * "Nov 28 – Dec 4, 2026", "Dec 28, 2026 – Jan 3, 2027".
 */
fun formatWeekRange(start: LocalDate, end: LocalDate): String {
    if (start == end) return "${monthShort(start.monthNumber)} ${start.dayOfMonth}, ${start.year}"
    val sameMonth = start.monthNumber == end.monthNumber && start.year == end.year
    return when {
        sameMonth -> "${monthShort(start.monthNumber)} ${start.dayOfMonth}–${end.dayOfMonth}, ${start.year}"
        start.year == end.year ->
            "${monthShort(start.monthNumber)} ${start.dayOfMonth} – ${monthShort(end.monthNumber)} ${end.dayOfMonth}, ${end.year}"
        else ->
            "${monthShort(start.monthNumber)} ${start.dayOfMonth}, ${start.year} – " +
                "${monthShort(end.monthNumber)} ${end.dayOfMonth}, ${end.year}"
    }
}
