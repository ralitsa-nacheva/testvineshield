package com.rncoding.testvineshield.core.domain.usecases.activity

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
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveActivityNotebookUseCaseTest {
    private val users=mockk<AuthenticatedUserProvider>()
    private val vineyards=mockk<VineyardRepository>()
    private val blocks=mockk<BlockRepository>()
    private val activities=mockk<ActivityRepository>()
    private val observe=ObserveActivityNotebookUseCase(users,vineyards,blocks,activities)
    @Test fun unauthenticatedCannotReadActivities()=runTest {
        every {users.observeUser()} returns flowOf(null)
        assertEquals(AuthError.NoActiveSession,assertIs<Result.Error<*>>(observe(ActivityScope(10)).first()).error)
        verify {activities wasNot Called}
    }
    @Test fun absentOwnedParentCannotReadActivities()=runTest {
        every {users.observeUser()} returns flowOf(UserDomainModel(1,"owner@example.com"))
        every {vineyards.observeVineyard(1,10)} returns flowOf(null)
        assertIs<Result.Error<*>>(observe(ActivityScope(10)).first())
        verify {activities wasNot Called}
    }
    @Test fun blockScopeFiltersAndOwnershipRemovalStopsRecords()=runTest {
        val parent=mockk<VineyardDomainModel>();every {parent.name} returns "My vines"
        val ownership=MutableStateFlow<VineyardDomainModel?>(parent)
        val child=BlockDomainModel(20,10,"North",null,"Merlot","","",null,null,null,null)
        val record=ActivityDomainModel(0,10,20,"Pruning","","2026",null,"medium","planned",LocalDate(2026,11,9),null,null,null,0,0,null,null,null,null,null)
        every {users.observeUser()} returns flowOf(UserDomainModel(1,"owner@example.com"))
        every {vineyards.observeVineyard(1,10)} returns ownership
        every {blocks.observeBlocks(10)} returns flowOf(listOf(child))
        every {activities.observeActivities(10)} returns flowOf(listOf(record.copy(activityId=30),record.copy(activityId=31,blockId=null)))
        val results=mutableListOf<Result<ActivityNotebookSnapshot,AppError>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {observe(ActivityScope(10,20)).collect {results.add(it)}}
        runCurrent();assertEquals(1,assertIs<Result.Success<ActivityNotebookSnapshot>>(results.last()).data.activities.size)
        ownership.value=null;runCurrent();assertIs<Result.Error<*>>(results.last())
    }
    @Test fun removedBlockIsErrorNotAnEmptyWholeVineyardFallback()=runTest {
        val parent=mockk<VineyardDomainModel>();every {parent.name} returns "My vines"
        every {users.observeUser()} returns flowOf(UserDomainModel(1,"owner@example.com"))
        every {vineyards.observeVineyard(1,10)} returns flowOf(parent)
        every {blocks.observeBlocks(10)} returns flowOf(emptyList())
        every {activities.observeActivities(10)} returns flowOf(emptyList())
        assertEquals(ActivityError.InvalidBlock,assertIs<Result.Error<*>>(observe(ActivityScope(10,20)).first()).error)
    }
}
