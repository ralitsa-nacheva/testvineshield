package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskMonitoringConfigurationRepository
import com.rncoding.testvineshield.core.domain.disease_risk.MonitoringDraftSaveOutcome
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.BlockError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate

class SaveBotrytisMonitoringDraftUseCase(
    private val users: AuthenticatedUserProvider,
    private val monitoring: DiseaseRiskMonitoringConfigurationRepository
) {
    suspend operator fun invoke(
        vineyardId: Long, blockId: Long, startDate: LocalDate
    ): Result<MonitoringDraftSaveOutcome, AppError> {
        if (vineyardId <= 0 || blockId <= 0) return Result.Error(BlockError.InvalidInput("Invalid vineyard or block ID."))
        val owner = users.currentUserOrNull()?.userId ?: return Result.Error(AuthError.NoActiveSession)
        return try {
            val outcome = monitoring.saveBotrytisDraft(owner, vineyardId, blockId, startDate)
            if (users.currentUserOrNull()?.userId != owner) Result.Error(AuthError.NoActiveSession)
            else Result.Success(outcome)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.Error(UnexpectedError(error))
        }
    }
}
