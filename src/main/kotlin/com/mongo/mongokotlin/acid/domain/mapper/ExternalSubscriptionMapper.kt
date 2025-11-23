package com.mongo.mongokotlin.acid.domain.mapper

import com.mongo.mongokotlin.acid.domain.dto.subscriptionfetch.ExternalSubscriptionDto
import com.mongo.mongokotlin.acid.domain.model.SubscriptionEntity
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Маппер для преобразования ExternalSubscriptionDto в SubscriptionEntity
 * Используется при синхронизации данных от внешнего сервиса
 */
@Component
class ExternalSubscriptionMapper {

    /**
     * Преобразует ExternalSubscriptionDto в SubscriptionEntity для создания новой подписки
     * 
     * @param externalDto DTO от внешнего сервиса
     * @param customerId ID клиента (cus)
     * @return новая SubscriptionEntity
     */
    fun toEntity(
        externalDto: ExternalSubscriptionDto,
        customerId: String
    ): SubscriptionEntity {
        val now = LocalDateTime.now()
        
        return SubscriptionEntity(
            id = null, // Будет сгенерирован MongoDB
            publicId = UUID.fromString(externalDto.subscriptionId), // Преобразуем String в UUID
            cus = customerId,
            offerId = externalDto.offerId,
            status = externalDto.status,
            balance = BigDecimal.valueOf(externalDto.price),
            createdAt = externalDto.startDate,
            updatedAt = now,
            // Поля от вендора заполняем из внешнего DTO
            vendorStatus = externalDto.status,
            vendorBalance = BigDecimal.valueOf(externalDto.price),
            lastSyncTime = now,
            usageCount = null,
            urlLogo = null,
            brand = null
        )
    }

    /**
     * Обновляет существующую SubscriptionEntity данными от внешнего сервиса
     * 
     * @param entity существующая сущность для обновления
     * @param externalDto DTO от внешнего сервиса
     */
    fun updateEntity(
        entity: SubscriptionEntity,
        externalDto: ExternalSubscriptionDto
    ) {
        // Обновляем основные поля
        entity.status = externalDto.status
        entity.balance = BigDecimal.valueOf(externalDto.price)
        entity.updatedAt = LocalDateTime.now()
        
        // Обновляем поля от вендора
        entity.vendorStatus = externalDto.status
        entity.vendorBalance = BigDecimal.valueOf(externalDto.price)
        entity.lastSyncTime = LocalDateTime.now()
    }
}

