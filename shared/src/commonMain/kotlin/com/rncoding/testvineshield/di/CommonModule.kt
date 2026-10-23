package com.rncoding.testvineshield.di

import com.rncoding.testvineshield.core.data.datasource.WeatherRemoteDataSource
import com.rncoding.testvineshield.core.data.disease_risk.DefaultDiseaseRiskHostContextProvider
import com.rncoding.testvineshield.core.data.disease_risk.DefaultDiseaseRiskPersistence
import com.rncoding.testvineshield.core.data.disease_risk.DefaultDiseaseRiskVineyardContextProvider
import com.rncoding.testvineshield.core.data.disease_risk.DiseaseRiskEngineImpl
import com.rncoding.testvineshield.core.data.disease_risk.DiseaseRiskInputFactoryImpl
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEngine
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskInputFactory
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
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherHistoryDao
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherForecastDao
import com.rncoding.testvineshield.core.data.local.mappers.BlockMapper
import com.rncoding.testvineshield.core.data.local.mappers.BlockSummaryMapper
import com.rncoding.testvineshield.core.data.local.mappers.DiseaseAlertMapper
import com.rncoding.testvineshield.core.data.local.mappers.DiseaseAlertSummaryMapper
import com.rncoding.testvineshield.core.data.local.mappers.DiseaseMapper
import com.rncoding.testvineshield.core.data.local.mappers.DiseaseOccurrenceMapper
import com.rncoding.testvineshield.core.data.local.mappers.DiseaseOccurrenceSummaryMapper
import com.rncoding.testvineshield.core.data.local.mappers.SecuritySettingsMapper
import com.rncoding.testvineshield.core.data.local.mappers.SymptomMapper
import com.rncoding.testvineshield.core.data.local.mappers.UserMapper
import com.rncoding.testvineshield.core.data.local.mappers.VineyardMapper
import com.rncoding.testvineshield.core.data.local.mappers.VineyardSummaryMapper
import com.rncoding.testvineshield.core.data.local.mappers.VineyardWeatherRiskSummaryMapper
import com.rncoding.testvineshield.core.data.local.mappers.VineyardWeatherSummaryMapper
import com.rncoding.testvineshield.core.data.repository.DiseaseRepositoryImpl
import com.rncoding.testvineshield.core.data.repository.SecuritySettingsRepositoryImpl
import com.rncoding.testvineshield.core.data.repository.SessionRepositoryImpl
import com.rncoding.testvineshield.core.data.repository.UserRepositoryImpl
import com.rncoding.testvineshield.core.data.repository.VineyardRepositoryImpl
import com.rncoding.testvineshield.core.data.time.SystemAppClock
import com.rncoding.testvineshield.core.domain.auth.AuthSessionCoordinator
import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.auth.ObserveAuthStateUseCase
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskHostContextProvider
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.security.PasswordHasher
import com.rncoding.testvineshield.core.domain.security.SessionManager
import com.rncoding.testvineshield.core.domain.validation.ValidateEmail
import com.rncoding.testvineshield.core.domain.validation.ValidatePassword
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.usecases.user.DeleteUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.LoginUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.LogoutUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RegisterUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RestoreSessionUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UnlockSessionUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UpdateUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.CreateVineyardUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.DeleteVineyardUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.GetVineyardUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.ObserveVineyardDetailsUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.ObserveVineyardSummariesUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.ObserveVineyardUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.ReorderVineyardsUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.UpdateVineyardUseCase
import com.rncoding.testvineshield.core.domain.usecases.weather.RefreshWeatherHistoryAndEvaluateRiskUseCase
import com.rncoding.testvineshield.core.domain.validation.VineyardValidator
import com.rncoding.testvineshield.core.presentation.account.AccountViewModel
import com.rncoding.testvineshield.core.presentation.security.SecuritySettingsViewModel
import com.rncoding.testvineshield.core.presentation.user_auth.AuthViewModel
import com.rncoding.testvineshield.core.presentation.vineyard_details.VineyardDetailsUiMapper
import com.rncoding.testvineshield.core.presentation.vineyard_list.VineyardListMapper
import com.rncoding.testvineshield.core.presentation.vineyard_list.VineyardListViewModel
import com.rncoding.testvineshield.core.presentation.vineyard_editor.VineyardEditorMode
import com.rncoding.testvineshield.core.presentation.vineyard_editor.VineyardEditorViewModel
import com.rncoding.testvineshield.core.presentation.vineyard_details.VineyardDetailsViewModel
import com.rncoding.testvineshield.core.data.disease_risk.DefaultDiseaseRiskWeatherProvider
import com.rncoding.testvineshield.core.data.disease_risk.DiseaseRiskMonitoringConfigurationRepositoryImpl
import com.rncoding.testvineshield.core.data.disease_risk.DiseaseRiskStateCodec
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEvaluationCoordinator
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskPersistence
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherProvider
import com.rncoding.testvineshield.core.domain.disease_risk.wetness.DiseaseRiskWeatherEnricher
import com.rncoding.testvineshield.core.domain.disease_risk.wetness.LeafWetnessEstimator
import com.rncoding.testvineshield.core.domain.disease_risk.wetness.WeatherBasedLeafWetnessEstimator
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelRegistry
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskMonitoringConfigurationRepository
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskVineyardContextProvider
import com.rncoding.testvineshield.core.domain.disease_risk.powdery.PowderyMildewAscosporeDiseaseRiskModel
import com.rncoding.testvineshield.core.domain.disease_risk.powdery.PowderyMildewUcDavisDiseaseRiskModel
import com.rncoding.testvineshield.core.domain.disease_risk.downy.DownyMildewPrimaryDiseaseRiskModel
import com.rncoding.testvineshield.core.domain.disease_risk.downy.DownyMildewSecondaryDiseaseRiskModel
import com.rncoding.testvineshield.core.domain.disease_risk.botrytis.BotrytisDiseaseRiskModel
import com.rncoding.testvineshield.core.data.disease_risk.DefaultDiseaseRiskWeatherWindowPolicy
import com.rncoding.testvineshield.core.data.disease_risk.VineyardDiseaseRiskEvaluatorImpl
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherWindowPolicy
import com.rncoding.testvineshield.core.data.local.mappers.WeatherForecastMapper
import com.rncoding.testvineshield.core.data.remote.WeatherRemoteDataSourceImpl
import com.rncoding.testvineshield.core.data.repository.WeatherForecastRepositoryImpl
import com.rncoding.testvineshield.core.data.repository.WeatherHistoryRepositoryImpl
import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluator
import com.rncoding.testvineshield.core.domain.repositories.WeatherForecastRepository
import com.rncoding.testvineshield.core.domain.repository.WeatherHistoryRepository
import com.rncoding.testvineshield.core.data.repository.VineyardWeatherLocationRepositoryImpl
import com.rncoding.testvineshield.core.domain.repository.VineyardWeatherLocationRepository
import com.rncoding.testvineshield.core.domain.usecases.weather.AuthorizeWeatherHistoryBackgroundWorkUseCase
import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryBackgroundWorkCoordinator
import com.rncoding.testvineshield.core.data.scheduler.BackgroundWorkVineyardSourceImpl
import com.rncoding.testvineshield.core.domain.scheduler.BackgroundWorkVineyardSource
import com.rncoding.testvineshield.core.data.repository.ActivityRepositoryImpl
import com.rncoding.testvineshield.core.domain.repository.ActivityRepository
import com.rncoding.testvineshield.core.domain.usecases.activity.CreateActivityUseCase
import com.rncoding.testvineshield.core.domain.usecases.activity.DeleteActivityUseCase
import com.rncoding.testvineshield.core.domain.usecases.activity.GetActivityDetailsUseCase
import com.rncoding.testvineshield.core.domain.usecases.activity.UpdateActivityUseCase
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

        single {
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        }

        single<DiseaseRiskWeatherWindowPolicy> {
            DefaultDiseaseRiskWeatherWindowPolicy()
        }

        /*
         * Datasources
         */

        single<WeatherRemoteDataSource> {
            WeatherRemoteDataSourceImpl(get())
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

        single<WeatherHistoryDao> {
            get<VineshieldDatabase>().weatherHistoryDao()
        }

        single<WeatherForecastDao> {
            get<VineshieldDatabase>().weatherForecastDao()
        }

        single {
            get<VineshieldDatabase>().diseaseRiskDao()
        }

        single<DiseaseRiskPersistence> {
            DefaultDiseaseRiskPersistence(
                diseaseRiskDao = get(),
                stateCodec = get()
            )
        }

        single {
            get<VineshieldDatabase>()
                .diseaseRiskMonitoringConfigurationDao()
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

        single {
            VineyardMapper()
        }

        single {
            VineyardSummaryMapper()
        }

        single {
            VineyardListMapper()
        }

        single {
            BlockSummaryMapper()
        }

        single {
            DiseaseOccurrenceSummaryMapper()
        }

        single {
            DiseaseAlertSummaryMapper()
        }

        single {
            VineyardWeatherSummaryMapper()
        }

        single {
            VineyardWeatherRiskSummaryMapper()
        }

        single { DiseaseMapper() }
        single { SymptomMapper() }
        single { DiseaseOccurrenceMapper() }
        single { DiseaseAlertMapper() }
        single { WeatherForecastMapper() }


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

        single<VineyardRepository> {
            VineyardRepositoryImpl(
                vineyardDao = get(),
                blockDao = get(),
                weatherHistoryDao = get(),
                weatherCalculationsDao = get(),
                diseaseOccurrenceDao = get(),
                diseaseAlertDao = get(),
                vineyardMapper = get(),
                vineyardSummaryMapper = get(),
                blockSummaryMapper = get(),
                vineyardWeatherSummaryMapper = get(),
                vineyardWeatherRiskSummaryMapper = get(),
                diseaseOccurrenceSummaryMapper = get(),
                diseaseAlertSummaryMapper = get()
            )
        }

        single<ActivityRepository> {
            ActivityRepositoryImpl(
                activityDao = get(),
                blockDao = get()
            )
        }

        single<DiseaseRepository> {
            DiseaseRepositoryImpl(
                diseaseDao = get(),
                diseaseOccurrenceDao = get(),
                symptomDao = get(),
                diseaseAlertDao = get(),
                diseaseMapper = get(),
                diseaseOccurrenceMapper = get(),
                symptomMapper = get(),
                diseaseAlertMapper = get()
            )
        }

        single<DiseaseRiskMonitoringConfigurationRepository> {
            DiseaseRiskMonitoringConfigurationRepositoryImpl(
                dao = get(),
                coordinator = get()
            )
        }
        single<WeatherForecastRepository> {
            WeatherForecastRepositoryImpl(
                remoteDataSource = get(),
                vineyardDao = get(),
                forecastDao = get(),
                forecastMapper = get()
            )
        }

        single<WeatherHistoryRepository> {
            WeatherHistoryRepositoryImpl(
                remoteDataSource = get(),
                vineyardDao = get(),
                weatherHistoryDao = get(),
                weatherHistoryMapper = get()
            )
        }


        single<VineyardWeatherLocationRepository> {
            VineyardWeatherLocationRepositoryImpl(
                vineyardDao = get()
            )
        }


        /*
         * Disease risk registry
         */

        single {
            DiseaseRiskModelRegistry(
                models = listOf(
                    PowderyMildewAscosporeDiseaseRiskModel(),
                    PowderyMildewUcDavisDiseaseRiskModel(),
                    DownyMildewPrimaryDiseaseRiskModel(),
                    DownyMildewSecondaryDiseaseRiskModel(),
                    BotrytisDiseaseRiskModel()
                )
            )
        }

        /*
         * Disease-risk orchestration
         */

        single<DiseaseRiskInputFactory> {
            DiseaseRiskInputFactoryImpl(
                monitoringRepository = get(),
                vineyardContextProvider = get(),
                weatherWindowPolicy = get(),
                weatherProvider = get(),
                hostContextProvider = get()
            )
        }

        single<DiseaseRiskEngine> {
            DiseaseRiskEngineImpl(
                modelRegistry = get(),
                inputFactory = get(),
                persistence = get(),
                coordinator = get()
            )
        }


        single<VineyardDiseaseRiskEvaluator> {
            VineyardDiseaseRiskEvaluatorImpl(
                monitoringRepository = get(),
                diseaseRiskEngine = get()
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

        single {
            VineyardDetailsUiMapper()
        }

        single { BlockMapper() }

        /*
         * Providers
         */

        single {
            AuthenticatedUserProvider(
                authSessionCoordinator = get()
            )
        }

        single<DiseaseRiskHostContextProvider> {
            DefaultDiseaseRiskHostContextProvider(
                blockDao = get(),
                diseaseOccurrenceDao = get()
            )
        }

        single<LeafWetnessEstimator> {
            WeatherBasedLeafWetnessEstimator()
        }

        single {
            DiseaseRiskWeatherEnricher(
                leafWetnessEstimator = get()
            )
        }

        single<DiseaseRiskWeatherProvider> {
            DefaultDiseaseRiskWeatherProvider(
                weatherHistoryDao = get(),
                weatherEnricher = get()
            )
        }

        single<DiseaseRiskVineyardContextProvider> {
            DefaultDiseaseRiskVineyardContextProvider(
                vineyardDao = get()
            )
        }

        /*
         * Codec
         */

        single {
            DiseaseRiskStateCodec()
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

        single {
            VineyardValidator()
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

        factory {
            ObserveAuthStateUseCase(
                authSessionCoordinator = get()
            )
        }


        factory {
            AuthorizeWeatherHistoryBackgroundWorkUseCase(
                sessionManager = get(),
                locationRepository = get()
            )
        }



        /*
         * Coordinators
         */

        single {
            AuthSessionCoordinator(
                restoreSessionUseCase = get(),
                unlockSessionUseCase = get(),
                loginUserUseCase = get(),
                registerUserUseCase = get(),
                logoutUserUseCase = get(),
                updateUserUseCase = get(),
                deleteUserUseCase = get(),
                backgroundWorkCoordinator = get(),
                weatherHistoryRiskScheduler = get()
            )
        }

        single {
            DiseaseRiskEvaluationCoordinator()
        }

        single {
            WeatherHistoryBackgroundWorkCoordinator(
                vineyardSource = get(),
                scheduler = get()
            )
        }

        single<BackgroundWorkVineyardSource> {
            BackgroundWorkVineyardSourceImpl(
                vineyardRepository = get()
            )
        }


         /*
         * Vineyard use cases
         */
        factory {
            ObserveVineyardSummariesUseCase(
                repository = get(),
                authenticatedUserProvider = get()
            )
        }

        factory {
            GetVineyardUseCase(
                repository = get(),
                authenticatedUserProvider = get()
            )
        }

        factory {
            DeleteVineyardUseCase(
                repository = get(),
                authenticatedUserProvider = get(),
                weatherHistoryRiskScheduler = get()
            )
        }

        factory {
            ReorderVineyardsUseCase(
                repository = get(),
                authenticatedUserProvider = get()
            )
        }

        factory {
            CreateVineyardUseCase(
                repository = get(),
                authenticatedUserProvider = get(),
                validator = get(),
                clock = get(),
                weatherHistoryRiskScheduler = get()
            )
        }

        factory {
            UpdateVineyardUseCase(
                repository = get(),
                authenticatedUserProvider = get(),
                validator = get(),
                clock = get()
            )
        }

        factory {
            ObserveVineyardUseCase(
                repository = get(),
                authenticatedUserProvider = get()
            )
        }

        factory {
            ObserveVineyardDetailsUseCase(
                repository = get(),
                authenticatedUserProvider = get()
            )
        }

        /*
         * Activity use cases
         */
        factory {
            CreateActivityUseCase(
                activityRepository = get(),
                vineyardRepository = get(),
                authenticatedUserProvider = get()
            )
        }

        factory {
            GetActivityDetailsUseCase(
                activityRepository = get(),
                vineyardRepository = get(),
                authenticatedUserProvider = get()
            )
        }

        factory {
            UpdateActivityUseCase(
                activityRepository = get(),
                vineyardRepository = get(),
                authenticatedUserProvider = get()
            )
        }

        factory {
            DeleteActivityUseCase(
                activityRepository = get(),
                vineyardRepository = get(),
                authenticatedUserProvider = get()
            )
        }


        /*
         * Weather use cases
         */


        factory {
            RefreshWeatherHistoryAndEvaluateRiskUseCase(
                weatherHistoryRepository = get(),
                vineyardDiseaseRiskEvaluator = get()
            )
        }


        /*
         * View models
         */

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

        viewModel {
            VineyardListViewModel(
                observeVineyardSummariesUseCase = get(),
                deleteVineyardUseCase = get(),
                reorderVineyardsUseCase = get(),
                mapper = get()
            )
        }

        viewModel { (mode: VineyardEditorMode) ->
            VineyardEditorViewModel(
                mode = mode,
                createVineyardUseCase = get(),
                updateVineyardUseCase = get(),
                getVineyardUseCase = get()
            )
        }

        viewModel { (vineyardId: Long) ->
            VineyardDetailsViewModel(
                vineyardId = vineyardId,
                observeVineyardDetailsUseCase = get(),
                deleteVineyardUseCase = get(),
                vineyardDetailsUiMapper = get()
            )
        }
    }