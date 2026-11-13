package com.rncoding.testvineshield.core.domain.usecases.disease

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockDiseaseObservation
import com.rncoding.testvineshield.core.domain.datamodels.CompletedBlockActivity
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

class ObservationActivityLinkUseCase(
    private val users: AuthenticatedUserProvider,
    private val vineyards: VineyardRepository,
    private val diseases: DiseaseRepository,
    private val clock: AppClock
) {
    suspend fun link(
        ownerId: Long, vineyardId: Long, blockId: Long,
        observation: BlockDiseaseObservation, activity: CompletedBlockActivity
    ): Result<Unit, AppError> {
        if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
        return try {
            val vineyard = when (val result = vineyards.getVineyard(ownerId, vineyardId)) {
                is Result.Error -> return result
                is Result.Success -> result.data
            }
            val zone = try {
                TimeZone.of(vineyard.timeZone)
            } catch (_: IllegalArgumentException) {
                return Result.Error(DiseaseError.InvalidInput("Set a valid vineyard time zone before linking an activity."))
            }
            val today = Instant.fromEpochMilliseconds(clock.nowMillis()).toLocalDateTime(zone).date
            if (activity.completedOn > today) {
                return Result.Error(DiseaseError.InvalidInput("The activity completion date is in the future for this vineyard."))
            }
            currentCoroutineContext().ensureActive()
            if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
            val linked = diseases.linkOwnedCompletedActivity(
                ownerId, vineyardId, blockId, observation.id, observation.updatedAt,
                activity.activityId, activity.completedOn, today
            )
            if (users.currentUserOrNull()?.userId != ownerId) Result.Error(AuthError.NoActiveSession)
            else if (linked) Result.Success(Unit)
            else Result.Error(DiseaseError.InvalidInput("The observation or activity changed, or this activity is already linked. Reopen the dialog."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.Error(UnexpectedError(error))
        }
    }

    suspend fun unlink(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long, activityId: Long
    ): Result<Unit, AppError> {
        if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
        return try {
            currentCoroutineContext().ensureActive()
            val removed = diseases.unlinkOwnedOccurrenceActivity(ownerId, vineyardId, blockId, occurrenceId, activityId)
            if (users.currentUserOrNull()?.userId != ownerId) Result.Error(AuthError.NoActiveSession)
            else if (removed) Result.Success(Unit)
            else Result.Error(DiseaseError.InvalidInput("This activity link is no longer available."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.Error(UnexpectedError(error))
        }
    }
}
