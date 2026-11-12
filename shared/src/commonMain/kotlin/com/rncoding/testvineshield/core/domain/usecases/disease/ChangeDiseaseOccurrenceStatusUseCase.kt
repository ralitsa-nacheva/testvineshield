package com.rncoding.testvineshield.core.domain.usecases.disease

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockDiseaseObservation
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DiseaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.time.AppClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class ChangeDiseaseOccurrenceStatusUseCase(
    private val users: AuthenticatedUserProvider,
    private val vineyards: VineyardRepository,
    private val diseases: DiseaseRepository,
    private val clock: AppClock
) {
    suspend operator fun invoke(
        ownerId: Long,
        vineyardId: Long,
        blockId: Long,
        observation: BlockDiseaseObservation,
        status: DiseaseOccurrenceStatus
    ): Result<Unit, AppError> {
        if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
        if (status == observation.status) return Result.Error(DiseaseError.InvalidInput("Choose a different status."))

        return try {
            val now = clock.nowMillis()
            val curedAt = if (status == DiseaseOccurrenceStatus.RESOLVED) {
                val vineyard = when (val result = vineyards.getVineyard(ownerId, vineyardId)) {
                    is Result.Error -> return result
                    is Result.Success -> result.data
                }
                val zone = try {
                    TimeZone.of(vineyard.timeZone)
                } catch (_: IllegalArgumentException) {
                    return Result.Error(DiseaseError.InvalidInput("Set a valid vineyard time zone before resolving this observation."))
                }
                val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(zone).date
                if (today < observation.observedOn) {
                    return Result.Error(DiseaseError.InvalidInput("The resolution date cannot precede the observation date."))
                }
                today
            } else null

            currentCoroutineContext().ensureActive()
            if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
            val changed = diseases.updateOwnedBlockOccurrenceStatus(
                ownerId, vineyardId, blockId, observation.id, observation.updatedAt, status, curedAt, now
            )
            if (users.currentUserOrNull()?.userId != ownerId) Result.Error(AuthError.NoActiveSession)
            else if (changed) Result.Success(Unit)
            else Result.Error(DiseaseError.InvalidInput("This observation changed or is no longer available. Close and reopen it before retrying."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.Error(UnexpectedError(error))
        }
    }
}
