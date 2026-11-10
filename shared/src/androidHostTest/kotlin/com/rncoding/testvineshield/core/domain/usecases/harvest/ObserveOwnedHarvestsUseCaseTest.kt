package com.rncoding.testvineshield.core.domain.usecases.harvest

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
class ObserveOwnedHarvestsUseCaseTest {
    private val users = mockk<AuthenticatedUserProvider>()
    private val vineyards = mockk<VineyardRepository>()
    private val blocks = mockk<BlockRepository>()
    private val harvests = mockk<HarvestRepository>()
    private val observe = ObserveOwnedHarvestsUseCase(users, vineyards, blocks, harvests)
    private val scope = HarvestScope(10, 20)
    private val child = BlockDomainModel(20, 10, "North", null, "Merlot", "", "", null, null, null, null)
    private fun signedIn() { every { users.observeUser() } returns flowOf(UserDomainModel(1, "owner@example.com")) }

    @Test fun invalidScopeDoesNotAccessRepositories() = runTest {
        assertIs<Result.Error<*>>(observe(HarvestScope(0, 20)).first())
        verify { users wasNot Called; harvests wasNot Called }
    }
    @Test fun unauthenticatedUserCannotReadHarvests() = runTest {
        every { users.observeUser() } returns flowOf(null)
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(observe(scope).first()).error)
        verify { harvests wasNot Called }
    }
    @Test fun absentOwnedParentDoesNotReadBlocksOrHarvests() = runTest {
        signedIn()
        every { vineyards.observeVineyard(1, 10) } returns flowOf(null)
        assertIs<Result.Error<*>>(observe(scope).first())
        verify { blocks wasNot Called; harvests wasNot Called }
    }
    @Test fun missingBlockDoesNotStartHarvestStream() = runTest {
        signedIn()
        every { vineyards.observeVineyard(1, 10) } returns flowOf(mockk<VineyardDomainModel>())
        every { blocks.observeBlocks(10) } returns flowOf(emptyList())
        assertEquals(BlockError.NotFound, assertIs<Result.Error<*>>(observe(scope).first()).error)
        verify { harvests wasNot Called }
    }
    @Test fun blockRemovalCancelsHarvestStreamAndClearsSuccess() = runTest {
        signedIn()
        every { vineyards.observeVineyard(1, 10) } returns flowOf(mockk<VineyardDomainModel>())
        val children = MutableStateFlow(listOf(child))
        every { blocks.observeBlocks(10) } returns children
        var cancelled = false
        every { harvests.observeHarvestsForBlock(20) } returns flow<List<HarvestDomainModel>> {
            try { emit(emptyList()); awaitCancellation() } finally { cancelled = true }
        }
        val results = mutableListOf<Result<HarvestNotebookSnapshot, AppError>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observe(scope).collect { results.add(it) } }
        runCurrent()
        assertEquals("North", assertIs<Result.Success<HarvestNotebookSnapshot>>(results.last()).data.blockName)
        children.value = emptyList(); runCurrent()
        assertEquals(BlockError.NotFound, assertIs<Result.Error<*>>(results.last()).error)
        assertTrue(cancelled)
    }
    @Test fun logoutCancelsOwnedChildStream() = runTest {
        val session = MutableStateFlow<UserDomainModel?>(UserDomainModel(1, "owner@example.com"))
        every { users.observeUser() } returns session
        every { vineyards.observeVineyard(1, 10) } returns flowOf(mockk<VineyardDomainModel>())
        every { blocks.observeBlocks(10) } returns flowOf(listOf(child))
        var cancelled = false
        every { harvests.observeHarvestsForBlock(20) } returns flow<List<HarvestDomainModel>> {
            try { emit(emptyList()); awaitCancellation() } finally { cancelled = true }
        }
        val results = mutableListOf<Result<HarvestNotebookSnapshot, AppError>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observe(scope).collect { results.add(it) } }
        runCurrent(); session.value = null; runCurrent()
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(results.last()).error)
        assertTrue(cancelled)
    }
    @Test fun ownershipRemovalClearsPreviouslyVisibleRecords() = runTest {
        signedIn()
        val ownership = MutableStateFlow<VineyardDomainModel?>(mockk<VineyardDomainModel>())
        every { vineyards.observeVineyard(1, 10) } returns ownership
        every { blocks.observeBlocks(10) } returns flowOf(listOf(child))
        every { harvests.observeHarvestsForBlock(20) } returns flowOf(emptyList())
        val results = mutableListOf<Result<HarvestNotebookSnapshot, AppError>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observe(scope).collect { results.add(it) } }
        runCurrent(); assertIs<Result.Success<HarvestNotebookSnapshot>>(results.last())
        ownership.value = null; runCurrent()
        assertIs<DatabaseError.NotFound>(assertIs<Result.Error<*>>(results.last()).error)
    }
    @Test fun storageFailureIsTypedAndCancellationStillPropagates() = runTest {
        signedIn()
        every { vineyards.observeVineyard(1, 10) } returns flow { throw IllegalStateException("offline storage failure") }
        assertIs<UnexpectedError>(assertIs<Result.Error<*>>(observe(scope).first()).error)
        // Cancel the collector job: throwing CancellationException in a
        // flatMapLatest child is not a reliable way to cancel its parent.
        val upstreamStarted = CompletableDeferred<Unit>()
        var upstreamCancelled = false
        every { vineyards.observeVineyard(1, 10) } returns flow<VineyardDomainModel?> {
            try {
                upstreamStarted.complete(Unit)
                awaitCancellation()
            } finally {
                upstreamCancelled = true
            }
        }
        val emissions = mutableListOf<Result<HarvestNotebookSnapshot, AppError>>()
        val collector = launch { observe(scope).collect { emissions.add(it) } }
        try {
            upstreamStarted.await()
            collector.cancelAndJoin()
            assertTrue(collector.isCancelled)
            assertTrue(upstreamCancelled)
            assertTrue(emissions.isEmpty(), "Cancellation must not become an error result.")
        } finally {
            collector.cancelAndJoin()
        }
    }
}
