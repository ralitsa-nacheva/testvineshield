
package com.rncoding.testvineshield.core.domain.usecases.disease

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DiseaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import com.rncoding.testvineshield.core.domain.validation.DiseaseValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

class CreateDiseaseUseCaseTest {

    private val diseaseRepository = mockk<DiseaseRepository>()

    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val validator = DiseaseValidator()

    private val useCase = CreateDiseaseUseCase(
        diseaseRepository = diseaseRepository,
        authenticatedUserProvider = authenticatedUserProvider,
        validator = validator
    )

    private fun disease(
        diseaseId: Long = 0L,
        name: String = "Custom disease",
        code: String? = "CUSTOM-01",
        isPredefined: Boolean = false
    ) = DiseaseDomainModel(
        diseaseId = diseaseId,
        code = code,
        name = name,
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

    @Test
    fun createsCustomDiseaseSuccessfully(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            diseaseRepository.createDisease(any())
        } returns 42L

        val result = useCase(disease())

        val success = assertIs<Result.Success<Long>>(result)
        assertEquals(42L, success.data)

        coVerify(exactly = 1) {
            diseaseRepository.createDisease(any())
        }
    }

    @Test
    fun unauthenticatedUserCannotCreateDisease(): Unit = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns null

        val result = useCase(disease())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            diseaseRepository.createDisease(any())
        }
    }

    @Test
    fun existingDiseaseIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            disease(diseaseId = 10L)
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<DiseaseError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            diseaseRepository.createDisease(any())
        }
    }

    @Test
    fun blankDiseaseNameIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            disease(name = "   ")
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<DiseaseError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            diseaseRepository.createDisease(any())
        }
    }

    @Test
    fun blankDiseaseCodeIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            disease(code = "   ")
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<DiseaseError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            diseaseRepository.createDisease(any())
        }
    }

    @Test
    fun nullDiseaseCodeIsAllowed(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            diseaseRepository.createDisease(any())
        } returns 11L

        val result = useCase(
            disease(code = null)
        )

        val success = assertIs<Result.Success<Long>>(result)
        assertEquals(11L, success.data)

        coVerify(exactly = 1) {
            diseaseRepository.createDisease(
                match { it.code == null }
            )
        }
    }

    @Test
    fun trimsFieldsBeforeCreatingDisease(): Unit = runBlocking {
        givenAuthenticatedUser()

        val capturedDisease = slot<DiseaseDomainModel>()

        coEvery {
            diseaseRepository.createDisease(
                capture(capturedDisease)
            )
        } returns 15L

        val result = useCase(
            disease(
                name = "  Downy mildew  ",
                code = "  CUSTOM-02  "
            ).copy(
                description = "  Description  ",
                vectors = "  Vectors  ",
                factors = "  Factors  ",
                treatmentSuggestion = "  Treatment  "
            )
        )

        assertIs<Result.Success<Long>>(result)

        val saved = capturedDisease.captured

        assertEquals("Downy mildew", saved.name)
        assertEquals("CUSTOM-02", saved.code)
        assertEquals("Description", saved.description)
        assertEquals("Vectors", saved.vectors)
        assertEquals("Factors", saved.factors)
        assertEquals("Treatment", saved.treatmentSuggestion)
    }

    @Test
    fun predefinedFlagIsForcedToFalse(): Unit = runBlocking {
        givenAuthenticatedUser()

        val capturedDisease = slot<DiseaseDomainModel>()

        coEvery {
            diseaseRepository.createDisease(
                capture(capturedDisease)
            )
        } returns 20L

        val result = useCase(
            disease(isPredefined = true)
        )

        assertIs<Result.Success<Long>>(result)

        assertFalse(capturedDisease.captured.isPredefined)
        assertEquals(0L, capturedDisease.captured.diseaseId)
    }

    @Test
    fun invalidGeneratedIdReturnsUnexpectedError(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            diseaseRepository.createDisease(any())
        } returns 0L

        val result = useCase(disease())

        val error = assertIs<Result.Error<*>>(result)
        assertIs<UnexpectedError>(error.error)
    }

    @Test
    fun repositoryExceptionReturnsUnexpectedError(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            diseaseRepository.createDisease(any())
        } throws IllegalStateException("Database failure")

        val result = useCase(disease())

        val error = assertIs<Result.Error<*>>(result)
        assertIs<UnexpectedError>(error.error)
    }

    @Test
    fun cancellationExceptionIsRethrown(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            diseaseRepository.createDisease(any())
        } throws CancellationException("Cancelled")

        assertFailsWith<CancellationException> {
            useCase(disease())
        }
    }

    @Test
    fun reservedCodeIsRejectedBeforePersistence(): Unit = runBlocking {
        givenAuthenticatedUser()
        val result = useCase(disease(code = "  grape_downy_mildew  "))
        val error = assertIs<Result.Error<*>>(result)
        assertIs<DiseaseError.InvalidInput>(error.error)
        coVerify(exactly = 0) {
            diseaseRepository.createDisease(any())
        }
    }
}
