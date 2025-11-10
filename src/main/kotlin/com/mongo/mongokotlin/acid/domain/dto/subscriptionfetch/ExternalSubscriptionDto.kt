package com.mongo.mongokotlin.acid.domain.dto.subscriptionfetch

import java.time.LocalDateTime

/**
 * DTO для подписки от внешнего клиента
 */
data class ExternalSubscriptionDto(
    val subscriptionId: String,
    val customerId: String,
    val productId: String,
    val status: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val price: Double,
    val billingPeriod: String
)


