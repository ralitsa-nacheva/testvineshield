package com.rncoding.testvineshield.core.domain.usecases.activity

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.export.*
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.usecases.block.GetBlockDetailsUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.GetVineyardUseCase
import kotlinx.coroutines.CancellationException

class PrepareSprayExportUseCase(
    private val users: AuthenticatedUserProvider,
    private val get: GetSprayRecordUseCase,
    private val vineyard: GetVineyardUseCase,
    private val block: GetBlockDetailsUseCase,
    private val encoder: SprayExportEncoder,
    private val clock: AppClock
) {
    suspend operator fun invoke(key: SprayKey, format: DocumentExportFormat): Result<PreparedDocument, AppError> {
        val owner = users.currentUserOrNull() ?: return Result.Error(AuthError.NoActiveSession)
        return try {
            val snapshot = when (val result = get(key)) { is Result.Error -> return Result.Error(result.error); is Result.Success -> result.data }
            val record = snapshot.record ?: return Result.Error(ActivityError.InvalidInput("Save a spray application before exporting it."))
            if (record.activityId != key.activityId || record.products.isEmpty()) return Result.Error(ActivityError.InvalidInput("Reload the saved spray application before exporting."))
            val parent = when (val result = vineyard(key.vineyardId)) { is Result.Error -> return Result.Error(result.error); is Result.Success -> result.data }
            val childId = snapshot.activity.blockId
            val childName = if (childId == null) null else when (val result = block(key.vineyardId, childId)) {
                is Result.Error -> return Result.Error(result.error); is Result.Success -> result.data.name
            }
            val activity = snapshot.activity
            val legacy = activity.treatmentProduct != null || activity.treatmentProductQuantity != null || activity.treatmentProductUnits != null || activity.waterQuantity != null || activity.treatmentProductRate != null
            val document = encoder.encode(SprayExportSnapshot(key.vineyardId, parent.name, childId, childName, activity.activityType,
                activity.season, clock.nowMillis(), legacy, record), format)
            if (users.currentUserOrNull()?.userId != owner.userId) return Result.Error(AuthError.NoActiveSession)
            Result.Success(document)
        } catch (e: CancellationException) { throw e }
        catch (_: SprayExportLimitException) { Result.Error(ActivityError.InvalidInput("The spray export is too large. Try JSON or review unusually long notes/names.")) }
        catch (e: Exception) { Result.Error(UnexpectedError(e)) }
    }
}
