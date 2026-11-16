package com.alwinsden.leptosagent

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    indices = [Index(value = ["userId"], unique = true)]
)
data class ModelPreferencesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val modelProvider: String,
    val model: String,
    val effortLevel: String,
    val useOnlineLeptos: Boolean
)
