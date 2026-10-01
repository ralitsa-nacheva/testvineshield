package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.projections.DiseaseOccurrenceSummaryProjection
import com.rncoding.testvineshield.core.data.local.database.relations.OccurrenceWithActivities
import com.rncoding.testvineshield.core.data.local.database.relations.OccurrenceWithSymptoms

@Dao
interface DiseaseOccurrenceDao {
    @Upsert
    @Transaction
    suspend fun upsertOccurrence(diseaseOccurrence: DiseaseOccurrenceEntity)

    @Query("Select * From disease_occurrence")
    fun observeOccurrences(): Flow<List<DiseaseOccurrenceEntity>>

    @Query("Select * From disease_occurrence Where occurrence_id = :occurrenceId")
    suspend fun getDiseaseOccurrenceById(occurrenceId: Long): DiseaseOccurrenceEntity

    @Query("Delete From disease_occurrence Where occurrence_id = :occurrenceId")
    suspend fun deleteDiseaseOccurrenceById(occurrenceId: Long)

    @Query("Delete From disease_occurrence")
    @Transaction
    suspend fun clearAllOccurrences()

    @Transaction
    @Query("SELECT * FROM disease_occurrence WHERE occurrence_id = :occurrenceId")
    suspend fun getOccurrenceWithActivity(occurrenceId: Long): OccurrenceWithActivities

    @Transaction
    @Query("SELECT * FROM disease_occurrence")
    suspend fun getAllOccurrencesWithActivity(): List<OccurrenceWithActivities>

    @Transaction
    @Query("SELECT * FROM disease_occurrence WHERE occurrence_id = :occurrenceId")
    suspend fun getOccurrenceWithSymptoms(occurrenceId: Long): OccurrenceWithSymptoms

    @Transaction
    @Query("SELECT * FROM disease_occurrence")
    suspend fun getAllOccurrencesWithSymptoms(): List<OccurrenceWithSymptoms>

    @Query("Select Count(occurrence_id) From disease_occurrence where vineyard_id = :vineyardId")
    suspend fun countDiseaseOccurrencesForVineyard(vineyardId: Long): Int

    @Transaction //use this in vineyard summary aggregate
    @Query(
        """
    SELECT disease.name
    FROM disease
    INNER JOIN disease_occurrence
        ON disease.disease_id =
           disease_occurrence.disease_id
    WHERE disease_occurrence.vineyard_id = :vineyardId
      AND disease_occurrence.status = 'active'
    ORDER BY disease_occurrence.observed_at DESC,
             disease_occurrence.occurrence_id DESC
    LIMIT 1
    """
    )
    suspend fun getLastActiveDiseaseOccurrenceForVineyard(
        vineyardId: Long
    ): String?

    @Query(
        """
    SELECT
        d_o.occurrence_id AS occurrence_id,
        d_o.disease_id AS disease_id,
        d.name AS disease_name,
        d_o.block_id AS block_id,
        b.name AS block_name,
        d_o.observed_at AS observed_at,
        d_o.severity AS severity,
        d_o.status AS status
    FROM disease_occurrence d_o
    INNER JOIN disease d
        ON d.disease_id = d_o.disease_id
    LEFT JOIN block b
        ON b.block_id = d_o.block_id
    WHERE d_o.vineyard_id = :vineyardId
      AND d_o.status = 'active'
    ORDER BY d_o.observed_at DESC,
             d_o.occurrence_id DESC
    """
    )
    fun observeActiveDiseaseOccurrencesForVineyard(
        vineyardId: Long
    ): Flow<List<DiseaseOccurrenceSummaryProjection>>

}