package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.SecuritySettingsEntity
import com.rncoding.testvineshield.core.domain.security.SecuritySettings

class SecuritySettingsMapper {

    fun entityToDomain(
        entity: SecuritySettingsEntity
    ): SecuritySettings {
        return SecuritySettings(
            biometricEnabled = entity.biometricEnabled,
            appPinEnabled = entity.appPinEnabled
        )
    }

    fun domainToEntity(
        userId: Long,
        settings: SecuritySettings
    ): SecuritySettingsEntity {
        return SecuritySettingsEntity(
            userId = userId,
            biometricEnabled = settings.biometricEnabled,
            appPinEnabled = settings.appPinEnabled
        )
    }
}