package com.mongo.mongokotlin.acid.domain.controller

import com.mongo.mongokotlin.acid.domain.model.SubscriptionEntity
import com.mongo.mongokotlin.acid.domain.service.SubscriptionInitService
import kotlinx.coroutines.flow.Flow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/subscriptions/init")
class SubscriptionInitController(
    private val subscriptionInitService: SubscriptionInitService
) {
    
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Инициализирует подписки для пользователя.
     * Не помечен как suspend, так как просто возвращает Flow из сервиса.
     * 
     * @param cus идентификатор пользователя из заголовка
     * @param count количество подписок для создания (по умолчанию 5)
     * @return поток созданных подписок
     */
    @PostMapping
    fun initializeSubscriptions(
        @RequestHeader("AUTH-USER-ID") cus: String,
        @RequestParam(defaultValue = "5") count: Int
    ): Flow<SubscriptionEntity> {
        log.info("Запрос на инициализацию {} подписок для cus: {}", count, cus)
        return subscriptionInitService.initializeSubscriptions(cus, count)
    }

    /**
     * Удаляет все подписки для пользователя
     * 
     * @param cus идентификатор пользователя из заголовка
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deleteAllSubscriptions(
        @RequestHeader("AUTH-USER-ID") cus: String
    ) {
        log.info("Запрос на удаление всех подписок для cus: {}", cus)
        subscriptionInitService.deleteAllByCus(cus)
    }
}

