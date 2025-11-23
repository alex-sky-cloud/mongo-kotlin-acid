package com.mongo.mongokotlin.acid.domain.service.subscriptionsync

import com.mongo.mongokotlin.acid.domain.dto.SubscriptionDto
import com.mongo.mongokotlin.acid.domain.dto.subscriptionfetch.ExternalSubscriptionDto
import com.mongo.mongokotlin.acid.domain.mapper.ExternalSubscriptionMapper
import com.mongo.mongokotlin.acid.domain.model.SubscriptionEntity
import com.mongo.mongokotlin.acid.domain.repository.SubscriptionRepository
import com.mongo.mongokotlin.acid.domain.service.subscriptionfetch.ExternalSubscriptionClient
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

/**
 * Подход 2: Синхронизация через Flow с batch обработкой
 * 
 * Особенности:
 * - Использует Kotlin Flow для реактивной обработки данных
 * - Группирует подписки в батчи через chunked()
 * - Сохраняет батчами через saveAll (оптимизация запросов к БД)
 * - Эффективная работа с большими объемами данных
 * - Минимальное использование памяти благодаря Flow
 * - Реактивный стиль программирования
 */
@Service
class SubscriptionSyncService2(
    private val repository: SubscriptionRepository,
    private val externalClient: ExternalSubscriptionClient,
    private val mapper: ExternalSubscriptionMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    // Размер батча для batch операций
    private val batchSize = 100

    /**
     * Синхронизирует подписки клиента с внешним сервисом
     * Использует Flow с batch обработкой для оптимизации
     * 
     * @param customerId ID клиента (cus)
     * @return список синхронизированных подписок
     */
    suspend fun syncSubscriptions(customerId: String): List<SubscriptionDto> {
        log.info("🔄 [Подход 2] Начало синхронизации для клиента: {}", customerId)

        // Шаг 1: Получаем данные от внешнего сервиса
        val externalResponse = externalClient.fetchSubscriptions(customerId)
        val externalSubscriptions = externalResponse.subscriptions
        log.info("📥 Получено {} подписок от внешнего сервиса", externalSubscriptions.size)

        // Шаг 2: Загружаем существующие подписки через Flow
        // Создаем Map для быстрого поиска
        val existingMap = repository.findByCus(customerId)
            .map { it.publicId to it } // Преобразуем в пары (publicId, entity)
            .toList()
            .toMap()
        log.info("💾 Найдено {} существующих подписок в БД", existingMap.size)

        // Шаг 3: Обрабатываем подписки через Flow с batch сохранением
        // Сначала обрабатываем все подписки и собираем в список
        val allToSave = externalSubscriptions
            .asFlow() // Преобразуем List в Flow
            .map { externalDto ->
                // Для каждой внешней подписки определяем: обновить или создать
                val publicId = UUID.fromString(externalDto.subscriptionId)
                val existing = existingMap[publicId]

                if (existing != null) {
                    // Обновляем существующую
                    mapper.updateEntity(existing, externalDto)
                    log.debug("✏️ Подписка {} будет обновлена", publicId)
                    existing
                } else {
                    // Создаем новую
                    val newEntity = mapper.toEntity(externalDto, customerId)
                    log.debug("➕ Подписка {} будет создана", publicId)
                    newEntity
                }
            }
            .toList() // Собираем все обработанные подписки в список

        // Шаг 4: Сохраняем батчами через chunked() (метод для List)
        allToSave.chunked(batchSize).forEach { batch ->
            // Сохраняем каждый батч через saveAll (оптимизация запросов к БД)
            repository.saveAll(batch).collect()
            log.debug("💾 Сохранен батч из {} подписок", batch.size)
        }

        log.info("💾 Все подписки сохранены батчами")

        // Шаг 5: Загружаем обновленные данные для возврата
        val result = repository.findByCus(customerId).toList()
        log.info("✅ [Подход 2] Синхронизация завершена. Всего подписок: {}", result.size)

        // Преобразуем в DTO
        return result.map { entity ->
            SubscriptionDto(
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
    }
}

