package com.rncoding.testvineshield.core.domain.usecases.activity

import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.SprayRepository
import com.rncoding.testvineshield.core.domain.usecases.vineyard.GetVineyardUseCase
import com.rncoding.testvineshield.core.domain.validation.SprayValidator
import kotlinx.coroutines.CancellationException

data class SprayKey(val vineyardId: Long, val activityId: Long)
data class SpraySnapshot(val activity: ActivityDomainModel, val vineyardTimeZone: String, val record: SprayRecord?)

class GetSprayRecordUseCase(
    private val activity: GetActivityDetailsUseCase,
    private val vineyard: GetVineyardUseCase,
    private val repository: SprayRepository
) {
    suspend operator fun invoke(key: SprayKey): Result<SpraySnapshot, AppError> {
        return try {
            val parent = when (val result = activity(key.vineyardId, key.activityId)) {
                is Result.Error -> return Result.Error(result.error)
                is Result.Success -> result.data
            }
            val zone = when (val result = vineyard(key.vineyardId)) {
                is Result.Error -> return Result.Error(result.error)
                is Result.Success -> result.data.timeZone
            }
            Result.Success(SpraySnapshot(parent, zone, repository.get(key.vineyardId, key.activityId)))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.Error(UnexpectedError(e)) }
    }
}

private fun sprayResult(outcome: SprayWriteOutcome): Result<Unit, AppError> = when (outcome) {
    SprayWriteOutcome.SAVED -> Result.Success(Unit)
    SprayWriteOutcome.ACTIVITY_NOT_FOUND -> Result.Error(ActivityError.NotFound)
    SprayWriteOutcome.CONFLICT -> Result.Error(ActivityError.InvalidInput("This spray record changed or was removed. Reload before editing again; your draft has been kept."))
    SprayWriteOutcome.INVALID_LINE_ID -> Result.Error(ActivityError.InvalidInput("Product-line identities do not match this record. Reload before editing again."))
}

class SaveSprayRecordUseCase(
    private val activity: GetActivityDetailsUseCase,
    private val repository: SprayRepository,
    private val validator: SprayValidator
) {
    suspend operator fun invoke(key: SprayKey, record: SprayRecord): Result<Unit, AppError> {
        if (record.activityId != key.activityId) return Result.Error(ActivityError.InvalidInput("Spray activity identity cannot be changed."))
        validator.validate(record)?.let { return Result.Error(it) }
        return try {
            when (val parent = activity(key.vineyardId, key.activityId)) {
                is Result.Error -> Result.Error(parent.error)
                is Result.Success -> sprayResult(repository.save(key.vineyardId, record.copy(notes = record.notes?.trim()?.takeIf { it.isNotEmpty() },
                    products = record.products.map { it.copy(productName = it.productName.trim(), labelReference = it.labelReference?.trim()?.takeIf { text -> text.isNotEmpty() }) })))
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.Error(UnexpectedError(e)) }
    }
}

class DeleteSprayRecordUseCase(private val activity: GetActivityDetailsUseCase, private val repository: SprayRepository) {
    suspend operator fun invoke(key: SprayKey, expectedRevision: Long, generation: String): Result<Unit, AppError> {
        if (expectedRevision <= 0) return Result.Error(ActivityError.InvalidInput("Reload the saved spray record before deleting it."))
        return try {
            when (val parent = activity(key.vineyardId, key.activityId)) {
                is Result.Error -> Result.Error(parent.error)
                is Result.Success -> sprayResult(repository.delete(key.vineyardId, key.activityId, expectedRevision, generation))
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.Error(UnexpectedError(e)) }
    }
}
