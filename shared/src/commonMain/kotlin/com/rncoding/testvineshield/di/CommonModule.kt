package com.rncoding.testvineshield.di

import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.daos.ActivityDao
import com.rncoding.testvineshield.core.data.local.database.daos.BlockDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseAlertDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseOccurrenceDao
import com.rncoding.testvineshield.core.data.local.database.daos.HarvestDao
import com.rncoding.testvineshield.core.data.local.database.daos.OccurrenceSymptomCrossRefDao
import com.rncoding.testvineshield.core.data.local.database.daos.SecuritySettingsDao
import com.rncoding.testvineshield.core.data.local.database.daos.SymptomDao
import com.rncoding.testvineshield.core.data.local.database.daos.UserDao
import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherCalculationsDao
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherDao
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherForecastDao
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
import com.rncoding.testvineshield.core.domain.usecases.user.DeleteUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.LoginUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.LogoutUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RegisterUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RestoreSessionUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UnlockSessionUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UpdateUserUseCase
import com.rncoding.testvineshield.core.presentation.account.AccountViewModel
import com.rncoding.testvineshield.core.presentation.security.SecuritySettingsViewModel
import com.rncoding.testvineshield.core.presentation.user_auth.AuthViewModel
import kotlinx.serialization.json.Json
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

        single<ActivityDao> {
            get<VineshieldDatabase>().activityDao()
        }

        single<BlockDao> {
            get<VineshieldDatabase>().blockDao()
        }

        single<DiseaseAlertDao> {
            get<VineshieldDatabase>().diseaseAlertDao()
        }

        single<DiseaseDao> {
            get<VineshieldDatabase>().diseaseDao()
        }

        single<DiseaseOccurrenceDao> {
            get<VineshieldDatabase>().diseaseOccurrenceDao()
        }

        single<HarvestDao> {
            get<VineshieldDatabase>().harvestDao()
        }

        single<OccurrenceSymptomCrossRefDao> {
            get<VineshieldDatabase>().occurrenceSymptomCrossRefDao()
        }

        single<SecuritySettingsDao> {
            get<VineshieldDatabase>().securitySettingsDao()
        }

        single<SymptomDao> {
            get<VineshieldDatabase>().symptomDao()
        }

        single<UserDao> {
            get<VineshieldDatabase>()
                .userDao()
        }

        single<VineyardDao> {
            get<VineshieldDatabase>().vineyardDao()
        }

        single<WeatherCalculationsDao> {
            get<VineshieldDatabase>().weatherCalculationsDao()
        }

        single<WeatherDao> {
            get<VineshieldDatabase>().weatherDao()
        }

        single<WeatherForecastDao> {
            get<VineshieldDatabase>().weatherForecastDao()
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
                database = get(),
                userDao = get(),
                userMapper = get(),
                hasher = get<PasswordHasher>()
            )
        }

        single {
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
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
            UpdateUserUseCase(
                userRepository = get(),
                validateEmail = get(),
                validatePassword = get()
            )
        }

        factory {
            DeleteUserUseCase(
                userRepository = get(),
                sessionManager = get()
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
                logoutUserUseCase = get(),
                updateUserUseCase = get(),
                deleteUserUseCase = get()
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

        viewModel {
            AccountViewModel(
                authSessionCoordinator = get(),
                observeAuthStateUseCase = get()
            )
        }

        viewModel { (userId: Long) ->
            SecuritySettingsViewModel(
                userId = userId,
                repository = get(),
                biometricAuthenticator = get()
            )
        }

    }