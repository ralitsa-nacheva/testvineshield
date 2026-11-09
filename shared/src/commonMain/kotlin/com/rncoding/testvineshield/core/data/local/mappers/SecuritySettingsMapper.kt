package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.SecuritySettingsEntity
import com.rncoding.testvineshield.core.domain.security.SecuritySettings

class SecuritySettingsMapper {

    fun entityToDomain(
        entity: SecuritySettingsEntity
    ): SecuritySettings {
        return SecuritySettings(
            biometricEnabled = entity.biometricEnabled,
            appPinEnabled = false,
            inactivityLockEnabled = entity.inactivityLockEnabled,
            inactivityTimeoutMillis = entity.inactivityTimeoutMillis,
            deviceCredentialEnabled = entity.deviceCredentialEnabled
        ).normalized()
    }

    fun domainToEntity(
        userId: Long,
        settings: SecuritySettings
    ): SecuritySettingsEntity {
        return SecuritySettingsEntity(
            userId = userId,
            biometricEnabled = settings.biometricEnabled,
            appPinEnabled = false,
            inactivityLockEnabled = settings.inactivityLockEnabled,
            inactivityTimeoutMillis = settings.inactivityTimeoutMillis,
            deviceCredentialEnabled = settings.deviceCredentialEnabled
        )
    }
}