package com.mongo.mongokotlin.acid.domain.controller.subscriptionsync

import com.mongo.mongokotlin.acid.domain.dto.SubscriptionDto
import com.mongo.mongokotlin.acid.domain.service.subscriptionsync.SubscriptionSyncService1
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RequestHeader

/**
 * Контроллер для синхронизации подписок - Подход 1: Batch операции
 * 
 * Endpoint: POST /api/subscriptions/sync/approach1
 * 
 * Использует подход с batch операциями через saveAll
 */
@RestController
@RequestMapping("/api/subscriptions/sync")
class SubscriptionSyncController1(
    private val syncService: SubscriptionSyncService1
) {

    /**
     * Синхронизация подписок клиента с внешним сервисом
     * Подход 1: Batch операции (saveAll)
     * 
     * @param authUserId ID клиента из заголовка AUTH-USER-ID
     * @return список синхронизированных подписок
     */
    @PostMapping("/approach1")
    suspend fun syncSubscriptionsApproach1(
        @RequestHeader("AUTH-USER-ID") authUserId: String
    ): ResponseEntity<List<SubscriptionDto>> {
        val subscriptions = syncService.syncSubscriptions(authUserId)
        return ResponseEntity.ok(subscriptions)
    }
}

