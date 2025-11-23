package com.mongo.mongokotlin.acid.domain.controller.subscriptionsync

import com.mongo.mongokotlin.acid.domain.dto.SubscriptionDto
import com.mongo.mongokotlin.acid.domain.service.subscriptionsync.SubscriptionSyncService3
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Контроллер для синхронизации подписок - Подход 3: Custom repository methods
 * 
 * Endpoint: POST /api/subscriptions/sync/approach3
 * 
 * Использует оптимизированные нативные запросы MongoDB
 */
@RestController
@RequestMapping("/api/subscriptions/sync")
class SubscriptionSyncController3(
    private val syncService: SubscriptionSyncService3
) {

    /**
     * Синхронизация подписок клиента с внешним сервисом
     * Подход 3: Custom repository methods с derived queries
     * 
     * @param authUserId ID клиента из заголовка AUTH-USER-ID
     * @return список синхронизированных подписок
     */
    @PostMapping("/approach3")
    suspend fun syncSubscriptionsApproach3(
        @RequestHeader("AUTH-USER-ID") authUserId: String
    ): ResponseEntity<List<SubscriptionDto>> {
        val subscriptions = syncService.syncSubscriptions(authUserId)
        return ResponseEntity.ok(subscriptions)
    }
}

