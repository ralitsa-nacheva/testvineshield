package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalAuthenticationPromptTest {
    @Test fun legacyImplementationReceivesReasonAndMethodThroughBridge() = runTest {
        var received: Pair<String, LocalUnlockMethod>? = null
        val authenticator = object : LocalAuthenticator {
            override suspend fun isAvailable(): Result<Boolean, AppError> = Result.Success(true)
            override suspend fun authenticate(reason: String): Result<Unit, AppError> = Result.Success(Unit)
            override suspend fun authenticate(reason: String, method: LocalUnlockMethod): Result<Unit, AppError> {
                received = reason to method
                return Result.Success(Unit)
            }
        }
        val prompt = LocalAuthenticationPrompt("Отключете профила си", "Отключване", "Отказ")
        authenticator.authenticateWithPrompt(prompt, LocalUnlockMethod.DeviceCredential)
        assertEquals(prompt.reason to LocalUnlockMethod.DeviceCredential, received)
    }
}
