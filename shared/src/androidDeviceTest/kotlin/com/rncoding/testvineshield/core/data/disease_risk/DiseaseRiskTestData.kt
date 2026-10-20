
package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.entities.BlockEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity
import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import kotlinx.datetime.LocalDate

internal data class DiseaseRiskTestIds(
    val userId: Long,
    val vineyardId: Long,
    val blockId: Long,
    val diseaseId: Long
)

internal suspend fun VineshieldDatabase.insertDiseaseRiskTestData():
        DiseaseRiskTestIds {

    val date = LocalDate(2026, 10, 20)

    val userId = userDao().insertUser(
        UserEntity(
            email = "integration-test@example.com",
            passwordHash = "test-hash",
            passwordSalt = "test-salt"
        )
    )

    val vineyardId = vineyardDao().insertVineyard(
        VineyardEntity(
            userId = userId,
            name = "Integration Test Vineyard",
            size = 1.0,
            country = "Bulgaria",
            city = "Plovdiv",
            latitude = 42.14,
            longitude = 24.75,
            timeZone = "Europe/Sofia",
            elevation = 160,
            sortOrder = 0,
            createdAt = date,
            updatedAt = date
        )
    )

    val blockId = 101L

    blockDao().upsertBlock(
        BlockEntity(
            blockId = blockId,
            vineyardId = vineyardId,
            name = "Test Block",
            area = 1,
            vineVariety = "Merlot",
            color = "Red",
            rootStock = "SO4",
            rows = "10",
            rowSpacing = 2,
            plantedAt = date,
            phenologicalStage = null
        )
    )

    val diseaseId = 201L

    diseaseDao().upsertDisease(
        DiseaseEntity(
            diseaseId = diseaseId,
            code = "POWDERY_MILDEW",
            name = "Powdery mildew",
            description = "Test disease",
            vectors = "",
            factors = "",
            treatmentSuggestion = ""
        )
    )

    return DiseaseRiskTestIds(
        userId = userId,
        vineyardId = vineyardId,
        blockId = blockId,
        diseaseId = diseaseId
    )
}
