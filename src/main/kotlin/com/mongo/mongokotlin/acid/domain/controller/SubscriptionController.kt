package com.mongo.mongokotlin.acid.domain.controller

import com.mongo.mongokotlin.acid.domain.dto.SubscriptionDto
import com.mongo.mongokotlin.acid.domain.service.SubscriptionUpdateService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/subscriptions")
class SubscriptionController(
    private val subscriptionUpdateService: SubscriptionUpdateService
) {
    
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Обновляет подписки пользователя данными от вендора.
     * 
     * Алгоритм:
     * 1. Получает документы из БД по cus пользователя
     * 2. СРАЗУ формирует и возвращает ответ клиенту с текущими данными из БД
     * 3. ПАРАЛЛЕЛЬНО в фоне запускает обновление от вендора
     * 4. Обновление сопоставляется по publicId и сохраняется в БД пакетно
     * 
     * Клиент получает данные моментально, обновление идет в фоне!
     * 
     * @param cus идентификатор пользователя из заголовка AUTH-USER-ID
     * @return список подписок с текущими данными из БД
     */
    @PutMapping
    suspend fun updateSubscriptions(
        @RequestHeader("AUTH-USER-ID") cus: String
    ): ResponseEntity<List<SubscriptionDto>> {
        val subscriptions = subscriptionUpdateService.updateSubscriptionsWithVendorData(cus)
        return ResponseEntity.ok(subscriptions)
    }
}

