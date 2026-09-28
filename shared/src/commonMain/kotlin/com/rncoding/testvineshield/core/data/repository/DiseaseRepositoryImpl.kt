package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.ActivityDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseOccurrenceDao
import com.rncoding.testvineshield.core.data.local.database.daos.OccurrenceSymptomCrossRefDao
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository

class DiseaseRepositoryImpl(private val diseaseDao: DiseaseDao,
                            private val occurrenceDao: DiseaseOccurrenceDao,
                            private val occurrenceSymptomCrossRefDao: OccurrenceSymptomCrossRefDao,
                            private val observedSymptomsDao: ObservedSymptomDao,
                            private val diseaseTreatmentDao: DiseaseTreatmentDao,
                            private val activityDao: ActivityDao
): DiseaseRepository {
    override suspend fun saveOccurrenceWithSymptoms(occurrence: ) {
        TODO("Not yet implemented")
    }
}