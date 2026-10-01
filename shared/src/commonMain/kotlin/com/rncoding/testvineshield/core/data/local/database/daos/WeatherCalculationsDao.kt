package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherCalculationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherCalculationsDao {

    @Upsert
    suspend fun upsertCalculation(
        weatherCalculation: WeatherCalculationEntity
    )

    @Query(
        """
        SELECT *
        FROM weather_calculation
        WHERE vineyard_id = :vineyardId
        ORDER BY calculated_at DESC
        LIMIT 1
        """
    )
    fun observeLatestCalculation(
        vineyardId: Long
    ): Flow<WeatherCalculationEntity?>

    @Query(
        """
        SELECT *
        FROM weather_calculation
        WHERE vineyard_id = :vineyardId
        ORDER BY calculated_at DESC
        """
    )
    fun observeCalculations(
        vineyardId: Long
    ): Flow<List<WeatherCalculationEntity>>

    @Query(
        """
        DELETE FROM weather_calculation
        """
    )
    suspend fun clearCalculations()
}