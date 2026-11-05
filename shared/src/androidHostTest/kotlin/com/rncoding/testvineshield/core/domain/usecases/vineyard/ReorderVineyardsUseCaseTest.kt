package com.rncoding.testvineshield.core.domain.usecases.vineyard

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class ReorderVineyardsUseCaseTest {
    private val repository = mockk<VineyardRepository>()
    private val user = mockk<AuthenticatedUserProvider>()
    private val useCase = ReorderVineyardsUseCase(repository, user)

    @Test fun noSessionCannotReorder(): Unit = runBlocking {
        every { user.currentUserOrNull() } returns null
        assertEquals(Result.Error(AuthError.NoActiveSession), useCase(listOf(10)))
        coVerify(exactly = 0) { repository.reorderVineyards(any(), any()) }
    }

    @Test fun ownerComesFromSessionNotTheCaller(): Unit = runBlocking {
        every { user.currentUserOrNull() } returns UserDomainModel(7, "test@example.test")
        coEvery { repository.reorderVineyards(7, listOf(20, 10)) } returns Result.Success(Unit)
        assertEquals(Result.Success(Unit), useCase(listOf(20, 10)))
        coVerify(exactly = 1) { repository.reorderVineyards(7, listOf(20, 10)) }
    }

    @Test fun emptyOrderStillReachesOwnedDatabaseValidation(): Unit = runBlocking {
        every { user.currentUserOrNull() } returns UserDomainModel(7, "test@example.test")
        coEvery { repository.reorderVineyards(7, emptyList()) } returns Result.Success(Unit)
        assertEquals(Result.Success(Unit), useCase(emptyList()))
        coVerify(exactly = 1) { repository.reorderVineyards(7, emptyList()) }
    }
}
