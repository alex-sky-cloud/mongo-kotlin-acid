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
 * Подход 5: Синхронизация с явным указанием readConcern=SNAPSHOT через transaction labels
 * 
 * Особенности:
 * - Использует оптимизированные нативные запросы MongoDB
 * - Поиск существующих подписок через findByCusAndPublicIdIn (один запрос)
 * - Загружает только нужные подписки, а не все подписки клиента
 * - Явное указание readConcern=SNAPSHOT через @Transactional label
 * - Все операции выполняются в транзакции с гарантированным snapshot isolation
 * - Минимальное количество запросов к БД
 * - Эффективная работа с большими объемами данных
 * 
 * Важно: Snapshot isolation работает БЕЗ явного указания label!
 * 
 * MongoDB транзакции по умолчанию имеют snapshot isolation на уровне транзакции,
 * независимо от указанного readConcern. Это означает, что даже без label все операции
 * внутри транзакции видят один консистентный снимок данных на момент начала транзакции.
 * 
 * Зачем тогда нужен label с readConcern=SNAPSHOT?
 * 
 * Label нужен для явного контроля УРОВНЯ snapshot:
 * - readConcern: "local" - snapshot на уровне транзакции, но данные могут быть не majority-committed
 * - readConcern: "snapshot" - snapshot из majority-committed данных, гарантия что данные не откатятся при failover
 * - readConcern: "majority" - тоже majority-committed, но без гарантии snapshot для multi-shard операций
 * 
 * Когда нужен label с readConcern=SNAPSHOT:
 * - Работа с sharded cluster и нужна консистентность между шардами
 * - Критично, чтобы данные были majority-committed и не откатились при failover
 * - Нужна явная гарантия, что читаются только majority-committed данные
 * 
 * Для replica set с single-shard операциями (как в нашем случае) дефолтного поведения достаточно,
 * но явное указание readConcern=SNAPSHOT делает код более явным и понятным.
 * 
 * Решение проблемы race condition (неповторяемое чтение):
 * - Транзакция обеспечивает snapshot isolation (работает и без label)
 * - Явное указание readConcern=SNAPSHOT гарантирует majority-committed snapshot
 * - Все операции find внутри транзакции видят один снимок данных на момент начала транзакции
 * - Другие потоки могут добавлять/изменять документы параллельно, но транзакция их не увидит
 * - При конфликте записи MongoDB выдаст WriteConflict и откатит транзакцию
 * - Уникальный индекс на publicId предотвращает дубликаты при параллельных операциях
 * 
 * Ссылки:
 * - https://docs.spring.io/spring-data/mongodb/reference/mongodb/client-session-transactions.html
 * - https://www.mongodb.com/docs/manual/reference/read-concern-snapshot/
 * - https://stackoverflow.com/questions/60156222/changing-mongodb-isolation-level-when-mongo-sessions-involved
 */
@Service
class SubscriptionSyncService5(
    private val repository: SubscriptionRepository,
    private val externalClient: ExternalSubscriptionClient,
    private val mapper: ExternalSubscriptionMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Синхронизирует подписки клиента с внешним сервисом
     * Использует оптимизированные нативные запросы в транзакции с явным readConcern=SNAPSHOT
     * 
     * Обеспечивает ACID свойства:
     * - Атомарность: все операции в одной транзакции (все или ничего)
     * - Изоляция: snapshot isolation (работает и без label, но label гарантирует majority-committed)
     * - Согласованность: уникальный индекс на publicId предотвращает дубликаты
     * - Долговечность: commit транзакции гарантирует сохранение изменений
     * 
     * @Transactional(label = ["mongo:readConcern=SNAPSHOT"]) - явное указание readConcern
     * 
     * Примечание: Snapshot isolation работает и БЕЗ label, так как MongoDB транзакции
     * по умолчанию имеют snapshot isolation на уровне транзакции. Однако явное указание
     * readConcern=SNAPSHOT гарантирует, что читаются только majority-committed данные,
     * что важно для защиты от отката данных при failover.
     * 
     * Spring Data MongoDB парсит метку "mongo:readConcern=SNAPSHOT" и применяет
     * ReadConcern.SNAPSHOT к TransactionOptions.
     * 
     * @param customerId ID клиента (cus)
     * @return список синхронизированных подписок
     */
    @Transactional(label = ["mongo:readConcern=SNAPSHOT"])
    suspend fun syncSubscriptions(customerId: String): List<SubscriptionDto> {
        log.info("🔄 [Подход 5] Начало транзакционной синхронизации с readConcern=SNAPSHOT для клиента: {}", customerId)

        try {
            // Шаг 1: Получаем данные от внешнего сервиса
            val externalResponse = externalClient.fetchSubscriptions(customerId)
            val externalSubscriptions = externalResponse.subscriptions
            log.info("📥 Получено {} подписок от внешнего сервиса", externalSubscriptions.size)

            // Шаг 2: Извлекаем список publicId из внешних подписок
            val externalPublicIds = externalSubscriptions.map { UUID.fromString(it.subscriptionId) }
            log.debug("🔑 Извлечено {} publicId из внешних подписок", externalPublicIds.size)

            // Шаг 3: Оптимизированный поиск существующих подписок В ТРАНЗАКЦИИ
            // readConcern=SNAPSHOT гарантирует, что все операции find видят один снимок данных
            // на момент начала транзакции. Это предотвращает неповторяемое чтение:
            // если другой поток добавит подписку после начала нашей транзакции, мы ее не увидим
            // до commit его транзакции
            val existingSubscriptions = repository.findByCusAndPublicIdIn(customerId, externalPublicIds)
            log.info("💾 Найдено {} существующих подписок в БД (readConcern=SNAPSHOT)", existingSubscriptions.size)

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

            // Шаг 6: Batch сохранение В ТРАНЗАКЦИИ с readConcern=SNAPSHOT
            // Все операции выполняются атомарно - либо все успешно, либо все откатятся
            // readConcern=SNAPSHOT гарантирует, что мы работаем с консистентным снимком данных
            // Если другой поток добавил подписку после начала нашей транзакции, мы ее не увидим
            // до commit его транзакции, что предотвращает race condition
            // При конфликте записи MongoDB выдаст WriteConflict и откатит транзакцию
            val allToSave = toUpdate + toCreate
            if (allToSave.isNotEmpty()) {
                val saved = repository.saveAll(allToSave).toList()
                log.info("💾 Сохранено {} подписок через saveAll в транзакции (readConcern=SNAPSHOT)", saved.size)
            }

            // Шаг 7: Загружаем обновленные данные для возврата (все еще в транзакции)
            // readConcern=SNAPSHOT гарантирует, что второй findByCus() вернет те же документы,
            // что видел первый findByCusAndPublicIdIn() - это устраняет неповторяемое чтение
            val result = repository.findByCus(customerId).toList()
            log.info("✅ [Подход 5] Транзакционная синхронизация с readConcern=SNAPSHOT завершена. Всего подписок: {}", result.size)

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

