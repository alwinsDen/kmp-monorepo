package com.alwinsden.eightparams.health

/**
 * The health metrics the app tracks. Kept in `shared` so a future iOS UI can
 * consume the exact same definitions.
 */
enum class MetricType(val title: String, val unit: String, val colorArgb: Long) {
    Weight("Weight", "kg", 0xFFE852C0),
    Height("Height", "cm", 0xFF9BD019),
    BloodPressure("Blood Pressure", "mmHg", 0xFFFF3B30),
    Sleep("Sleep", "hrs", 0xFF5E5CE6);

    companion object {
        fun fromName(name: String): MetricType =
            entries.firstOrNull { it.name == name } ?: Weight
    }
}

/** True when the metric carries a second value (blood pressure: diastolic). */
fun MetricType.hasSecondValue(): Boolean = this == MetricType.BloodPressure
