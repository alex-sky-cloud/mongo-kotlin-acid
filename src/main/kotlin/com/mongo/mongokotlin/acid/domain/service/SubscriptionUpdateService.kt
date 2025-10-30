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
            // Пакетный запрос к вендору по cus с таймаутом
            try {
                val vendorDtos = withTimeout(vendorProperties.timeout) {
                    vendorService.fetchVendorDataForCus(cus, subscriptionMap.keys)
                }
                
                // Успешно получили данные от вендора
                // Сопоставляем по publicId и обновляем (убираем дубликаты)
                val entitiesToUpdate = vendorDtos
                    .distinctBy { it.publicId }  // Убираем дубликаты по publicId
                    .mapNotNull { vendorDto ->
                        val entity = subscriptionMap[vendorDto.publicId]
                        if (entity == null) {
                            log.warn("Получен vendorDto для несуществующего publicId: {}", vendorDto.publicId)
                        }
                        entity?.apply {
                            mapper.updateEntityWithVendorData(this, vendorDto)
                        }
                    }
                
                if (entitiesToUpdate.isNotEmpty()) {
                    runCatchingCancellableSuspend {
                        // Имитация БД ошибки: если первая entity имеет vendorStatus = "CORRUPTED"
                        if (entitiesToUpdate.first().vendorStatus == "CORRUPTED") {
                            throw IllegalStateException("⚠️ Имитация БД ошибки: невозможно сохранить CORRUPTED status")
                        }
                        subscriptionRepository.saveAll(entitiesToUpdate).collect()
                    }.onSuccess {
                        log.info("Пакетно обновлено {} подписок в БД", entitiesToUpdate.size)
                    }.onFailure { error ->
                        log.error("Ошибка сохранения подписок в БД для cus: {}", cus, error)
                    }
                }
                
            } catch (e: TimeoutCancellationException) {
                log.error("Timeout пакетного запроса к вендору для cus: {}", cus)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.error("Ошибка пакетного запроса к вендору для cus: {}", cus, e)
            }
        }
        // Возвращаем список подписок из БД (без ожидания фонового обновления)
        return subscriptions.map { mapper.toDto(it) }
    }

}

