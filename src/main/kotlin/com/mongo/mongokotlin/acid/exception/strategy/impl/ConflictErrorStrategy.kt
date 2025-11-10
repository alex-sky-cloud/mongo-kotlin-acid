package com.mongo.mongokotlin.acid.exception.strategy.impl

import com.mongo.mongokotlin.acid.config.properties.ErrorStrategiesProperties
import com.mongo.mongokotlin.acid.exception.BusinessException
import com.mongo.mongokotlin.acid.exception.LogicErrorCode
import com.mongo.mongokotlin.acid.exception.strategy.ErrorHandlingStrategy
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/**
 * Стратегия обработки ошибки Conflict
 * HTTP код читается из ErrorStrategiesProperties
 */
@Component
class ConflictErrorStrategy(
    private val properties: ErrorStrategiesProperties
) : ErrorHandlingStrategy {
    
    override fun getStatusCode(): Int = properties.conflict
    
    override fun buildException(cause: Throwable, params: Map<String, String>): BusinessException {
        return BusinessException.builder()
            .errorCode(LogicErrorCode.SUBSCRIPTIONS_TEMPORARILY_UNAVAILABLE)
            .httpCode(HttpStatus.CONFLICT)
            .params(*params.map { it.key to it.value }.toTypedArray())
            .logLevel(BusinessException.LogLevel.WARN)
            .cause(cause)
            .build()
    }
}

