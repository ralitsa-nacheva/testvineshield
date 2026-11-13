package com.rncoding.testvineshield.core.data.local.database.daos

import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceSymptomCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.SymptomEntity
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseCatalogueCodes
import com.rncoding.testvineshield.core.domain.disease_risk.SymptomCatalogueCodes
import androidx.room.OnConflictStrategy
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceActivityCrossRef
import com.rncoding.testvineshield.core.domain.datamodels.CompletedBlockActivity
import com.rncoding.testvineshield.core.domain.datamodels.LinkedObservationActivity
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.ColumnInfo
import kotlinx.datetime.LocalDate
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.projections.DiseaseOccurrenceSummaryProjection
import com.rncoding.testvineshield.core.data.local.database.projections.DiseaseRiskSymptomProjection
import com.rncoding.testvineshield.core.data.local.database.relations.OccurrenceWithActivities
import com.rncoding.testvineshield.core.data.local.database.relations.OccurrenceWithSymptoms

data class OwnedBlockDiseaseObservationRow(
    @ColumnInfo(name = "occurrence_id") val id: Long,
    @ColumnInfo(name = "disease_name") val diseaseName: String,
    @ColumnInfo(name = "observed_at") val observedOn: LocalDate,
    val severity: Int,
    val status: DiseaseOccurrenceStatus,
    val notes: String?,
    @ColumnInfo(name = "cured_at") val curedAt: LocalDate?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "disease_code") val diseaseCode: String?,
    @ColumnInfo(name = "oil_spot_id") val oilSpotId: Long?,
    @ColumnInfo(name = "oil_spot_date") val oilSpotDate: LocalDate?,
    @ColumnInfo(name = "oil_spot_notes") val oilSpotNotes: String?
)

@Dao
interface DiseaseOccurrenceDao {
    @Query("""
        SELECT o.occurrence_id, d.name AS disease_name, o.observed_at, o.severity, o.status, o.notes,
               o.cured_at, o.updated_at, d.code AS disease_code,
               os.symptom_id AS oil_spot_id, os.observed_at AS oil_spot_date, os.notes AS oil_spot_notes
        FROM disease_occurrence o
        JOIN block b ON b.block_id = o.block_id AND b.vineyard_id = o.vineyard_id
        JOIN vineyard v ON v.vineyard_id = b.vineyard_id
        JOIN disease d ON d.disease_id = o.disease_id
        LEFT JOIN occurrence_symptom_cross_ref os
          ON os.occurrence_id = o.occurrence_id AND d.code = '${DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW}'
          AND os.symptom_id = (
              SELECT MIN(link.symptom_id)
              FROM occurrence_symptom_cross_ref link JOIN symptom s ON s.symptom_id = link.symptom_id
              WHERE link.occurrence_id = o.occurrence_id AND s.code = '${SymptomCatalogueCodes.DOWNY_MILDEW_OIL_SPOT}'
          )
        WHERE v.user_id = :ownerId AND v.vineyard_id = :vineyardId
          AND b.block_id = :blockId AND d.is_predefined = 1
        ORDER BY o.observed_at DESC, o.occurrence_id DESC
    """)
    fun observeOwnedBlockOccurrences(
        ownerId: Long, vineyardId: Long, blockId: Long
    ): Flow<List<OwnedBlockDiseaseObservationRow>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM block b JOIN vineyard v ON v.vineyard_id = b.vineyard_id
            WHERE v.user_id = :ownerId AND v.vineyard_id = :vineyardId AND b.block_id = :blockId
        )
    """)
    suspend fun ownsObservationBlock(ownerId: Long, vineyardId: Long, blockId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM disease WHERE disease_id = :diseaseId AND is_predefined = 1)")
    suspend fun isPredefinedObservationDisease(diseaseId: Long): Boolean

    @Insert
    suspend fun insertOccurrence(occurrence: DiseaseOccurrenceEntity): Long

    @Transaction
    suspend fun insertOwnedBlockOccurrence(ownerId: Long, occurrence: DiseaseOccurrenceEntity): Long? {
        val blockId = occurrence.blockId ?: return null
        if (!ownsObservationBlock(ownerId, occurrence.vineyardId, blockId)) return null
        if (!isPredefinedObservationDisease(occurrence.diseaseId)) return null
        return insertOccurrence(occurrence)
    }

    @Query("""
        UPDATE disease_occurrence
        SET status = :status, cured_at = :curedAt, updated_at = MAX(:updatedAt, updated_at + 1)
        WHERE occurrence_id = :occurrenceId
          AND vineyard_id = :vineyardId AND block_id = :blockId
          AND updated_at = :expectedUpdatedAt AND status != :status
          AND (:curedAt IS NULL OR :curedAt >= observed_at)
          AND EXISTS (
              SELECT 1 FROM block b JOIN vineyard v ON v.vineyard_id = b.vineyard_id
              WHERE v.user_id = :ownerId AND v.vineyard_id = :vineyardId AND b.block_id = :blockId
          )
          AND EXISTS (
              SELECT 1 FROM disease d WHERE d.disease_id = disease_occurrence.disease_id AND d.is_predefined = 1
          )
    """)
    suspend fun updateOwnedBlockOccurrenceStatus(
        ownerId: Long,
        vineyardId: Long,
        blockId: Long,
        occurrenceId: Long,
        expectedUpdatedAt: Long,
        status: DiseaseOccurrenceStatus,
        curedAt: LocalDate?,
        updatedAt: Long
    ): Int

    @Query("""
        UPDATE disease_occurrence
        SET observed_at = :observedOn, severity = :severity, notes = :notes,
            updated_at = MAX(:updatedAt, updated_at + 1)
        WHERE occurrence_id = :occurrenceId
          AND vineyard_id = :vineyardId AND block_id = :blockId
          AND updated_at = :expectedUpdatedAt
          AND (cured_at IS NULL OR :observedOn <= cured_at)
          AND EXISTS (
              SELECT 1 FROM block b JOIN vineyard v ON v.vineyard_id = b.vineyard_id
              WHERE v.user_id = :ownerId AND v.vineyard_id = :vineyardId AND b.block_id = :blockId
          )
          AND EXISTS (
              SELECT 1 FROM disease d WHERE d.disease_id = disease_occurrence.disease_id AND d.is_predefined = 1
          )
    """)
    suspend fun updateOwnedBlockOccurrenceDetails(
        ownerId: Long,
        vineyardId: Long,
        blockId: Long,
        occurrenceId: Long,
        expectedUpdatedAt: Long,
        observedOn: LocalDate,
        severity: Int,
        notes: String?,
        updatedAt: Long
    ): Int

    @Query("""
        SELECT EXISTS (
            SELECT 1 FROM disease_occurrence o
            JOIN block b ON b.block_id = o.block_id AND b.vineyard_id = o.vineyard_id
            JOIN vineyard v ON v.vineyard_id = b.vineyard_id
            JOIN disease d ON d.disease_id = o.disease_id
            WHERE o.occurrence_id = :occurrenceId AND o.updated_at = :expectedUpdatedAt
              AND v.user_id = :ownerId AND v.vineyard_id = :vineyardId AND b.block_id = :blockId
              AND d.is_predefined = 1 AND d.code = '${DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW}'
              AND (o.cured_at IS NULL OR :observedOn <= o.cured_at)
              AND NOT EXISTS (
                  SELECT 1 FROM occurrence_symptom_cross_ref link JOIN symptom s ON s.symptom_id = link.symptom_id
                  WHERE link.occurrence_id = o.occurrence_id AND s.code = '${SymptomCatalogueCodes.DOWNY_MILDEW_OIL_SPOT}'
              )
        )
    """)
    suspend fun canRecordOilSpot(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long,
        expectedUpdatedAt: Long, observedOn: LocalDate
    ): Boolean

    @Query("SELECT MIN(symptom_id) FROM symptom WHERE code = '${SymptomCatalogueCodes.DOWNY_MILDEW_OIL_SPOT}'")
    suspend fun findOilSpotSymptomId(): Long?

    @Insert
    suspend fun insertOilSpotReference(symptom: SymptomEntity): Long

    @Insert
    suspend fun insertObservedOilSpot(link: OccurrenceSymptomCrossRef)

    @Transaction
    suspend fun recordOwnedOilSpot(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long,
        expectedUpdatedAt: Long, observedOn: LocalDate, notes: String
    ): Boolean {
        if (!canRecordOilSpot(ownerId, vineyardId, blockId, occurrenceId, expectedUpdatedAt, observedOn)) return false
        val symptomId = findOilSpotSymptomId() ?: insertOilSpotReference(
            SymptomEntity(
                symptomId = 0,
                code = SymptomCatalogueCodes.DOWNY_MILDEW_OIL_SPOT,
                name = "Leaf oil spot",
                plantPart = "Leaf",
                phenologicalStage = PhenologicalStage.LEAF_DEVELOPMENT,
                notes = ""
            )
        )
        insertObservedOilSpot(OccurrenceSymptomCrossRef(occurrenceId, symptomId, observedOn, "", notes))
        return true
    }

    @Query("""
        SELECT a.activity_id AS activityId, a.activity_type AS activityType,
               a.activity_description AS description, a.completed_at AS completedOn
        FROM activity a
        JOIN block b ON b.block_id = a.block_id AND b.vineyard_id = a.vineyard_id
        JOIN vineyard v ON v.vineyard_id = b.vineyard_id
        WHERE v.user_id = :ownerId AND v.vineyard_id = :vineyardId AND b.block_id = :blockId
          AND a.status = 'completed' AND a.completed_at IS NOT NULL
        ORDER BY a.completed_at DESC, a.activity_id DESC
    """)
    fun observeCompletedActivitiesForLink(
        ownerId: Long, vineyardId: Long, blockId: Long
    ): Flow<List<CompletedBlockActivity>>

    @Query("""
        SELECT o.occurrence_id AS occurrenceId, a.activity_id AS activityId, a.activity_type AS activityType,
               a.activity_description AS description, x.applied_at AS appliedOn
        FROM OccurrenceActivityCrossRef x
        JOIN disease_occurrence o ON o.occurrence_id = x.occurrence_id
        JOIN activity a ON a.activity_id = x.activity_id
                       AND a.vineyard_id = o.vineyard_id
        JOIN block b ON b.block_id = o.block_id AND b.vineyard_id = o.vineyard_id
        JOIN vineyard v ON v.vineyard_id = b.vineyard_id
        JOIN disease d ON d.disease_id = o.disease_id
        WHERE v.user_id = :ownerId AND v.vineyard_id = :vineyardId AND b.block_id = :blockId
          AND d.is_predefined = 1
        ORDER BY x.applied_at DESC, a.activity_id DESC
    """)
    fun observeOwnedOccurrenceActivityLinks(
        ownerId: Long, vineyardId: Long, blockId: Long
    ): Flow<List<LinkedObservationActivity>>

    @Query("""
        SELECT EXISTS (
            SELECT 1 FROM disease_occurrence o
            JOIN block b ON b.block_id = o.block_id AND b.vineyard_id = o.vineyard_id
            JOIN vineyard v ON v.vineyard_id = b.vineyard_id
            JOIN disease d ON d.disease_id = o.disease_id
            JOIN activity a ON a.activity_id = :activityId
                           AND a.block_id = b.block_id AND a.vineyard_id = v.vineyard_id
            WHERE o.occurrence_id = :occurrenceId AND o.updated_at = :expectedUpdatedAt
              AND v.user_id = :ownerId AND v.vineyard_id = :vineyardId AND b.block_id = :blockId
              AND d.is_predefined = 1
              AND a.status = 'completed' AND a.completed_at = :completedOn
        )
    """)
    suspend fun canLinkCompletedActivity(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long,
        expectedUpdatedAt: Long, activityId: Long, completedOn: LocalDate
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrenceActivityLink(link: OccurrenceActivityCrossRef): Long

    @Transaction
    suspend fun linkOwnedCompletedActivity(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long,
        expectedUpdatedAt: Long, activityId: Long, completedOn: LocalDate, linkedOn: LocalDate
    ): Boolean {
        if (!canLinkCompletedActivity(
                ownerId, vineyardId, blockId, occurrenceId, expectedUpdatedAt, activityId, completedOn
            )
        ) return false
        return insertOccurrenceActivityLink(
            OccurrenceActivityCrossRef(occurrenceId, activityId, completedOn, linkedOn, "")
        ) != -1L
    }

    @Query("""
        DELETE FROM OccurrenceActivityCrossRef
        WHERE occurrence_id = :occurrenceId AND activity_id = :activityId
          AND EXISTS (
              SELECT 1 FROM disease_occurrence o
              JOIN block b ON b.block_id = o.block_id AND b.vineyard_id = o.vineyard_id
              JOIN vineyard v ON v.vineyard_id = b.vineyard_id
              JOIN disease d ON d.disease_id = o.disease_id
              JOIN activity a ON a.activity_id = :activityId AND a.vineyard_id = v.vineyard_id
              WHERE o.occurrence_id = :occurrenceId AND v.user_id = :ownerId
                AND v.vineyard_id = :vineyardId AND b.block_id = :blockId AND d.is_predefined = 1
          )
    """)
    suspend fun unlinkOwnedOccurrenceActivity(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long, activityId: Long
    ): Int

    @Query("""
        UPDATE occurrence_symptom_cross_ref
        SET observed_at = :observedOn, notes = :notes
        WHERE occurrence_id = :occurrenceId AND symptom_id = :symptomId
          AND observed_at = :previousDate AND notes = :previousNotes
          AND EXISTS (
              SELECT 1 FROM disease_occurrence o
              JOIN block b ON b.block_id = o.block_id AND b.vineyard_id = o.vineyard_id
              JOIN vineyard v ON v.vineyard_id = b.vineyard_id
              JOIN disease d ON d.disease_id = o.disease_id
              JOIN symptom s ON s.symptom_id = :symptomId
              WHERE o.occurrence_id = :occurrenceId AND o.updated_at = :expectedUpdatedAt
                AND v.user_id = :ownerId AND v.vineyard_id = :vineyardId AND b.block_id = :blockId
                AND d.is_predefined = 1 AND d.code = '${DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW}'
                AND s.code = '${SymptomCatalogueCodes.DOWNY_MILDEW_OIL_SPOT}'
                AND (o.cured_at IS NULL OR :observedOn <= o.cured_at)
          )
    """)
    suspend fun updateOwnedOilSpot(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long,
        expectedUpdatedAt: Long, symptomId: Long, previousDate: LocalDate, previousNotes: String,
        observedOn: LocalDate, notes: String
    ): Int

    @Query("""
        DELETE FROM occurrence_symptom_cross_ref
        WHERE occurrence_id = :occurrenceId AND symptom_id = :symptomId
          AND observed_at = :previousDate AND notes = :previousNotes
          AND EXISTS (
              SELECT 1 FROM disease_occurrence o
              JOIN block b ON b.block_id = o.block_id AND b.vineyard_id = o.vineyard_id
              JOIN vineyard v ON v.vineyard_id = b.vineyard_id
              JOIN disease d ON d.disease_id = o.disease_id
              JOIN symptom s ON s.symptom_id = :symptomId
              WHERE o.occurrence_id = :occurrenceId AND o.updated_at = :expectedUpdatedAt
                AND v.user_id = :ownerId AND v.vineyard_id = :vineyardId AND b.block_id = :blockId
                AND d.is_predefined = 1 AND d.code = '${DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW}'
                AND s.code = '${SymptomCatalogueCodes.DOWNY_MILDEW_OIL_SPOT}'
          )
    """)
    suspend fun removeOwnedOilSpot(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long,
        expectedUpdatedAt: Long, symptomId: Long, previousDate: LocalDate, previousNotes: String
    ): Int

    @Upsert
    @Transaction
    suspend fun upsertOccurrence(diseaseOccurrence: DiseaseOccurrenceEntity)

    @Query("Select * From disease_occurrence")
    fun observeOccurrences(): Flow<List<DiseaseOccurrenceEntity>>

    @Query(
        """
    SELECT *
    FROM disease_occurrence
    WHERE occurrence_id = :occurrenceId
    """
    )
    suspend fun getDiseaseOccurrenceById(
        occurrenceId: Long
    ): DiseaseOccurrenceEntity?

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

    @Query(
        """
    SELECT
        s.symptom_id AS symptom_id,
        s.code AS code,
        s.name AS name,
        s.plant_part AS plant_part,
        s.phenological_stage AS phenological_stage,
        os.observed_at AS observed_at,
        os.severity AS severity,
        os.notes AS notes
    FROM occurrence_symptom_cross_ref os
    INNER JOIN symptom s
        ON s.symptom_id = os.symptom_id
    WHERE os.occurrence_id = :occurrenceId
    ORDER BY os.observed_at DESC, s.symptom_id ASC
    """
    )
    suspend fun getRiskSymptomsForOccurrence(
        occurrenceId: Long
    ): List<DiseaseRiskSymptomProjection>

    @Query(
        """
    SELECT *
    FROM disease_occurrence
    WHERE block_id = :blockId
    ORDER BY observed_at DESC, occurrence_id DESC
    """
    )
    fun observeOccurrencesForBlock(
        blockId: Long
    ): Flow<List<DiseaseOccurrenceEntity>>

    @Query(
        """
    SELECT *
    FROM disease_occurrence
    WHERE vineyard_id = :vineyardId
      AND block_id = :blockId
      AND disease_id = :diseaseId
      AND status = 'active'
    ORDER BY observed_at DESC, occurrence_id DESC
    LIMIT 1
    """
    )
    suspend fun getActiveOccurrenceForRisk(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long
    ): DiseaseOccurrenceEntity?

}