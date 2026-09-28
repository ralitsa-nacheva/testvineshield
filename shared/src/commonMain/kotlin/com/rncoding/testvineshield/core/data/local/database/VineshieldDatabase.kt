package com.rncoding.testvineshield.core.data.local.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherDao
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
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherCalculationEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherForecastEntity

@Database(entities = [ActivityEntity::class, BlockEntity::class, DiseaseAlertEntity::class,
    DiseaseEntity::class, DiseaseOccurrenceEntity::class, DiseaseSymptomCrossRef::class, HarvestEntity::class,
    ObservedSymptomEntity::class, OccurrenceActivityCrossRef::class, OccurrenceSymptomCrossRef::class, SecuritySettingsEntity::class,
    SymptomEntity::class, UserEntity::class, VineyardEntity::class, WeatherCalculationEntity::class,
    WeatherEntity::class, WeatherForecastEntity::class],
    version = 1)
@TypeConverters(LocalDateRoomConverters::class)
@ConstructedBy(VineshieldConstructor::class)

abstract class VineshieldDatabase: RoomDatabase() {
    companion object {
        const val DB_NAME = "vine_shield.db"
    }
    abstract fun vineyardDao(): VineyardDao
    abstract fun activityDao(): ActivityDao
    abstract fun blockDao(): BlockDao
    abstract fun diseaseAlertDao(): DiseaseAlertDao
    abstract fun diseaseDao(): DiseaseDao
    abstract fun diseaseOccurrenceDao(): DiseaseOccurrenceDao
    abstract fun harvestDao(): HarvestDao
    abstract fun occurrenceSymptomCrossRefDao(): OccurrenceSymptomCrossRefDao
    abstract fun symptomDao(): SymptomDao
    abstract fun userDao(): UserDao
    abstract fun securitySettingsDao(): SecuritySettingsDao
    abstract fun weatherCalculationsDao(): WeatherCalculationsDao

    abstract fun weatherDao(): WeatherDao

    abstract fun weatherForecastDao(): WeatherForecastDao

}