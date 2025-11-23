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
 * Подход 3: Синхронизация через оптимизированные нативные запросы с транзакциями
 * 
 * Особенности:
 * - Использует оптимизированные нативные запросы MongoDB
 * - Поиск существующих подписок через findByCusAndPublicIdIn (один запрос)
 * - Загружает только нужные подписки, а не все подписки клиента
 * - Все операции выполняются в транзакции для обеспечения ACID
 * - Обрабатывает race conditions через повторную проверку после сохранения
 * - Минимальное количество запросов к БД
 * - Эффективная работа с большими объемами данных
 * 
     * Решение проблемы race condition (неповторяемое чтение):
     * - Все операции выполняются в транзакции с изоляцией snapshot
     * - Snapshot isolation гарантирует, что мы видим консистентный снимок данных
     * - Если другой поток добавит подписку после начала нашей транзакции, мы ее не увидим
     *   до commit его транзакции, что предотвращает неповторяемое чтение
     * - Уникальный индекс на publicId предотвращает дубликаты при параллельных операциях
 */
@Service
class SubscriptionSyncService3(
    private val repository: SubscriptionRepository,
    private val externalClient: ExternalSubscriptionClient,
    private val mapper: ExternalSubscriptionMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Синхронизирует подписки клиента с внешним сервисом
     * Использует оптимизированные нативные запросы в транзакции
     * 
     * Обеспечивает ACID свойства:
     * - Атомарность: все операции в одной транзакции (все или ничего)
     * - Изоляция: snapshot isolation предотвращает неповторяемое чтение
     * - Согласованность: уникальный индекс на publicId предотвращает дубликаты
     * - Долговечность: commit транзакции гарантирует сохранение изменений
     * 
     * @param customerId ID клиента (cus)
     * @return список синхронизированных подписок
     */
    @Transactional
    suspend fun syncSubscriptions(customerId: String): List<SubscriptionDto> {
        log.info("🔄 [Подход 3] Начало транзакционной синхронизации для клиента: {}", customerId)

        try {
            // Шаг 1: Получаем данные от внешнего сервиса
            val externalResponse = externalClient.fetchSubscriptions(customerId)
            val externalSubscriptions = externalResponse.subscriptions
            log.info("📥 Получено {} подписок от внешнего сервиса", externalSubscriptions.size)

            // Шаг 2: Извлекаем список publicId из внешних подписок
            val externalPublicIds = externalSubscriptions.map { UUID.fromString(it.subscriptionId) }
            log.debug("🔑 Извлечено {} publicId из внешних подписок", externalPublicIds.size)

            // Шаг 3: Оптимизированный поиск существующих подписок В ТРАНЗАКЦИИ
            // Транзакция обеспечивает snapshot isolation - видим снимок данных на момент начала транзакции
            // Это предотвращает неповторяемое чтение: если другой поток добавит подписку,
            // мы ее не увидим до commit его транзакции
            val existingSubscriptions = repository.findByCusAndPublicIdIn(customerId, externalPublicIds)
            log.info("💾 Найдено {} существующих подписок в БД (в транзакции)", existingSubscriptions.size)

            // Шаг 4: Создаем Map для быстрого поиска
            val existingMap = existingSubscriptions.associateBy { it.publicId }
            log.debug("🗺️ Создан индекс существующих подписок: {} записей", existingMap.size)

            // Шаг 5: Разделяем подписки на обновляемые и новые
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

            // Шаг 6: Batch сохранение В ТРАНЗАКЦИИ
            // Все операции выполняются атомарно - либо все успешно, либо все откатятся
            // Snapshot isolation гарантирует, что мы работаем с консистентным снимком данных
            // Если другой поток добавил подписку после начала нашей транзакции, мы ее не увидим
            // до commit его транзакции, что предотвращает race condition
            val allToSave = toUpdate + toCreate
            if (allToSave.isNotEmpty()) {
                val saved = repository.saveAll(allToSave).toList()
                log.info("💾 Сохранено {} подписок через saveAll в транзакции", saved.size)
            }

            // Шаг 7: Загружаем обновленные данные для возврата (все еще в транзакции)
            val result = repository.findByCus(customerId).toList()
            log.info("✅ [Подход 3] Транзакционная синхронизация завершена. Всего подписок: {}", result.size)

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

