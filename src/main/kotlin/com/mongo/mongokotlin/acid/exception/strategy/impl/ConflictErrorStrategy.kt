package com.mongo.mongokotlin.acid.exception.strategy.impl

import com.mongo.mongokotlin.acid.config.properties.ErrorStrategiesProperties
import com.mongo.mongokotlin.acid.exception.BusinessException
import com.mongo.mongokotlin.acid.exception.ErrorContext
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
    
    override fun buildException(cause: Throwable, context: ErrorContext): BusinessException {
        // Достаём только нужные параметры из контекста
        return BusinessException.builder()
            .errorCode(LogicErrorCode.SUBSCRIPTIONS_TEMPORARILY_UNAVAILABLE)
            .httpCode(HttpStatus.CONFLICT)
            .params("customerId" to context.customerId!!)
            .logLevel(BusinessException.LogLevel.WARN)
            .cause(cause)
            .build()
    }
}

