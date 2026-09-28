package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity
import com.rncoding.testvineshield.core.data.local.database.relations.DiseaseWithAlerts
import com.rncoding.testvineshield.core.data.local.database.relations.DiseaseWithOccurrences
import com.rncoding.testvineshield.core.data.local.database.relations.DiseaseWithSymptoms

@Dao
interface DiseaseDao {
    @Transaction
    @Upsert
    suspend fun upsertDisease(disease: DiseaseEntity)

    @Query("Select * From disease")
    fun observeDiseases(): Flow<List<DiseaseEntity>>

    @Transaction
    @Query("Delete From disease")
    suspend fun clearAllDiseases()

    @Transaction
    @Query("Select * From disease")
    fun getDiseaseWithSymptoms(): List<DiseaseWithSymptoms>

    @Transaction
    @Query("Select * From disease")
    fun getDiseaseWithOccurrences(): List<DiseaseWithOccurrences>

    @Transaction
    @Query("Select * From disease")
    fun getDiseaseWithAlerts(): List<DiseaseWithAlerts>

}