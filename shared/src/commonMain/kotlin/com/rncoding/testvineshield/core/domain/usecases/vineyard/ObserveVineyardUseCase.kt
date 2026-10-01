package com.rncoding.testvineshield.core.domain.usecases.vineyard

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

class ObserveVineyardUseCase(
    private val repository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        vineyardId: Long
    ): Flow<VineyardDomainModel?> {
        return authenticatedUserProvider
            .observeUser()
            .flatMapLatest { user ->
                if (user == null) {
                    emptyFlow()
                } else {
                    repository.observeVineyard(
                        userId = user.userId,
                        vineyardId = vineyardId
                    )
                }
            }
    }
}