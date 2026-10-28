
package com.rncoding.testvineshield.core.data.local.database.seed

import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase

class DiseaseCatalogueSeeder(
    private val database: VineshieldDatabase
) {

    suspend fun seed() {
        val catalogue = PredefinedDiseaseCatalogue.diseases

        require(catalogue.all { it.isPredefined }) {
            "Every seeded disease must be predefined."
        }

        require(catalogue.all { !it.code.isNullOrBlank() }) {
            "Every predefined disease must have a stable code."
        }

        require(
            catalogue.map { it.code }.distinct().size ==
                    catalogue.size
        ) {
            "Predefined disease codes must be unique."
        }

        database.diseaseDao().seedPredefinedDiseases(catalogue)
    }
}
