package com.mongo.mongokotlin.acid.domain.dto.subscriptionfetch

/**
 * DTO для ответа со списком подписок от внешнего сервиса
 */
data class SubscriptionListResponseDto(
    val subscriptions: List<ExternalSubscriptionDto>,
    val total: Int,
    val message: String? = null
)


