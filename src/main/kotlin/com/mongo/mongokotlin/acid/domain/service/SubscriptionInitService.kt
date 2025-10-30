package com.mongo.mongokotlin.acid.domain.service

import com.mongo.mongokotlin.acid.domain.model.SubscriptionEntity
import com.mongo.mongokotlin.acid.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import net.datafaker.Faker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
class SubscriptionInitService(
    private val subscriptionRepository: SubscriptionRepository
) {
    
    private val log = LoggerFactory.getLogger(javaClass)
    private val faker = Faker()

    /**
     * Инициализирует подписки для указанного cus.
     * Не помечен как suspend, так как просто создает и возвращает Flow.
     * Suspend операции выполняются внутри flow builder при сборе.
     */
    fun initializeSubscriptions(cus: String, count: Int): Flow<SubscriptionEntity> {
        log.info("Инициализация {} подписок для cus: {}", count, cus)
        
        return kotlinx.coroutines.flow.flow {
            repeat(count) {
                val subscription = createFakeSubscription(cus)
                val saved = subscriptionRepository.save(subscription)
                emit(saved)
            }
        }
            .onEach { sub ->
                log.debug("Создана подписка: publicId={}, cus={}, offerId={}", 
                    sub.publicId, sub.cus, sub.offerId)
            }
            .onCompletion {
                log.info("Инициализация завершена для cus: {}", cus)
            }
    }

    /**
     * Удаляет все подписки для указанного cus
     */
    suspend fun deleteAllByCus(cus: String) {
        log.info("Удаление всех подписок для cus: {}", cus)
        subscriptionRepository.findByCus(cus)
            .collect { subscription ->
                subscriptionRepository.delete(subscription)
            }
        log.info("Все подписки удалены для cus: {}", cus)
    }

    /**
     * Создает фейковую подписку с использованием Faker
     */
    private fun createFakeSubscription(cus: String): SubscriptionEntity {
        val statuses = listOf("ACTIVE", "INACTIVE", "PENDING", "TRIAL")
        val offers = listOf("OFFER-BASIC", "OFFER-PREMIUM", "OFFER-ENTERPRISE", "OFFER-TRIAL")
        
        return SubscriptionEntity(
            publicId = UUID.randomUUID(),
            cus = cus,
            offerId = offers.random(),
            status = statuses.random(),
            balance = BigDecimal.valueOf(faker.number().randomDouble(2, 0, 5000)),
            createdAt = LocalDateTime.now().minusDays(faker.number().numberBetween(1, 365).toLong()),
            updatedAt = LocalDateTime.now(),
            vendorStatus = null,
            vendorBalance = null,
            lastSyncTime = null,
            usageCount = null,
            urlLogo = null,
            brand = null
        )
    }
}

