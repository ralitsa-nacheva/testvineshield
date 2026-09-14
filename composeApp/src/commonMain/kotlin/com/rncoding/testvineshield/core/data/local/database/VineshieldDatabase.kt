package com.rncoding.testvineshield.core.data.local.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.rncoding.testvineshield.core.data.local.database.daos.ActivityDao
import com.rncoding.testvineshield.core.data.local.database.daos.BlockDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseAlertDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseOccurrenceDao
import com.rncoding.testvineshield.core.data.local.database.daos.HarvestDao
import com.rncoding.testvineshield.core.data.local.database.daos.SecuritySettingsDao
import com.rncoding.testvineshield.core.data.local.database.daos.SymptomDao
import com.rncoding.testvineshield.core.data.local.database.daos.UserDao
import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherCalculationsDao
import com.rncoding.testvineshield.core.data.local.database.entities.ActivityEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseTreatmentEntity
import com.rncoding.testvineshield.core.data.local.database.entities.BlockEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseAlertEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseSymptomCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.HarvestEntity
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceTreatmentCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceSymptomCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.SymptomEntity
import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherCalculationEntity

@Database(entities = [ActivityEntity::class, DiseaseTreatmentEntity::class, BlockEntity::class, DiseaseEntity::class,
    DiseaseAlertEntity::class, DiseaseOccurrenceEntity::class, DiseaseSymptomCrossRef::class, HarvestEntity::class,
    OccurrenceTreatmentCrossRef::class, OccurrenceSymptomCrossRef::class, SymptomEntity::class,
    UserEntity::class, VineyardEntity::class, WeatherEntity::class, WeatherCalculationEntity::class],
    version = 1)
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
    abstract fun symptomDao(): SymptomDao
    abstract fun userDao(): UserDao
    abstract fun securitySettingsDao(): SecuritySettingsDao
    abstract fun weatherCalculationsDao(): WeatherCalculationsDao

}