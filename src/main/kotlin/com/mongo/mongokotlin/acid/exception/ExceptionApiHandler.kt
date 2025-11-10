package com.mongo.mongokotlin.acid.exception

import com.mongo.mongokotlin.acid.domain.dto.subscriptionfetch.ErrorResponseDto
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Глобальный обработчик исключений для REST API
 * Использует Cloud Messages из application.yml для формирования ответов
 */
@RestControllerAdvice
class ExceptionApiHandler(
    private val environment: Environment
) {
    
    private val log = LoggerFactory.getLogger(javaClass)
    
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ErrorResponseDto> {
        // Логируем согласно уровню из исключения
        when (ex.logLevel) {
            BusinessException.LogLevel.ERROR -> log.error(
                "Business exception: {} ({})", 
                ex.logicErrorCode, 
                ex.httpStatus, 
                ex
            )
            BusinessException.LogLevel.WARN -> log.warn(
                "Business exception: {} ({})", 
                ex.logicErrorCode, 
                ex.httpStatus
            )
            BusinessException.LogLevel.INFO -> log.info(
                "Business exception: {} ({})", 
                ex.logicErrorCode, 
                ex.httpStatus
            )
        }
        
        // Получаем шаблон сообщения из application.yml
        val messageTemplate = environment.getProperty(ex.logicErrorCode.getMessage())
            ?: "Ошибка при выполнении операции"
        
        // Подставляем параметры в шаблон
        val messageRu = substituteParams(messageTemplate, ex.params)
        
        val errorResponse = ErrorResponseDto.create(
            errorCode = ex.logicErrorCode.getType(),
            messageRu = messageRu
        )
        
        return ResponseEntity
            .status(ex.httpStatus)
            .body(errorResponse)
    }
    
    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(ex: Exception): ResponseEntity<ErrorResponseDto> {
        log.error("Unexpected exception", ex)
        
        val messageTemplate = environment.getProperty(LogicErrorCode.UNEXPECTED_ERROR.getMessage())
            ?: "Непредвиденная ошибка при выполнении операции"
        
        val params = mapOf(
            "customerId" to "",
            "details" to (ex.message ?: "")
        )
        val messageRu = substituteParams(messageTemplate, params)
        
        val errorResponse = ErrorResponseDto.create(
            errorCode = LogicErrorCode.UNEXPECTED_ERROR.getType(),
            messageRu = messageRu
        )
        
        return ResponseEntity
            .status(500)
            .body(errorResponse)
    }
    
    /**
     * Подстановка параметров в шаблон сообщения
     * Заменяет {paramName} на значение из мапы параметров
     */
    private fun substituteParams(template: String, params: Map<String, Any>): String {
        var result = template
        params.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }
        return result
    }
}

