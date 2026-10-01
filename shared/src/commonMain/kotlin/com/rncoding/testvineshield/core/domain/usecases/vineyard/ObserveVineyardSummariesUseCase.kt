package com.rncoding.testvineshield.core.domain.usecases.vineyard

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.VineyardSummary
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

class ObserveVineyardSummariesUseCase(
    private val repository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<VineyardSummary>> {
        return authenticatedUserProvider
            .observeUser()
            .flatMapLatest { user ->
                if (user == null) {
                    emptyFlow()
                } else {
                    repository.observeVineyardSummaries(
                        userId = user.userId
                    )
                }
            }
    }

}