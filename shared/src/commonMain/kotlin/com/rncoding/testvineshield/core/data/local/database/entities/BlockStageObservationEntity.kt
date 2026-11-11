package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "block_stage_observation",
    primaryKeys = ["block_id", "observed_on"],
    foreignKeys = [ForeignKey(
        entity = BlockEntity::class,
        parentColumns = ["block_id"],
        childColumns = ["block_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BlockStageObservationEntity(
    @ColumnInfo(name = "block_id") val blockId: Long,
    @ColumnInfo(name = "observed_on") val observedOn: LocalDate,
    val stage: PhenologicalStage
)
