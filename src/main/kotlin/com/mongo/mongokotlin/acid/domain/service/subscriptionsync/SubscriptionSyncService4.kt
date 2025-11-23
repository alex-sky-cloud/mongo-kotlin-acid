package com.mongo.mongokotlin.acid.domain.service.subscriptionsync

import com.mongo.mongokotlin.acid.domain.dto.SubscriptionDto
import com.mongo.mongokotlin.acid.domain.dto.subscriptionfetch.ExternalSubscriptionDto
import com.mongo.mongokotlin.acid.domain.mapper.ExternalSubscriptionMapper
import com.mongo.mongokotlin.acid.domain.model.SubscriptionEntity
import com.mongo.mongokotlin.acid.domain.repository.SubscriptionRepository
import com.mongo.mongokotlin.acid.domain.service.subscriptionfetch.ExternalSubscriptionClient
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Подход 4: Синхронизация через транзакционную обработку
 * 
 * Особенности:
 * - Все операции выполняются в одной транзакции
 * - Гарантирует атомарность операций (все или ничего)
 * - Откат изменений при ошибке
 * - Подходит для критически важных операций
 * - Требует MongoDB replica set или sharded cluster
 * 
 * Примечание: MongoDB транзакции требуют replica set или sharded cluster.
 * В standalone режиме транзакции не поддерживаются.
 */
@Service
class SubscriptionSyncService4(
    private val repository: SubscriptionRepository,
    private val externalClient: ExternalSubscriptionClient,
    private val mapper: ExternalSubscriptionMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Синхронизирует подписки клиента с внешним сервисом
     * Все операции выполняются в одной транзакции
     * 
     * @param customerId ID клиента (cus)
     * @return список синхронизированных подписок
     */
    @Transactional
    suspend fun syncSubscriptions(customerId: String): List<SubscriptionDto> {
        log.info("🔄 [Подход 4] Начало транзакционной синхронизации для клиента: {}", customerId)

        try {
            // Шаг 1: Получаем данные от внешнего сервиса
            val externalResponse = externalClient.fetchSubscriptions(customerId)
            val externalSubscriptions = externalResponse.subscriptions
            log.info("📥 Получено {} подписок от внешнего сервиса", externalSubscriptions.size)

            // Шаг 2: Загружаем существующие подписки из MongoDB
            // В транзакции все операции выполняются атомарно
            val existingSubscriptions = repository.findByCus(customerId).toList()
            log.info("💾 Найдено {} существующих подписок в БД", existingSubscriptions.size)

            // Шаг 3: Создаем Map для быстрого поиска по publicId
            val existingMap = existingSubscriptions.associateBy { it.publicId }
            log.debug("🗺️ Создан индекс существующих подписок: {} записей", existingMap.size)

            // Шаг 4: Разделяем подписки на обновляемые и новые
            val toUpdate = mutableListOf<SubscriptionEntity>()
            val toCreate = mutableListOf<SubscriptionEntity>()

            externalSubscriptions.forEach { externalDto ->
                val publicId = UUID.fromString(externalDto.subscriptionId)
                val existing = existingMap[publicId]

                if (existing != null) {
                    // Подписка существует - обновляем
                    mapper.updateEntity(existing, externalDto)
                    toUpdate.add(existing)
                    log.debug("✏️ Подписка {} помечена для обновления", publicId)
                } else {
                    // Подписка не существует - создаем новую
                    val newEntity = mapper.toEntity(externalDto, customerId)
                    toCreate.add(newEntity)
                    log.debug("➕ Подписка {} помечена для создания", publicId)
                }
            }

            log.info("📊 Статистика: обновить={}, создать={}", toUpdate.size, toCreate.size)

            // Шаг 5: Batch сохранение в транзакции
            // Если произойдет ошибка - все изменения откатятся
            val allToSave = toUpdate + toCreate
            if (allToSave.isNotEmpty()) {
                val saved = repository.saveAll(allToSave).toList()
                log.info("💾 Сохранено {} подписок в транзакции", saved.size)
            } else {
                log.info("ℹ️ Нет изменений для сохранения")
            }

            // Шаг 6: Загружаем обновленные данные для возврата
            // Все еще в рамках транзакции
            val result = repository.findByCus(customerId).toList()
            log.info("✅ [Подход 4] Транзакционная синхронизация завершена. Всего подписок: {}", result.size)

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
        } catch (ex: Exception) {
            // При ошибке транзакция автоматически откатится
            log.error("❌ Ошибка в транзакции синхронизации для клиента: {}. Все изменения будут откачены.", customerId, ex)
            throw ex
        }
    }
}

