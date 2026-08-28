package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "weather_calculation", foreignKeys = [ForeignKey(
    entity = ActivityEntity::class,
    parentColumns = ["activity_id"],
    childColumns = ["activity_id"],
    onDelete = ForeignKey.CASCADE
)]) //Add indices (vineyardId, calculatedAt) or (vineyardId, calculatedAt)
data class WeatherCalculationEntity(
    @PrimaryKey
    @ColumnInfo(name = "weather_calc_id")
    val weatherCalcId: Long,
    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long,
    @ColumnInfo(name = "calculated_at")
    val calculatedAt: LocalDate,
    @ColumnInfo(name = "count_wetness_hours_at_10C")
    val countWetnessHoursAt10C: Int, // 8-15 hours
    @ColumnInfo(name = "count_hours_at_21C")
    val countHoursAt21C: Int, // at least 6 hours at 21C
    @ColumnInfo(name = "powdery_mildew_initial_infection")
    val powderyMildewInitialInfection: Boolean,
    @ColumnInfo(name = "days_from_budburst")
    val daysFromBudBurst: Int,
    @ColumnInfo(name = "primary_incubation_days_count_pd")
    val primaryIncubationDaysCountPD: Int,
    @ColumnInfo(name = "secondary_incubation_days_count_pd")
    val secondaryIncubationDaysCountPD: Int,
    // Low index values of 0~30 indicate that the pathogen is not reproducing.
    // An index of 40~50 is considered moderate and implies a powdery mildew reproductive rate of approximately 15 days.
    // Index values over 60 indicate that the pathogen is reproducing rapidly (every 5 days) and that the risk
    // for a disease epidemic is great.
    @ColumnInfo(name = "infection_score")
    val infectionScore: Int,


    )