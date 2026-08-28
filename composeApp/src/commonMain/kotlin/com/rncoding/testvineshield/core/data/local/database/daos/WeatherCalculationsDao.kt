package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherCalculationEntity

@Dao
interface WeatherCalculationsDao {
    @Upsert
    @Transaction
    suspend fun upsertCalculation(weatherCalculations: WeatherCalculationEntity)

    @Query("Select * From weather_calculation")
    fun getCalculations(): Flow<WeatherCalculationEntity>

    @Query("Delete From weather_calculation")
    @Transaction
    suspend fun clearCalculations()
}