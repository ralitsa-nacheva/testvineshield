package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity
import com.rncoding.testvineshield.core.data.local.database.relations.DiseaseWithAlerts
import com.rncoding.testvineshield.core.data.local.database.relations.DiseaseWithOccurrences
import com.rncoding.testvineshield.core.data.local.database.relations.DiseaseWithSymptoms

@Dao
interface DiseaseDao {


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPredefinedDiseases(
        diseases: List<DiseaseEntity>
    ): List<Long>


    @Query(
        """
    SELECT *
    FROM disease
    ORDER BY name ASC, disease_id ASC
    """
    )
    fun observeDiseases(): Flow<List<DiseaseEntity>>

    @Transaction
    @Query("Select * From disease")
    fun getDiseaseWithSymptoms(): List<DiseaseWithSymptoms>

    @Transaction
    @Query("Select * From disease")
    fun getDiseaseWithOccurrences(): List<DiseaseWithOccurrences>

    @Transaction
    @Query("Select * From disease")
    fun getDiseaseWithAlerts(): List<DiseaseWithAlerts>


    @Query(
        """
    SELECT *
    FROM disease
    WHERE disease_id = :diseaseId
    LIMIT 1
    """
    )
    suspend fun getDiseaseById(
        diseaseId: Long
    ): DiseaseEntity?

    @Insert
    suspend fun insertDisease(
        disease: DiseaseEntity
    ): Long

    @Query(
        """
    UPDATE disease
    SET
        code = :code,
        name = :name,
        description = :description,
        vectors = :vectors,
        factors = :factors,
        treatment_suggestion = :treatmentSuggestion
    WHERE disease_id = :diseaseId
      AND is_predefined = 0
    """
    )
    suspend fun updateCustomDisease(
        diseaseId: Long,
        code: String?,
        name: String,
        description: String,
        vectors: String,
        factors: String,
        treatmentSuggestion: String
    ): Int

    @Query(
        """
    DELETE FROM disease
    WHERE disease_id = :diseaseId
      AND is_predefined = 0
      AND NOT EXISTS (
          SELECT 1
          FROM disease_occurrence
          WHERE disease_occurrence.disease_id = :diseaseId
      )
      AND NOT EXISTS (
          SELECT 1
          FROM disease_alert
          WHERE disease_alert.disease_id = :diseaseId
      )
    """
    )
    suspend fun deleteUnreferencedCustomDisease(
        diseaseId: Long
    ): Int


    @Query(
        """
    SELECT (
        EXISTS (
            SELECT 1
            FROM disease_occurrence
            WHERE disease_id = :diseaseId
        )
        OR
        EXISTS (
            SELECT 1
            FROM disease_alert
            WHERE disease_id = :diseaseId
        )
    )
    """
    )
    suspend fun isDiseaseReferenced(
        diseaseId: Long
    ): Boolean


    @Query(
        """
    SELECT *
    FROM disease
    WHERE code = :code
    LIMIT 1
    """
    )
    suspend fun getDiseaseByCode(
        code: String
    ): DiseaseEntity?



    @Transaction
    suspend fun seedPredefinedDiseases(
        diseases: List<DiseaseEntity>
    ) {
        for (disease in diseases) {
            val code = requireNotNull(disease.code)

            val existing = getDiseaseByCode(code)

            if (existing != null) {
                check(existing.isPredefined) {
                    "Reserved disease code '$code' is used by a custom disease."
                }
                continue
            }

            val insertedIds = insertPredefinedDiseases(
                listOf(disease)
            )

            check(insertedIds.single() > 0L) {
                "Could not insert predefined disease '$code'."
            }
        }
    }


}