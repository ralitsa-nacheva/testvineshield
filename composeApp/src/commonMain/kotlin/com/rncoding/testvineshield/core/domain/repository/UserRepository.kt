package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    suspend fun register(email: String, password: String): Result<UserDomainModel, AuthError> // does it have to return anything

    suspend fun login(email: String, password: String): Result<UserDomainModel, AuthError>

    suspend fun logout()

    suspend fun observeUser(userId: Long): Flow<UserDomainModel?>

    suspend fun getUserById(userId: Long): Flow<UserDomainModel?>

    suspend fun upsertUser(domainUser: UserDomainModel) // do i need this as i already have register function

    suspend fun deleteUser(userId: Long)
}