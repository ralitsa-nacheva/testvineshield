package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceSymptomCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.SymptomEntity


@Dao
interface OccurrenceSymptomCrossRefDao {
    @Upsert
    suspend fun upsertCrossRefs(refs: List<OccurrenceSymptomCrossRef>)

    @Query("DELETE FROM OccurrenceSymptomCrossRef WHERE occurrence_id = :occurrenceId")
    suspend fun deleteByOccurrenceId(occurrenceId: Long)

    @Transaction
    @Query("SELECT * FROM symptom WHERE symptom_id IN (:symptomIds)")
    suspend fun getSymptomsByIds(symptomIds: List<Long>): List<SymptomEntity>
}