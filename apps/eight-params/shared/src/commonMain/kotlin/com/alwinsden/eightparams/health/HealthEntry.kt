package com.alwinsden.eightparams.health

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One logged measurement. `value1` is the primary value (weight, height,
 * systolic, sleep hours); `value2` is only used for blood pressure (diastolic).
 */
@Entity(tableName = "health_entries")
data class HealthEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value1: Double,
    val value2: Double? = null,
    val timestampMillis: Long,
)

/** UI-facing domain model, decoupled from Room. */
data class HealthEntry(
    val id: Long,
    val type: MetricType,
    val value1: Double,
    val value2: Double?,
    val timestampMillis: Long,
)

fun HealthEntryEntity.toDomain(): HealthEntry = HealthEntry(
    id = id,
    type = MetricType.fromName(type),
    value1 = value1,
    value2 = value2,
    timestampMillis = timestampMillis,
)

fun HealthEntry.toEntity(): HealthEntryEntity = HealthEntryEntity(
    id = id,
    type = type.name,
    value1 = value1,
    value2 = value2,
    timestampMillis = timestampMillis,
)
