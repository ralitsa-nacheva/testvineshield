package com.rncoding.testvineshield.core.domain.usecases.block

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveOwnedBlocksUseCaseTest {
    private val users = mockk<AuthenticatedUserProvider>()
    private val vineyards = mockk<VineyardRepository>()
    private val blocks = mockk<BlockRepository>()
    private val use = ObserveOwnedBlocksUseCase(blocks, vineyards, users)
    @Test fun noAuthenticatedUserNeverSubscribesToBlocks() = runTest {
        every {users.observeUser()} returns flowOf(null)
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(use(10).first()).error)
        verify(exactly = 0) {
            blocks.observeBlocks(any())
        }
    }
    @Test fun absentOwnedParentNeverSubscribesToBlocks() = runTest {
        every {users.observeUser()} returns flowOf(UserDomainModel(1, "owner@example.com"))
        every {vineyards.observeVineyard(1, 10)} returns flowOf(null)
        assertIs<Result.Error<*>>(use(10).first())
        verify(exactly = 0) {
            blocks.observeBlocks(any())
        }
    }
    @Test fun ownershipRemovalClearsSuccessAndStopsChildEmission() = runTest {
        val parent = MutableStateFlow<VineyardDomainModel?>(mockk<VineyardDomainModel>())
        val children = MutableStateFlow(listOf(BlockDomainModel(20, 10, "North", null, "Merlot", "", "", null, null, null, null)))
        every {users.observeUser()} returns flowOf(UserDomainModel(1, "owner@example.com"))
        every {vineyards.observeVineyard(1, 10)} returns parent
        every {blocks.observeBlocks(10)} returns children
        val results = mutableListOf<Result<List<BlockDomainModel>, AppError>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            use(10).collect { result ->
                results.add(result)
            }
        }
        runCurrent()
        assertIs<Result.Success<*>>(results.last())
        parent.value = null
        runCurrent()
        assertIs<Result.Error<*>>(results.last())
        val count = results.size
        children.value = emptyList()
        runCurrent()
        assertEquals(count, results.size)
    }
    @Test fun streamFailureBecomesRetryableTypedError() = runTest {
        every {users.observeUser()} returns flowOf(UserDomainModel(1, "owner@example.com"))
        every {vineyards.observeVineyard(1, 10)} returns flowOf(mockk<VineyardDomainModel>())
        every {blocks.observeBlocks(10)} returns flow {throw IllegalStateException("private DB text")}
        val error = assertIs<Result.Error<*>>(use(10).first()).error
        assertIs<UnexpectedError>(error)
        assertFalse(error.userMessage.contains("private DB text"))
    }
}
