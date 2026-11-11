package com.rncoding.testvineshield.core.domain.repository
import com.rncoding.testvineshield.core.domain.datamodels.VineyardMapPoint
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
interface LocationMetadataRepository {
    suspend fun suggestTimeZone(point: VineyardMapPoint): Result<String,AppError>
}
