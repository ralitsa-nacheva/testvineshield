package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Entity(
    tableName = "weather_calculation",
    foreignKeys = [
        ForeignKey(
            entity = VineyardEntity::class,
            parentColumns = ["vineyard_id"],
            childColumns = ["vineyard_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("vineyard_id"),
        Index(value = ["vineyard_id", "calculated_at"])
    ]
)
data class WeatherCalculationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "weather_calc_id")
    val weatherCalcId: Long,

    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long,

    @ColumnInfo(name = "calculated_at")
    val calculatedAt: LocalDate,

    @ColumnInfo(name = "count_wetness_hours_at_10C")
    val countWetnessHoursAt10C: Int,

    @ColumnInfo(name = "count_hours_at_21C")
    val countHoursAt21C: Int,

    @ColumnInfo(name = "powdery_mildew_initial_infection")
    val powderyMildewInitialInfection: Boolean,

    @ColumnInfo(name = "days_from_budburst")
    val daysFromBudBurst: Int,

    @ColumnInfo(name = "primary_incubation_days_count_pd")
    val primaryIncubationDaysCountPD: Int,

    @ColumnInfo(name = "secondary_incubation_days_count_pd")
    val secondaryIncubationDaysCountPD: Int,

    @ColumnInfo(name = "infection_score")
    val infectionScore: Int
)