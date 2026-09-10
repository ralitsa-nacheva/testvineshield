package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.data.local.database.daos.UserDao
import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.mappers.UserMapper
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val userMapper: UserMapper,
    private val hasher: PasswordHasher,
    private val sessionRepository: SessionRepositoryImpl
): UserRepository {

    override suspend fun register(email: String, password: String): Result<UserDomainModel, AppError> {
        val existing = userDao.getUserByEmail(email)
        if (existing != null) {
            return Result.Error(AuthError.UserAlreadyExists)
        }

        val salt = hasher.generateSalt()
        val hash = hasher.hash(password, salt)

        val user = userDao.registerUser(
            UserEntity(
                userEmail = email,
                passHash = hash,
                passSalt = salt
               // createdAt = Clock.System.now().toEpochMilliseconds()
            )
        )

        val userDomain = userMapper.entityToDomain(user)
        return Result.Success(userDomain )// check to see how to change userId to UserDomainModel and is it necessary
    }

    // should i add safe call?
    override suspend fun login(email: String, password: String): Result<UserDomainModel, AppError> {
        val user = userDao.getUserByEmail(email)
            ?: return Result.Error(AuthError.InvalidCredentials)

        val valid = hasher.verify(password, user.passSalt, user.passHash)

        return if (valid) {
            Result.Success(userMapper.entityToDomain(user))
        } else {
            Result.Error(AuthError.InvalidCredentials)
        }
    }

    override suspend fun logout() {
        sessionRepository.clearSession()
    } // add code for session management?

    override suspend fun upsertUser(domainUser: UserDomainModel) { // maybe this should be changePassword function
        userDao.upsertUser(userMapper.domainToEntity(domainUser))
    }

   // override suspend fun observeUser(userId: Long): Flow<UserDomainModel?> { // duplicate with getuserbyid
     //   return userDao.getUserById(userId)
      //      .map{entity -> userMapper.entityToDomain(entity)}
   // }

    override suspend fun getUserById(userId: Long): Result<UserDomainModel, AppError> {

        val user = userDao.getUserById(userId)

        if (user == null) {
            return Result.Error(AuthError.UserAlreadyExists)
        }
        val userDomain = userMapper.entityToDomain(user)
        return Result.Success(userDomain )
        //return userDao.getUserById(userId)
          //  .map{entity -> userMapper.entityToDomain(entity)}
    }

    override suspend fun deleteUser(userId: Long) {
        userDao.deleteUser(userId)
    }
}