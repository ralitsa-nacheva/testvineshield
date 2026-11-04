package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.SecuritySettingsDao
import com.rncoding.testvineshield.core.data.local.mappers.SecuritySettingsMapper
import com.rncoding.testvineshield.core.domain.security.SecuritySettings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertFailsWith

class SecuritySettingsRepositoryCancellationTest {
    @Test
    fun allSuspendOperationsPropagateCancellation(): Unit = runBlocking {
        val dao = mockk<SecuritySettingsDao>()
        val repository = SecuritySettingsRepositoryImpl(dao, SecuritySettingsMapper())
        coEvery { dao.getSettings(any()) } throws CancellationException("Stopped")
        coEvery { dao.upsertSettings(any()) } throws CancellationException("Stopped")
        coEvery { dao.setBiometricEnabled(any(), any()) } throws CancellationException("Stopped")
        coEvery { dao.setPinEnabled(any(), any()) } throws CancellationException("Stopped")
        assertFailsWith<CancellationException> { repository.getSettings(1L) }
        assertFailsWith<CancellationException> { repository.saveSettings(1L, SecuritySettings(false, false)) }
        assertFailsWith<CancellationException> { repository.setBiometricEnabled(1L, true) }
        assertFailsWith<CancellationException> { repository.setPinEnabled(1L, true) }
    }
}
