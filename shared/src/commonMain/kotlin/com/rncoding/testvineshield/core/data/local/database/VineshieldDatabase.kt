package com.rncoding.testvineshield.core.data.local.database

import com.rncoding.testvineshield.core.data.local.database.converters.MeasurementUnitRoomConverters
import com.rncoding.testvineshield.core.data.local.database.daos.SprayDao
import com.rncoding.testvineshield.core.data.local.database.entities.SprayApplicationEntity
import com.rncoding.testvineshield.core.data.local.database.entities.SprayProductEntity
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rncoding.testvineshield.core.data.local.database.converters.DomainEnumRoomConverters
import com.rncoding.testvineshield.core.data.local.database.converters.LocalDateRoomConverters
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
import com.rncoding.testvineshield.core.data.local.database.entities.ActivityEntity
import com.rncoding.testvineshield.core.data.local.database.entities.BlockEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseAlertEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseSymptomCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.HarvestEntity
import com.rncoding.testvineshield.core.data.local.database.entities.ObservedSymptomEntity
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceActivityCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceSymptomCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.SecuritySettingsEntity
import com.rncoding.testvineshield.core.data.local.database.entities.SymptomEntity
import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherCalculationEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherForecastEntity
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseRiskDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseRiskMonitoringConfigurationDao
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseRiskEvaluationEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseRiskModelStateEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseRiskMonitoringConfigurationEntity

import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryArchiveEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherSourceEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistorySourceEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherCacheStatusEntity
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherCacheDao

import com.rncoding.testvineshield.core.data.local.database.entities.BlockStageObservationEntity

@Database(entities = [BlockStageObservationEntity::class, ActivityEntity::class, BlockEntity::class, DiseaseAlertEntity::class,
    DiseaseEntity::class, DiseaseOccurrenceEntity::class, DiseaseRiskMonitoringConfigurationEntity::class,
    DiseaseRiskEvaluationEntity::class, DiseaseRiskModelStateEntity::class, DiseaseSymptomCrossRef::class,
    HarvestEntity::class, ObservedSymptomEntity::class, OccurrenceActivityCrossRef::class,
    OccurrenceSymptomCrossRef::class, SecuritySettingsEntity::class, SymptomEntity::class, UserEntity::class,
    VineyardEntity::class, WeatherCalculationEntity::class, WeatherHistoryEntity::class, WeatherForecastEntity::class, SprayApplicationEntity::class, SprayProductEntity::class, WeatherSourceEntity::class, WeatherHistorySourceEntity::class, WeatherCacheStatusEntity::class, WeatherHistoryArchiveEntity::class],
    version = 12)
@TypeConverters(LocalDateRoomConverters::class, DomainEnumRoomConverters::class, MeasurementUnitRoomConverters::class)
@ConstructedBy(VineshieldConstructor::class)

abstract class VineshieldDatabase: RoomDatabase() {
    companion object {
        const val DB_NAME = "vine_shield.db"
    }
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun vineyardDao(): VineyardDao
    abstract fun activityDao(): ActivityDao
    abstract fun sprayDao(): SprayDao
    abstract fun blockDao(): BlockDao
    abstract fun diseaseAlertDao(): DiseaseAlertDao
    abstract fun diseaseDao(): DiseaseDao
    abstract fun diseaseOccurrenceDao(): DiseaseOccurrenceDao

    abstract fun diseaseRiskDao(): DiseaseRiskDao

    abstract fun diseaseRiskMonitoringConfigurationDao():
            DiseaseRiskMonitoringConfigurationDao
    abstract fun harvestDao(): HarvestDao
    abstract fun occurrenceSymptomCrossRefDao(): OccurrenceSymptomCrossRefDao
    abstract fun symptomDao(): SymptomDao
    abstract fun userDao(): UserDao
    abstract fun securitySettingsDao(): SecuritySettingsDao
    abstract fun weatherCalculationsDao(): WeatherCalculationsDao

    abstract fun weatherHistoryDao(): WeatherHistoryDao

    abstract fun weatherForecastDao(): WeatherForecastDao

}