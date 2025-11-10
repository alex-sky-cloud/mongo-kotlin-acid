package com.mongo.mongokotlin.acid.exception

import org.springframework.http.HttpStatus

/**
 * Универсальное бизнес-исключение с Builder паттерном
 */
class BusinessException private constructor(
    val logicErrorCode: LogicErrorCode,
    val httpStatus: HttpStatus,
    val params: Map<String, Any>,
    val logLevel: LogLevel,
    cause: Throwable?
) : RuntimeException(logicErrorCode.getMessage(), cause) {
    
    enum class LogLevel {
        ERROR, WARN, INFO
    }
    
    companion object {
        fun builder(): Builder = Builder()
    }
    
    class Builder {
        private var logicErrorCode: LogicErrorCode? = null
        private var httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
        private val params: MutableMap<String, Any> = mutableMapOf()
        private var logLevel: LogLevel = LogLevel.ERROR
        private var cause: Throwable? = null
        
        fun errorCode(logicErrorCode: LogicErrorCode): Builder {
            this.logicErrorCode = logicErrorCode
            return this
        }
        
        fun httpCode(httpStatus: HttpStatus): Builder {
            this.httpStatus = httpStatus
            return this
        }
        
        fun params(vararg pairs: Pair<String, Any>): Builder {
            this.params.putAll(pairs)
            return this
        }
        
        fun params(key: String, value: Any): Builder {
            this.params[key] = value
            return this
        }
        
        fun logLevel(logLevel: LogLevel): Builder {
            this.logLevel = logLevel
            return this
        }
        
        fun cause(cause: Throwable?): Builder {
            this.cause = cause
            return this
        }
        
        fun build(): BusinessException {
            requireNotNull(logicErrorCode) { "LogicErrorCode must be set" }
            return BusinessException(
                logicErrorCode = logicErrorCode!!,
                httpStatus = httpStatus,
                params = params.toMap(),
                logLevel = logLevel,
                cause = cause
            )
        }
    }
}

