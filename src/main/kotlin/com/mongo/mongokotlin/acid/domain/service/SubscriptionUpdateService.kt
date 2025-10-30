package com.mongo.mongokotlin.acid.domain.service

import com.mongo.mongokotlin.acid.config.VendorProperties
import com.mongo.mongokotlin.acid.domain.dto.SubscriptionDto
import com.mongo.mongokotlin.acid.domain.mapper.SubscriptionMapper
import com.mongo.mongokotlin.acid.domain.repository.SubscriptionRepository
import com.mongo.mongokotlin.acid.domain.util.runCatchingCancellableSuspend
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SubscriptionUpdateService(
    private val subscriptionRepository: SubscriptionRepository,
    private val vendorService: VendorService,
    private val mapper: SubscriptionMapper,
    private val payCoroutineScope: CoroutineScope,
    private val vendorProperties: VendorProperties
) {
    
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun updateSubscriptionsWithVendorData(cus: String): List<SubscriptionDto> {
        // Загружаем все подписки пользователя из БД
        val subscriptions = subscriptionRepository.findByCus(cus).toList()
        // Если подписок нет — сразу возвращаем пустой ответ
        if (subscriptions.isEmpty()) return emptyList()
        // Создаем быстрый индекс: publicId -> entity (для сопоставления с данными вендора)
        val subscriptionMap = subscriptions.associateBy { it.publicId }
        // Запускаем фоновое обновление (клиент не ждет, получает данные из БД сразу)
        payCoroutineScope.launch {
            // Пакетный запрос к вендору по cus с таймаутом 200мс
            val vendorResult = runCatchingCancellableSuspend {
                withTimeout(vendorProperties.timeout) {
                    vendorService.fetchVendorDataForCus(cus, subscriptionMap.keys)
                }
            }
            
            vendorResult.onSuccess { vendorDtos ->
                // Сопоставляем по publicId и обновляем
                val entitiesToUpdate = vendorDtos.mapNotNull { vendorDto ->
                    subscriptionMap[vendorDto.publicId]?.apply {
                        mapper.updateEntityWithVendorData(this, vendorDto)
                    }
                }
                if (entitiesToUpdate.isNotEmpty()) {
                    runCatchingCancellableSuspend {
                        subscriptionRepository.saveAll(entitiesToUpdate).collect()
                    }.onSuccess {
                        log.info("Пакетно обновлено {} подписок в БД", entitiesToUpdate.size)
                    }.onFailure { error ->
                        log.error("Ошибка сохранения подписок в БД для cus: {}", cus, error)
                    }
                }
            }.onFailure { error ->
                when (error) {
                    is TimeoutCancellationException -> log.error("Timeout пакетного запроса к вендору для cus: {}", cus)
                    else -> log.error("Ошибка пакетного запроса к вендору для cus: {}", cus, error)
                }
            }
        }
        // Возвращаем список подписок из БД (без ожидания фонового обновления)
        return subscriptions.map { mapper.toDto(it) }
    }

}

