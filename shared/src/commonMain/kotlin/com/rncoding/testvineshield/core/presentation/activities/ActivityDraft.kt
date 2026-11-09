package com.rncoding.testvineshield.core.presentation.activities

import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.*
import kotlinx.datetime.LocalDate

enum class ActivityField { Type, Description, Season, Planned, Completed, Tool, Notes }
data class ActivityDraft(
    val type: String = "",
    val description: String = "",
    val season: String = "",
    val planned: String = "",
    val completed: String = "",
    val tool: String = "",
    val notes: String = "",
    val priority: ActivityPriority = ActivityPriority.MEDIUM,
    val status: ActivityStatus = ActivityStatus.PLANNED,
    val stage: PhenologicalStage? = null
) {
    fun text(field: ActivityField): String = when(field) {
        ActivityField.Type -> type
        ActivityField.Description -> description
        ActivityField.Season -> season
        ActivityField.Planned -> planned
        ActivityField.Completed -> completed
        ActivityField.Tool -> tool
        ActivityField.Notes -> notes
    }
    fun changed(field: ActivityField, value: String): ActivityDraft = when(field) {
        ActivityField.Type -> copy(type=value)
        ActivityField.Description -> copy(description=value)
        ActivityField.Season -> copy(season=value)
        ActivityField.Planned -> copy(planned=value)
        ActivityField.Completed -> copy(completed=value)
        ActivityField.Tool -> copy(tool=value)
        ActivityField.Notes -> copy(notes=value)
    }
    fun savedValues(): ArrayList<String> = arrayListOf(type,description,season,planned,completed,tool,notes,priority.dbValue,status.dbValue,stage?.dbValue.orEmpty())
    companion object {
        fun from(record: ActivityDomainModel) = ActivityDraft(record.activityType,record.activityDescription,record.season,
            record.plannedAt.toString(),record.completedAt?.toString().orEmpty(),record.tool.orEmpty(),record.notes.orEmpty(),
            ActivityPriority.fromDbValue(record.priority),ActivityStatus.fromDbValue(record.status),record.phenologicalStage?.let(PhenologicalStage::fromDbValue))
        fun restore(values: List<String>): ActivityDraft? {
            if(values.size != 10) return null
            return try { ActivityDraft(values[0],values[1],values[2],values[3],values[4],values[5],values[6],
                ActivityPriority.fromDbValue(values[7]),ActivityStatus.fromDbValue(values[8]),values[9].takeIf {it.isNotEmpty()}?.let(PhenologicalStage::fromDbValue)) }
            catch(_: IllegalArgumentException) { null }
        }
    }
}
internal data class ActivityDraftResult(val record: ActivityDomainModel?, val errors: Map<ActivityField,String>)
internal fun ActivityDraft.parsed(vineyardId: Long, blockId: Long?, original: ActivityDomainModel?): ActivityDraftResult {
    val errors = mutableMapOf<ActivityField,String>()
    if(type.isBlank()) errors[ActivityField.Type] = "Activity type is required."
    if(season.isBlank()) errors[ActivityField.Season] = "Enter a season, e.g. 2026 or 2026/2027."
    fun date(field: ActivityField, required: Boolean): LocalDate? {
        val value=text(field).trim()
        if(value.isEmpty()) {
            if(required) errors[field]="An explicit date is required."
            return null
        }
        return try {LocalDate.parse(value)} catch(_: IllegalArgumentException) {
            errors[field]="Use a valid ISO date, e.g. 2026-11-09."
            null
        }
    }
    val plannedDate=date(ActivityField.Planned,true)
    val completedDate=date(ActivityField.Completed,status==ActivityStatus.COMPLETED)
    if(status!=ActivityStatus.COMPLETED && completed.isNotBlank()) errors[ActivityField.Completed]="Clear the completion date for a non-completed record."
    if(plannedDate!=null && completedDate!=null && completedDate<plannedDate) errors[ActivityField.Completed]="Completion cannot precede the planned date under the current record rule."
    if(errors.isNotEmpty() || plannedDate==null) return ActivityDraftResult(null,errors)
    // Copy hidden treatment values and immutable identity from the loaded record; never blank them on a basic edit.
    val template=original ?: ActivityDomainModel(0,vineyardId,blockId,"","","",null,"medium","planned",plannedDate,null,null,null,0,0,null,null,null,null,null)
    return ActivityDraftResult(template.copy(activityType=type.trim(),activityDescription=description.trim(),season=season.trim(),
        plannedAt=plannedDate,completedAt=completedDate,tool=tool.trim().takeIf {it.isNotEmpty()},notes=notes.trim().takeIf {it.isNotEmpty()},
        priority=priority.dbValue,status=status.dbValue,phenologicalStage=stage?.dbValue),errors)
}
