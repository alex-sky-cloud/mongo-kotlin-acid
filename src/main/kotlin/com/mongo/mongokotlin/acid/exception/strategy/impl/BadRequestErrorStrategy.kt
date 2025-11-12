package com.mongo.mongokotlin.acid.exception.strategy.impl

import com.mongo.mongokotlin.acid.config.properties.ErrorStrategiesProperties
import com.mongo.mongokotlin.acid.exception.BusinessException
import com.mongo.mongokotlin.acid.exception.ErrorContext
import com.mongo.mongokotlin.acid.exception.LogicErrorCode
import com.mongo.mongokotlin.acid.exception.strategy.ErrorHandlingStrategy
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/**
 * Стратегия обработки ошибки Bad Request
 * HTTP код читается из ErrorStrategiesProperties
 */
@Component
class BadRequestErrorStrategy(
    private val properties: ErrorStrategiesProperties
) : ErrorHandlingStrategy {
    
    override fun getStatusCode(): Int = properties.badRequest
    
    override fun buildException(cause: Throwable, context: ErrorContext): BusinessException {
        // Достаём только нужные параметры из контекста
        return BusinessException.builder()
            .errorCode(LogicErrorCode.INVALID_REQUEST_FETCH_SUBSCRIPTIONS)
            .httpCode(HttpStatus.BAD_REQUEST)
            .params("customerId" to context.customerId!!)
            .logLevel(BusinessException.LogLevel.WARN)
            .cause(cause)
            .build()
    }
}

