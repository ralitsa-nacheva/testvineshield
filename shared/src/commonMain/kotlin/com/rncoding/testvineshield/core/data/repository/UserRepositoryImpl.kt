package com.rncoding.testvineshield.core.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.data.local.database.daos.UserDao
import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.mappers.UserMapper
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.security.PasswordHasher

class UserRepositoryImpl(
    private val database: VineshieldDatabase,
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

    override suspend fun updateUser(
        userId: Long,
        currentPassword: String,
        newEmail: String,
        newPassword: String?
    ): Result<UserDomainModel, AppError> {

        return try {

            val existingUser =
                userDao.getUserById(userId)
                    ?: return Result.Error(
                        AuthError.UserNotFound
                    )

            val valid =
                hasher.verify(
                    password = currentPassword,
                    salt = existingUser.passwordSalt,
                    hash = existingUser.passwordHash
                )

            if (!valid) {
                return Result.Error(
                    AuthError.InvalidCredentials
                )
            }

            val normalizedEmail =
                newEmail.trim()

            /*
             * If the email is changing, check whether another
             * account already owns it.
             */
            if (normalizedEmail != existingUser.email) {

                val emailOwner =
                    userDao.getUserByEmail(normalizedEmail)

                if (
                    emailOwner != null &&
                    emailOwner.userId != userId
                ) {
                    return Result.Error(
                        AuthError.UserAlreadyExists
                    )
                }
            }

            /*
             * Keep the existing password unless the caller
             * explicitly supplied a new password.
             */
            val finalSalt: String
            val finalHash: String

            if (newPassword != null) {

                finalSalt =
                    hasher.generateSalt()

                finalHash =
                    hasher.hash(
                        password = newPassword,
                        salt = finalSalt
                    )

            } else {

                finalSalt =
                    existingUser.passwordSalt

                finalHash =
                    existingUser.passwordHash
            }

            val rowsUpdated =
                database
                    .useWriterConnection { connection ->

                        connection.immediateTransaction {

                            userDao.updateUser(
                                userId = userId,
                                email = normalizedEmail,
                                passwordHash = finalHash,
                                passwordSalt = finalSalt
                            )
                        }
                    }

            if (rowsUpdated == 0) {
                return Result.Error(
                    AuthError.UserNotFound
                )
            }

            val updatedUser =
                userDao.getUserById(userId)
                    ?: return Result.Error(
                        AuthError.UserNotFound
                    )

            Result.Success(
                userMapper.entityToDomain(updatedUser)
            )

        } catch (e: Exception) {

            Result.Error(
                DatabaseError.Unknown(
                    operation = DatabaseError.Operation.UPDATE,
                    cause = e
                )
            )
        }
    }

    override suspend fun deleteUser(
        userId: Long,
        currentPassword: String
    ): Result<Unit, AppError> {

        return try {

            val user =
                userDao.getUserById(userId)
                    ?: return Result.Error(
                        AuthError.UserNotFound
                    )

            val valid =
                hasher.verify(
                    password = currentPassword,
                    salt = user.passwordSalt,
                    hash = user.passwordHash
                )

            if (!valid) {
                return Result.Error(
                    AuthError.InvalidCredentials
                )
            }

            val rowsDeleted =
                userDao.deleteUser(userId)

            if (rowsDeleted == 0) {
                return Result.Error(
                    AuthError.UserNotFound
                )
            }

            Result.Success(Unit)

        } catch (e: Exception) {

            Result.Error(
                DatabaseError.Unknown(
                    operation =
                        DatabaseError.Operation.DELETE,
                    cause = e
                )
            )
        }
    }
}