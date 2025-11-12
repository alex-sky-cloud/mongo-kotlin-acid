package com.mongo.mongokotlin.acid.exception

/**
 * Контекст ошибки с nullable полями для всех возможных параметров
 * Каждая стратегия сама выбирает какие параметры использовать
 */
data class  ErrorContext(
    val customerId: String? = null,
    val offerId: String? = null,
    val publicGatewayId: String? = null,
    val statusCode: Int? = null,
    val statusMessage: String? = null,
    val responseBody: String? = null
)

