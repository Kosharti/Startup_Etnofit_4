package com.example.startup_etnofit_2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checks_data")
data class ChecksData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Автоматическая генерация ID
    val year: Int,
    val month: Int,
    val revenue: Double,
    val numberOfChecks: Int,
    val averageCheck: Double
)

@Entity(tableName = "reckoning_data")
data class ReckoningData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val month: Int,
    val region: String, // Название региона
    val electricityPrev: Double,
    val electricityCurr: Double,
    val gasPrev: Double,
    val gasCurr: Double,
    val hotWaterPrev: Double,
    val hotWaterCurr: Double,
    val coldWaterPrev: Double,
    val coldWaterCurr: Double,
    val S: Double,
    val M: Double
)