package com.mongo.mongokotlin.acid.domain.controller.subscriptionsync

import com.mongo.mongokotlin.acid.domain.dto.SubscriptionDto
import com.mongo.mongokotlin.acid.domain.service.subscriptionsync.SubscriptionSyncService4
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Контроллер для синхронизации подписок - Подход 4: Транзакционная обработка
 * 
 * Endpoint: POST /api/subscriptions/sync/approach4
 * 
 * Использует транзакции для атомарной обработки всех операций
 * 
 * Примечание: Требует MongoDB replica set или sharded cluster
 */
@RestController
@RequestMapping("/api/subscriptions/sync")
class SubscriptionSyncController4(
    private val syncService: SubscriptionSyncService4
) {

    /**
     * Синхронизация подписок клиента с внешним сервисом
     * Подход 4: Транзакционная обработка
     * 
     * @param authUserId ID клиента из заголовка AUTH-USER-ID
     * @return список синхронизированных подписок
     */
    @PostMapping("/approach4")
    suspend fun syncSubscriptionsApproach4(
        @RequestHeader("AUTH-USER-ID") authUserId: String
    ): ResponseEntity<List<SubscriptionDto>> {
        val subscriptions = syncService.syncSubscriptions(authUserId)
        return ResponseEntity.ok(subscriptions)
    }
}

