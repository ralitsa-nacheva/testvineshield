package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel

class UserMapper {
        fun entityToDomain(entityUser: UserEntity): UserDomainModel {
        return UserDomainModel(
            userId = entityUser.userId,
            userEmail = entityUser.email
        )
    }
}