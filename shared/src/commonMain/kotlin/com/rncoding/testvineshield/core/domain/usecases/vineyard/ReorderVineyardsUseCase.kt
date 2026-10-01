package com.rncoding.testvineshield.core.domain.usecases.vineyard

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository

class ReorderVineyardsUseCase(
    private val repository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider
) {

    suspend operator fun invoke(
        vineyardIds: List<Long>
    ): Result<Unit, AppError> {

        val user =
            authenticatedUserProvider.currentUserOrNull()
                ?: return Result.Error(
                    AuthError.NoActiveSession
                )

        if (vineyardIds.isEmpty()) {
            return Result.Success(Unit)
        }

        return repository.reorderVineyards(
            userId = user.userId,
            vineyardIds = vineyardIds
        )
    }
}