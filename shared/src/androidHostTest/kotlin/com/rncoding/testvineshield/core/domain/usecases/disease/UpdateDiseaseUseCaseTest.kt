
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
import kotlin.test.assertIs

class UpdateDiseaseUseCaseTest {

    private val diseaseRepository = mockk<DiseaseRepository>()

    private val authenticatedUserProvider =
        mockk<AuthenticatedUserProvider>()

    private val validator = DiseaseValidator()

    private val useCase = UpdateDiseaseUseCase(
        diseaseRepository = diseaseRepository,
        authenticatedUserProvider = authenticatedUserProvider,
        validator = validator
    )

    private fun disease(
        diseaseId: Long = 10L,
        name: String = "Custom disease",
        code: String? = "CUSTOM-01",
        isPredefined: Boolean = false
    ) = DiseaseDomainModel(
        diseaseId = diseaseId,
        code = code,
        name = name,
        description = "Original description",
        vectors = "Original vectors",
        factors = "Original factors",
        treatmentSuggestion = "Original treatment",
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
    fun updatesCustomDiseaseSuccessfully(): Unit = runBlocking {
        givenExistingCustomDisease()

        coEvery {
            diseaseRepository.updateDisease(any())
        } returns true

        val result = useCase(
            disease(name = "Updated disease")
        )

        assertIs<Result.Success<Unit>>(result)

        coVerify(exactly = 1) {
            diseaseRepository.getDisease(10L)
        }

        coVerify(exactly = 1) {
            diseaseRepository.updateDisease(
                match {
                    it.diseaseId == 10L &&
                            it.name == "Updated disease" &&
                            !it.isPredefined
                }
            )
        }
    }

    @Test
    fun trimsAllTextFieldsBeforeUpdating(): Unit = runBlocking {
        givenExistingCustomDisease()

        val capturedDisease = slot<DiseaseDomainModel>()

        coEvery {
            diseaseRepository.updateDisease(
                capture(capturedDisease)
            )
        } returns true

        val result = useCase(
            disease(
                name = "  Updated disease  ",
                code = "  CUSTOM-02  "
            ).copy(
                description = "  Description  ",
                vectors = "  Vectors  ",
                factors = "  Factors  ",
                treatmentSuggestion = "  Treatment  "
            )
        )

        assertIs<Result.Success<Unit>>(result)

        val saved = capturedDisease.captured

        assertEquals(10L, saved.diseaseId)
        assertEquals("Updated disease", saved.name)
        assertEquals("CUSTOM-02", saved.code)
        assertEquals("Description", saved.description)
        assertEquals("Vectors", saved.vectors)
        assertEquals("Factors", saved.factors)
        assertEquals("Treatment", saved.treatmentSuggestion)
    }

    @Test
    fun unauthenticatedUserCannotUpdateDisease(): Unit = runBlocking {
        every {
            authenticatedUserProvider.currentUserOrNull()
        } returns null

        val result = useCase(disease())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(AuthError.NoActiveSession, error.error)

        coVerify(exactly = 0) {
            diseaseRepository.getDisease(any())
        }

        coVerify(exactly = 0) {
            diseaseRepository.updateDisease(any())
        }
    }

    @Test
    fun zeroDiseaseIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            disease(diseaseId = 0L)
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<DiseaseError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            diseaseRepository.updateDisease(any())
        }
    }

    @Test
    fun negativeDiseaseIdIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            disease(diseaseId = -1L)
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<DiseaseError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            diseaseRepository.updateDisease(any())
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
            diseaseRepository.updateDisease(any())
        }
    }

    @Test
    fun blankDiseaseCodeIsRejected(): Unit = runBlocking {
        givenAuthenticatedUser()

        val result = useCase(
            disease(code = "  ")
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<DiseaseError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            diseaseRepository.updateDisease(any())
        }
    }

    @Test
    fun missingDiseaseReturnsNotFound(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            diseaseRepository.getDisease(10L)
        } returns null

        val result = useCase(disease())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(DiseaseError.NotFound, error.error)

        coVerify(exactly = 0) {
            diseaseRepository.updateDisease(any())
        }
    }

    @Test
    fun predefinedDiseaseCannotBeUpdated(): Unit = runBlocking {
        givenAuthenticatedUser()

        coEvery {
            diseaseRepository.getDisease(10L)
        } returns disease(isPredefined = true)

        val result = useCase(
            disease(
                name = "Unauthorized modification",
                isPredefined = false
            )
        )

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(
            DiseaseError.PredefinedDisease,
            error.error
        )

        coVerify(exactly = 0) {
            diseaseRepository.updateDisease(any())
        }
    }

    @Test
    fun suppliedPredefinedFlagCannotPromoteCustomDisease(): Unit = runBlocking {
        givenExistingCustomDisease()

        val capturedDisease = slot<DiseaseDomainModel>()

        coEvery {
            diseaseRepository.updateDisease(
                capture(capturedDisease)
            )
        } returns true

        val result = useCase(
            disease(isPredefined = true)
        )

        assertIs<Result.Success<Unit>>(result)
        assertEquals(false, capturedDisease.captured.isPredefined)
    }

    @Test
    fun failedAtomicUpdateReturnsNotFound(): Unit = runBlocking {
        givenExistingCustomDisease()

        coEvery {
            diseaseRepository.updateDisease(any())
        } returns false

        val result = useCase(disease())

        val error = assertIs<Result.Error<*>>(result)
        assertEquals(DiseaseError.NotFound, error.error)
    }

    @Test
    fun repositoryExceptionReturnsUnexpectedError(): Unit = runBlocking {
        givenExistingCustomDisease()

        coEvery {
            diseaseRepository.updateDisease(any())
        } throws IllegalStateException("Database failure")

        val result = useCase(disease())

        val error = assertIs<Result.Error<*>>(result)
        assertIs<UnexpectedError>(error.error)
    }

    @Test
    fun cancellationExceptionIsRethrown(): Unit = runBlocking {
        givenExistingCustomDisease()

        coEvery {
            diseaseRepository.updateDisease(any())
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
            diseaseRepository.updateDisease(any())
        }
    }
}
