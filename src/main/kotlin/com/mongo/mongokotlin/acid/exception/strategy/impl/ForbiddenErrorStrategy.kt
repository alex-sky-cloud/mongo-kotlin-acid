package com.mongo.mongokotlin.acid.exception.strategy.impl

import com.mongo.mongokotlin.acid.config.properties.ErrorStrategiesProperties
import com.mongo.mongokotlin.acid.exception.BusinessException
import com.mongo.mongokotlin.acid.exception.ErrorContext
import com.mongo.mongokotlin.acid.exception.LogicErrorCode
import com.mongo.mongokotlin.acid.exception.strategy.ErrorHandlingStrategy
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/**
 * Стратегия обработки ошибки Forbidden
 * HTTP код читается из ErrorStrategiesProperties
 */
@Component
class ForbiddenErrorStrategy(
    private val properties: ErrorStrategiesProperties
) : ErrorHandlingStrategy {
    
    override fun getStatusCode(): Int = properties.forbidden
    
    override fun buildException(cause: Throwable, context: ErrorContext): BusinessException {
        // Достаём только нужные параметры из контекста
        return BusinessException.builder()
            .errorCode(LogicErrorCode.FORBIDDEN_ACCESS_SUBSCRIPTIONS)
            .httpCode(HttpStatus.FORBIDDEN)
            .params("customerId" to context.customerId!!)
            .logLevel(BusinessException.LogLevel.WARN)
            .cause(cause)
            .build()
    }
}

