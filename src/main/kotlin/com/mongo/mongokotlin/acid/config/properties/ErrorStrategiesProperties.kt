package com.mongo.mongokotlin.acid.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Свойства для конфигурации HTTP кодов ошибок стратегий
 * Читается из application.yml: error.strategies.*
 */
@ConfigurationProperties(prefix = "error.strategies")
data class ErrorStrategiesProperties(
    val badRequest: Int = 400,
    val forbidden: Int = 403,
    val notFound: Int = 404,
    val conflict: Int = 409,
    val internalServerError: Int = 500
)


