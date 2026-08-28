package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.relations.OccurrenceWithTreatments
import com.rncoding.testvineshield.core.data.local.database.relations.OccurrenceWithSymptoms

@Dao
interface DiseaseOccurrenceDao {
    @Upsert
    @Transaction
    suspend fun upsertOccurrence(diseaseOccurrence: DiseaseOccurrenceEntity)

    @Query("Select * From disease_occurrence")
    suspend fun getAllOccurrences(): Flow<List<DiseaseOccurrenceEntity>>

    @Query("Select * From disease_occurrence Where occurrence_id = :occurrenceId")
    suspend fun getDiseaseOccurrenceById(occurrenceId: Long): DiseaseOccurrenceEntity

    @Query("Delete From disease_occurrence Where occurrence_id = :occurrenceId")
    suspend fun deleteDiseaseOccurrenceById(occurrenceId: Long)

    @Query("Delete From disease_occurrence")
    @Transaction
    suspend fun clearAllOccurrences()

    @Transaction
    @Query("SELECT * FROM disease_occurrence WHERE occurrence_id = :occurrenceId")
    suspend fun getOccurrenceWithActivity(occurrenceId: Long): OccurrenceWithTreatments

    @Transaction
    @Query("SELECT * FROM disease_occurrence")
    suspend fun getAllOccurrencesWithActivity(): List<OccurrenceWithTreatments>

    @Transaction
    @Query("SELECT * FROM disease_occurrence WHERE occurrence_id = :occurrenceId")
    suspend fun getOccurrenceWithSymptoms(occurrenceId: Long): OccurrenceWithSymptoms

    @Transaction
    @Query("SELECT * FROM disease_occurrence")
    suspend fun getAllOccurrencesWithSymptoms(): List<OccurrenceWithSymptoms>

    @Query("Select Count(occurrence_id) From disease_occurrence where vineyard_id = :vineyardId")
    suspend fun countDiseaseOccurrencesForVineyard(vineyardId: Long): Int

    @Transaction //use this in vineyard summary aggregate
    @Query("Select disease.name From disease Inner Join disease_occurrence " +
            "On disease.disease_id = disease_occurrence.disease_id " +
            "Where disease_occurrence.vineyard_id = :vineyardId And disease_occurrence.status = 'Open' " +
            "And created_at = (Select Max(created_at) From disease_occurrence " +
            "Where disease_occurrence.vineyard_id = :vineyardId)")
    suspend fun getLastOpenDiseaseOccurrenceForVineyard(vineyardId: Long): String

}