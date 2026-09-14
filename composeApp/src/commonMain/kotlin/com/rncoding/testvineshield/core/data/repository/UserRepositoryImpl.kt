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

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val userMapper: UserMapper,
    private val hasher: PasswordHasher
) : UserRepository {

    override suspend fun register(
        email: String,
        password: String
    ): Result<UserDomainModel, AppError> {

        val normalizedEmail =
            email.trim()

        /*
         * Friendly early check.
         */
        val existingUser =
            userDao.getUserByEmail(normalizedEmail)

        if (existingUser != null) {
            return Result.Error(
                AuthError.UserAlreadyExists
            )
        }

        /*
         * Generate a unique salt for this user.
         */
        val salt =
            hasher.generateSalt()

        /*
         * Never store the plaintext password.
         */
        val hash =
            hasher.hash(
                password = password,
                salt = salt
            )

        val userEntity =
            UserEntity(
                email = normalizedEmail,
                passwordHash = hash,
                passwordSalt = salt
            )

        /*
         * IGNORE means a database-level unique-email
         * conflict will not replace an existing user.
         *
         * Room returns -1 when the insert is ignored.
         */
        val generatedUserId =
            userDao.insertUser(userEntity)

        if (generatedUserId == -1L) {
            return Result.Error(
                AuthError.UserAlreadyExists
            )
        }

        val savedUser =
            userEntity.copy(
                userId = generatedUserId
            )

        return Result.Success(
            userMapper.entityToDomain(savedUser)
        )
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<UserDomainModel, AppError> {

        val normalizedEmail =
            email.trim()

        val user =
            userDao.getUserByEmail(normalizedEmail)
                ?: return Result.Error(
                    AuthError.InvalidCredentials
                )

        val valid =
            hasher.verify(
                password = password,
                salt = user.passwordSalt,
                hash = user.passwordHash
            )

        if (!valid) {
            return Result.Error(
                AuthError.InvalidCredentials
            )
        }

        return Result.Success(
            userMapper.entityToDomain(user)
        )
    }

    override suspend fun getUserById(
        userId: Long
    ): Result<UserDomainModel?, AppError> {

        val user =
            userDao.getUserById(userId)

        return Result.Success(
            user?.let(userMapper::entityToDomain)
        )
    }
    override suspend fun deleteUser(userId: Long) {
        userDao.deleteUser(userId)
    }
}