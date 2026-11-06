package com.rncoding.testvineshield.core.domain.usecases.vineyard

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryRiskScheduler
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.validation.VineyardValidator
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.*

class VineyardUnitUseCaseTest {
    private val repository = mockk<VineyardRepository>()
    private val user = mockk<AuthenticatedUserProvider>()
    private val clock = mockk<AppClock>()
    private val scheduler = mockk<WeatherHistoryRiskScheduler>(relaxed = true)
    private val date = LocalDate(2026, 11, 5)
    private val input = VineyardInput("Vineyard", 1.25, "BG", "Plovdiv", 42.1, 24.7,
        "Europe/Sofia", 200.0, AreaUnit.HECTARE)
    private fun authenticated() {
        every { user.currentUserOrNull() } returns UserDomainModel(1, "test@example.test")
        every { clock.today("Europe/Sofia") } returns date
    }
    @Test fun createCarriesSelectedUnitIntoOwnedRecord(): Unit = runBlocking {
        authenticated()
        coEvery { repository.getNextSortOrder(1) } returns Result.Success(0)
        coEvery { repository.createVineyard(any()) } returns Result.Success(10L)
        val useCase = CreateVineyardUseCase(repository, user, VineyardValidator(), clock, scheduler)
        assertEquals(Result.Success(10L), useCase(input))
        coVerify { repository.createVineyard(match { it.userId == 1L && it.size == 1.25 && it.sizeUnit == AreaUnit.HECTARE }) }
    }
    @Test fun updateCarriesUnitWithoutRewritingValueOrCreationDate(): Unit = runBlocking {
        authenticated()
        val original = VineyardDomainModel(10, 1, "Old", 1.25, "BG", "Plovdiv", 42.1, 24.7,
            "Europe/Sofia", 200.0, 3, LocalDate(2020, 1, 1), date, null)
        coEvery { repository.getVineyard(1, 10) } returns Result.Success(original)
        coEvery { repository.updateVineyard(any()) } returns Result.Success(Unit)
        val useCase = UpdateVineyardUseCase(repository, user, VineyardValidator(), clock)
        assertEquals(Result.Success(Unit), useCase(10, input))
        coVerify { repository.updateVineyard(match { it.sizeUnit == AreaUnit.HECTARE && it.size == 1.25 && it.createdAt == original.createdAt && it.sortOrder == 3 }) }
    }
}
