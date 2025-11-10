package com.mongo.mongokotlin.acid.domain.service.subscriptionfetch

/**
 * Исключение для ошибок внешнего сервиса подписок
 */
class ExternalServiceException(
    val statusCode: Int,
    val statusMessage: String,
    val responseBody: String
) : RuntimeException("Ошибка внешнего сервиса: [$statusCode] $statusMessage")


