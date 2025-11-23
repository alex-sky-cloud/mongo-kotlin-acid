package com.mongo.mongokotlin.acid.domain.controller.subscriptionsync

import com.mongo.mongokotlin.acid.domain.dto.SubscriptionDto
import com.mongo.mongokotlin.acid.domain.service.subscriptionsync.SubscriptionSyncService2
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Контроллер для синхронизации подписок - Подход 2: Flow и реактивные операторы
 * 
 * Endpoint: POST /api/subscriptions/sync/approach2
 * 
 * Использует Kotlin Flow для реактивной обработки данных
 */
@RestController
@RequestMapping("/api/subscriptions/sync")
class SubscriptionSyncController2(
    private val syncService: SubscriptionSyncService2
) {

    /**
     * Синхронизация подписок клиента с внешним сервисом
     * Подход 2: Flow и реактивные операторы
     * 
     * @param authUserId ID клиента из заголовка AUTH-USER-ID
     * @return список синхронизированных подписок
     */
    @PostMapping("/approach2")
    suspend fun syncSubscriptionsApproach2(
        @RequestHeader("AUTH-USER-ID") authUserId: String
    ): ResponseEntity<List<SubscriptionDto>> {
        val subscriptions = syncService.syncSubscriptions(authUserId)
        return ResponseEntity.ok(subscriptions)
    }
}

