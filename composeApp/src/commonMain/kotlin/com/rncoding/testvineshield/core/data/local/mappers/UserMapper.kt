package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel

class UserMapper {
    fun domainToEntity(domainUser: UserDomainModel): UserEntity {
        return UserEntity(
            userId = domainUser.userId,
            userEmail = domainUser.userEmail,
            passHash = domainUser.passHash,
            passSalt = domainUser.passSalt
           // createdAt = domainUser.createdAt
        )
    }
    fun entityToDomain(entityUser: UserEntity): UserDomainModel {
        return UserDomainModel(
            userId = entityUser.userId,
            userEmail = entityUser.userEmail,
            passHash = entityUser.passHash,
            passSalt = entityUser.passSalt
        )
    }
}