package com.rncoding.testvineshield.core.domain.usecases.vineyard

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository

class GetVineyardUseCase(
    private val repository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider
) {

    suspend operator fun invoke(
        vineyardId: Long
    ): Result<VineyardDomainModel, AppError> {

        val user =
            authenticatedUserProvider.currentUserOrNull()
                ?: return Result.Error(
                    AuthError.NoActiveSession
                )

        return repository.getVineyard(
            userId = user.userId,
            vineyardId = vineyardId
        )
    }
}