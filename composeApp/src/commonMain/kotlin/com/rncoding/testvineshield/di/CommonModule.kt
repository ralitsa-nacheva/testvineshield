package com.rncoding.testvineshield.di

import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.daos.SecuritySettingsDao
import com.rncoding.testvineshield.core.data.local.database.daos.UserDao
import com.rncoding.testvineshield.core.data.local.mappers.SecuritySettingsMapper
import com.rncoding.testvineshield.core.data.local.mappers.UserMapper
import com.rncoding.testvineshield.core.data.repository.SecuritySettingsRepositoryImpl
import com.rncoding.testvineshield.core.data.repository.SessionRepositoryImpl
import com.rncoding.testvineshield.core.data.repository.UserRepositoryImpl
import com.rncoding.testvineshield.core.data.time.SystemAppClock
import com.rncoding.testvineshield.core.domain.auth.AuthSessionCoordinator
import com.rncoding.testvineshield.core.domain.auth.ObserveAuthStateUseCase
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.security.PasswordHasher
import com.rncoding.testvineshield.core.domain.security.SessionManager
import com.rncoding.testvineshield.core.domain.security.ValidateEmail
import com.rncoding.testvineshield.core.domain.security.ValidatePassword
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.usecases.user.LoginUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.LogoutUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RegisterUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RestoreSessionUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UnlockSessionUseCase
import com.rncoding.testvineshield.core.presentation.user_auth.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule =
    module {

        /*
         * Core infrastructure
         */

        single<AppClock> {
            SystemAppClock()
        }


        /*
         * Room DAOs
         *
         * VineyardDatabase itself is supplied
         * by the platform-specific module.
         */

        single<UserDao> {
            get<VineshieldDatabase>()
                .userDao()
        }

        single<SecuritySettingsDao> {
            get<VineshieldDatabase>()
                .securitySettingsDao()
        }


        /*
         * Mappers
         */

        single {
            UserMapper()
        }

        single {
            SecuritySettingsMapper()
        }


        /*
         * Repositories
         */

        single<UserRepository> {
            UserRepositoryImpl(
                userDao = get(),
                userMapper = get(),
                hasher = get<PasswordHasher>()
            )
        }

        single<SessionRepository> {
            SessionRepositoryImpl(
                secureStorage = get(),
                json = get()
            )
        }

        single<SecuritySettingsRepository> {
            SecuritySettingsRepositoryImpl(
                dao = get(),
                mapper = get()
            )
        }


        /*
         * Session manager
         */

        single {
            SessionManager(
                sessionRepository = get(),
                clock = get()
            )
        }


        /*
         * Validators
         */

        factory {
            ValidateEmail()
        }

        factory {
            ValidatePassword()
        }


        /*
         * Auth use cases
         */

        factory {
            LoginUserUseCase(
                userRepository = get(),
                sessionManager = get(),
                validateEmail = get(),
                validatePassword = get()
            )
        }

        factory {
            RegisterUserUseCase(
                userRepository = get(),
                validateEmail = get(),
                validatePassword = get()
            )
        }

        factory {
            RestoreSessionUseCase(
                sessionManager = get(),
                userRepository = get(),
                securitySettingsRepository = get()
            )
        }

        factory {
            UnlockSessionUseCase(
                sessionManager = get(),
                userRepository = get(),
                localAuthenticator = get()
            )
        }

        factory {
            LogoutUserUseCase(
                sessionManager = get()
            )
        }


        /*
         * Global auth coordinator
         */

        single {
            AuthSessionCoordinator(
                restoreSessionUseCase = get(),
                unlockSessionUseCase = get(),
                loginUserUseCase = get(),
                registerUserUseCase = get(),
                logoutUserUseCase = get()
            )
        }

        factory {
            ObserveAuthStateUseCase(
                authSessionCoordinator = get()
            )
        }

        viewModel {
            AuthViewModel(
                authSessionCoordinator = get(),
                observeAuthStateUseCase = get()
            )
        }
    }