package com.rncoding.testvineshield.core.data.local.mappers


import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel


class VineyardMapper {
    fun domainToEntity(domainVineyard: VineyardDomainModel): VineyardEntity {
        return VineyardEntity(
            vineyardId = domainVineyard.vineyardId,
            userId = domainVineyard.userId,
            name = domainVineyard.name,
            size = domainVineyard.size,
            country = domainVineyard.country,
            city = domainVineyard.city,
            latitude = domainVineyard.latitude,
            longitude = domainVineyard.longitude,
            timeZone = domainVineyard.timeZone,
            elevation = domainVineyard.elevation,
            sortOrder = domainVineyard.sortOrder,
            createdAt = domainVineyard.createdAt,
            updatedAt = domainVineyard.updatedAt
        )
    }
    fun entityToDomain(entityVineyard: VineyardEntity): VineyardDomainModel {
        return VineyardDomainModel(
            vineyardId = entityVineyard.vineyardId,
            userId = entityVineyard.userId,
            name = entityVineyard.name,
            size = entityVineyard.size,
            country = entityVineyard.country,
            city = entityVineyard.city,
            latitude = entityVineyard.latitude,
            longitude = entityVineyard.longitude,
            timeZone = entityVineyard.timeZone,
            elevation = entityVineyard.elevation,
            sortOrder = entityVineyard.sortOrder,
            createdAt = entityVineyard.createdAt,
            updatedAt = entityVineyard.updatedAt
        )
    }

}