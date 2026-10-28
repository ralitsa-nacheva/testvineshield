
package com.rncoding.testvineshield.core.domain.usecases.disease

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DiseaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DeleteDiseaseUseCaseTest {

    private val diseaseRepository = mockk<DiseaseRepository>()

    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val useCase = DeleteDiseaseUseCase(
        diseaseRepository = diseaseRepository,
        authenticatedUserProvider = authenticatedUserProvider
    )

    private fun disease(
        diseaseId: Long = 10L,
        isPredefined: Boolean = false
    ) = DiseaseDomainModel(
        diseaseId = diseaseId,
        code = "CUSTOM-01",
        name = "Custom disease",
        description = "Test description",
        vectors = "Test vectors",
        factors = "Test factors",
        treatmentSuggestion = "Test treatment",
        isPredefined = isPredefined
    )

    private fun givenAuthenticatedUser() {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns UserDomainModel(
            userId = 1L,
            userEmail = "test@example.com"
        )
    }

    private fun givenExistingCustomDisease() {
        givenAuthenticatedUser()

        coEvery {
            diseaseRepository.getDisease(10L)
        } returns disease()
    }

    @Test
    fun deletesUnreferencedCustomDiseaseSuccessfully(): Unit = runBlocking {
        givenExistingCustomDisease()

        coEvery {
            diseaseRepository.isDiseaseReferenced(10L)
        } returns false

        coEvery {
            diseaseRepository.deleteDisease(10L)
        } returns true

        val result = useCase(10L)

        assertIs<Result.Success<Unit>>(result)

        coVerify(exactly = 1) {
            diseaseRepository.getDisease(10L)
        }

        coVerify(exactly = 1) {
            diseaseRepository.isDiseaseReferenced(10L)
        }

        coVerify(exactly = 1) {
            diseaseRepository.deleteDisease(10L)
        }
    }

    @Test
    fun unauthenticatedUserCannotDeleteDisease(): Unit = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns null

        val result = useCase(10L)

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            diseaseRepository.getDisease(any())
        }

        coVerify(exactly = 0) {
            diseaseRepository.deleteDisease(any())
        }
    }

    @Test
    fun zeroDiseaseIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(0L)

        val error = assertIs<Result.Error<*>>(result)
        assertIs<DiseaseError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            diseaseRepository.getDisease(any())
        }

        coVerify(exactly = 0) {
            diseaseRepository.deleteDisease(any())
        }
    }

    @Test
    fun negativeDiseaseIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(-1L)

        val error = assertIs<Result.Error<*>>(result)
        assertIs<DiseaseError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            diseaseRepository.deleteDisease(any())
        }
    }

    @Test
    fun missingDiseaseReturnsNotFound(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            diseaseRepository.getDisease(10L)
        } returns null

        val result = useCase(10L)

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(DiseaseError.NotFound, error.error)

        coVerify(exactly = 0) {
            diseaseRepository.isDiseaseReferenced(any())
        }

        coVerify(exactly = 0) {
            diseaseRepository.deleteDisease(any())
        }
    }

    @Test
    fun predefinedDiseaseCannotBeDeleted(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            diseaseRepository.getDisease(10L)
        } returns disease(isPredefined = true)

        val result = useCase(10L)

        val error = assertIs<Result.Error<*>>(result)

        assertEquals(
            DiseaseError.PredefinedDisease,
            error.error
        )

        coVerify(exactly = 0) {
            diseaseRepository.isDiseaseReferenced(any())
        }

        coVerify(exactly = 0) {
            diseaseRepository.deleteDisease(any())
        }
    }

    @Test
    fun referencedCustomDiseaseCannotBeDeleted(): Unit = runBlocking {
        givenExistingCustomDisease()

        coEvery {
            diseaseRepository.isDiseaseReferenced(10L)
        } returns true

        val result = useCase(10L)

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(DiseaseError.DiseaseInUse, error.error)

        coVerify(exactly = 0) {
            diseaseRepository.deleteDisease(any())
        }
    }

    @Test
    fun diseaseDeletedConcurrentlyReturnsNotFound(): Unit = runBlocking {
        givenExistingCustomDisease()

        coEvery {
            diseaseRepository.isDiseaseReferenced(10L)
        } returns false

        coEvery {
            diseaseRepository.deleteDisease(10L)
        } returns false

        coEvery {
            diseaseRepository.getDisease(10L)
        } returnsMany listOf(disease(), null)

        val result = useCase(10L)

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(DiseaseError.NotFound, error.error)

        coVerify(exactly = 2) {
            diseaseRepository.getDisease(10L)
        }
    }

    @Test
    fun diseaseBecomesPredefinedBeforeDeletion(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            diseaseRepository.getDisease(10L)
        } returnsMany listOf(
            disease(isPredefined = false),
            disease(isPredefined = true)
        )

        coEvery {
            diseaseRepository.isDiseaseReferenced(10L)
        } returns false

        coEvery {
            diseaseRepository.deleteDisease(10L)
        } returns false

        val result = useCase(10L)

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(
            DiseaseError.PredefinedDisease,
            error.error
        )
    }

    @Test
    fun diseaseBecomesReferencedBeforeDeletion(): Unit = runBlocking {
        givenExistingCustomDisease()

        coEvery {
            diseaseRepository.isDiseaseReferenced(10L)
        } returnsMany listOf(false, true)

        coEvery {
            diseaseRepository.deleteDisease(10L)
        } returns false

        val result = useCase(10L)

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(DiseaseError.DiseaseInUse, error.error)

        coVerify(exactly = 2) {
            diseaseRepository.isDiseaseReferenced(10L)
        }
    }

    @Test
    fun unexplainedDeletionFailureReturnsUnexpectedError(): Unit = runBlocking {
        givenExistingCustomDisease()

        coEvery {
            diseaseRepository.isDiseaseReferenced(10L)
        } returns false

        coEvery {
            diseaseRepository.deleteDisease(10L)
        } returns false

        val result = useCase(10L)

        val error = assertIs<Result.Error<*>>(result)
        assertIs<UnexpectedError>(error.error)
    }

    @Test
    fun repositoryExceptionReturnsUnexpectedError(): Unit = runBlocking {
        givenExistingCustomDisease()

        coEvery {
            diseaseRepository.isDiseaseReferenced(10L)
        } throws IllegalStateException("Database failure")

        val result = useCase(10L)

        val error = assertIs<Result.Error<*>>(result)
        assertIs<UnexpectedError>(error.error)

        coVerify(exactly = 0) {
            diseaseRepository.deleteDisease(any())
        }
    }

    @Test
    fun cancellationExceptionIsRethrown(): Unit = runBlocking {
        givenExistingCustomDisease()

        coEvery {
            diseaseRepository.isDiseaseReferenced(10L)
        } returns false

        coEvery {
            diseaseRepository.deleteDisease(10L)
        } throws CancellationException("Cancelled")

        assertFailsWith<CancellationException> {
            useCase(10L)
        }
    }
}
