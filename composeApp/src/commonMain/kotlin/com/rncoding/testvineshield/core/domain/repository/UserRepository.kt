package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    suspend fun register(email: String, password: String): Result<UserDomainModel, AppError>

    suspend fun login(email: String, password: String): Result<UserDomainModel, AppError>

    //suspend fun logout() move it in session management

   // suspend fun observeUser(userId: Long): Flow<UserDomainModel?>

    suspend fun getUserById(userId: Long): Result<UserDomainModel?, AppError>
    suspend fun deleteUser(userId: Long)
}