package com.mongo.mongokotlin.acid.domain.mapper

import com.mongo.mongokotlin.acid.domain.dto.SubscriptionDto
import com.mongo.mongokotlin.acid.domain.dto.SubscriptionVendorDto
import com.mongo.mongokotlin.acid.domain.model.SubscriptionEntity
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class SubscriptionMapper {

    /**
     * Преобразует Entity в DTO
     */
    fun toDto(entity: SubscriptionEntity): SubscriptionDto {
        return SubscriptionDto(
            id = entity.id?.toHexString(),
            publicId = entity.publicId,
            cus = entity.cus,
            offerId = entity.offerId,
            status = entity.status,
            balance = entity.balance,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            vendorStatus = entity.vendorStatus,
            vendorBalance = entity.vendorBalance,
            lastSyncTime = entity.lastSyncTime,
            usageCount = entity.usageCount,
            urlLogo = entity.urlLogo,
            brand = entity.brand
        )
    }

    /**
     * Обновляет Entity данными от вендора (мутирует entity)
     */
    fun updateEntityWithVendorData(entity: SubscriptionEntity, vendorDto: SubscriptionVendorDto) {
        entity.vendorStatus = vendorDto.vendorStatus
        entity.vendorBalance = vendorDto.vendorBalance
        entity.lastSyncTime = vendorDto.lastSyncTime
        entity.usageCount = vendorDto.usageCount
        entity.urlLogo = vendorDto.urlLogo
        entity.brand = vendorDto.brand
        entity.updatedAt = LocalDateTime.now()
    }

    /**
     * Создает DTO с объединенными данными из Entity и VendorDto
     */
    fun toDtoWithVendorData(entity: SubscriptionEntity, vendorDto: SubscriptionVendorDto?): SubscriptionDto {
        return SubscriptionDto(
            id = entity.id?.toHexString(),
            publicId = entity.publicId,
            cus = entity.cus,
            offerId = entity.offerId,
            status = entity.status,
            balance = entity.balance,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            vendorStatus = vendorDto?.vendorStatus ?: entity.vendorStatus,
            vendorBalance = vendorDto?.vendorBalance ?: entity.vendorBalance,
            lastSyncTime = vendorDto?.lastSyncTime ?: entity.lastSyncTime,
            usageCount = vendorDto?.usageCount ?: entity.usageCount,
            urlLogo = vendorDto?.urlLogo ?: entity.urlLogo,
            brand = vendorDto?.brand ?: entity.brand
        )
    }
}


