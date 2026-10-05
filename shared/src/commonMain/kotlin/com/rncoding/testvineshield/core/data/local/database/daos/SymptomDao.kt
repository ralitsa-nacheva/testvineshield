package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.SymptomEntity
import com.rncoding.testvineshield.core.data.local.database.relations.SymptomWithDiseases
import com.rncoding.testvineshield.core.data.local.database.relations.SymptomWithOccurrences

@Dao
interface SymptomDao {
    @Transaction
    @Upsert
    suspend fun upsertSymptom(symptom: SymptomEntity)

    @Transaction
    @Upsert
    suspend fun upsertAllSymptom(symptoms: List<SymptomEntity>)

    @Query(
        """
    SELECT *
    FROM symptom
    ORDER BY name ASC, symptom_id ASC
    """
    )
    fun observeSymptoms(): Flow<List<SymptomEntity>>

    @Query("Select * From symptom Where symptom_id = :symptomId")
    suspend fun getSymptomById(symptomId: Long): SymptomEntity

    @Query("Select * From symptom")
    suspend fun getAllSymptoms(): List<SymptomEntity>

    @Transaction
    @Query("Delete From symptom")
    suspend fun clearAllSymptoms()

    @Transaction
    @Query("Select * From symptom")
    fun getSymptomWithDiseases(): List<SymptomWithDiseases>

    @Transaction
    @Query("SELECT * FROM symptom WHERE symptom_id = :symptomId")
    suspend fun getSymptomWithOccurrences(symptomId: Long): SymptomWithOccurrences?

    @Transaction
    @Query("SELECT * FROM symptom")
    suspend fun getAllSymptomsWithOccurrences(): List<SymptomWithOccurrences>
}