package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel

class UserMapper {
    fun domainToEntity(domainUser: UserDomainModel): UserEntity {
        return UserEntity(
            userId = domainUser.userId,
            userName = domainUser.userName,
            passHash = domainUser.passHash,
            passSalt = domainUser.passSalt
        )
    }
    fun entityToDomain(entityUser: UserEntity): UserDomainModel {
        return UserDomainModel(
            userId = entityUser.userId,
            userName = entityUser.userName,
            passHash = entityUser.passHash,
            passSalt = entityUser.passSalt
        )
    }
}