package com.rncoding.testvineshield.core.domain.usecases.alerts

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertInboxItem
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertStatus
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.DiseaseAlertRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*

class ObserveOwnedDiseaseAlertsUseCase(
    private val users: AuthenticatedUserProvider,
    private val repository: DiseaseAlertRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(ownerId: Long): Flow<Result<List<DiseaseAlertInboxItem>, AppError>> =
        users.observeUser().flatMapLatest { user ->
            if (ownerId <= 0 || user?.userId != ownerId) {
                flowOf<Result<List<DiseaseAlertInboxItem>, AppError>>(Result.Error(AuthError.NoActiveSession))
            } else repository.observeOwnedInbox(ownerId)
                .map<List<DiseaseAlertInboxItem>, Result<List<DiseaseAlertInboxItem>, AppError>> { Result.Success(it) }
        }.buffer(0).catch { error ->
            if (error is CancellationException) throw error
            emit(Result.Error(UnexpectedError(error)))
        }
}

class AcknowledgeDiseaseAlertUseCase(
    private val users: AuthenticatedUserProvider,
    private val repository: DiseaseAlertRepository
) {
    suspend operator fun invoke(ownerId: Long, expected: DiseaseAlertInboxItem): Result<Unit, AppError> {
        if (users.currentUserOrNull()?.userId != ownerId || ownerId <= 0) return Result.Error(AuthError.NoActiveSession)
        if (expected.alertId <= 0 || expected.vineyardId <= 0 || expected.status != AlertStatus.ACTIVE)
            return Result.Error(DatabaseError.NotFound("active disease alert"))
        return try {
            val changed = repository.acknowledgeOwned(ownerId, expected)
            currentCoroutineContext().ensureActive()
            if (users.currentUserOrNull()?.userId != ownerId) Result.Error(AuthError.NoActiveSession)
            else if (changed) Result.Success(Unit)
            else Result.Error(DatabaseError.ConstraintViolation(DatabaseError.Operation.UPDATE, "Alert changed or is no longer available; reload"))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.Error(UnexpectedError(error))
        }
    }
}
