package com.mongo.mongokotlin.acid.domain.service.subscriptionfetch

import com.mongo.mongokotlin.acid.domain.dto.subscriptionfetch.SubscriptionListResponseDto
import com.mongo.mongokotlin.acid.exception.BusinessException
import com.mongo.mongokotlin.acid.exception.LogicErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

/**
 * Сервис для получения подписок клиента через внешний сервис
 * Обрабатывает различные ошибки и предоставляет удобный API для контроллера
 * Использует suspend функции для работы с корутинами
 */
@Service
class SubscriptionFetchService(
    private val externalClient: ExternalSubscriptionClient
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    /**
     * Получить список подписок для клиента
     * 
     * @param customerId ID клиента
     * @return список подписок
     */
    suspend fun getCustomerSubscriptions(customerId: String): SubscriptionListResponseDto {
        log.info("Получение подписок для клиента: {}", customerId)
        
        return try {
            val response = externalClient.fetchSubscriptions(customerId)
            log.info("Подписки успешно получены для клиента: {}. Всего: {}", customerId, response.total)
            response
        } catch (ex: ExternalServiceException) {
            log.error(
                "Ошибка внешнего сервиса при получении подписок. Код: {}, Сообщение: {}",
                ex.statusCode, ex.statusMessage
            )
            throw handleExternalServiceError(ex, customerId)
        } catch (ex: Exception) {
            log.error("Непредвиденная ошибка при получении подписок для клиента: {}", customerId, ex)
            throw BusinessException.builder()
                .errorCode(LogicErrorCode.UNEXPECTED_ERROR)
                .httpCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .params("customerId" to customerId, "details" to (ex.message ?: ""))
                .logLevel(BusinessException.LogLevel.ERROR)
                .cause(ex)
                .build()
        }
    }
    
    /**
     * Обработка ошибок от внешнего сервиса
     * Конструирует BusinessException с правильным кодом ошибки и параметрами
     */
    private fun handleExternalServiceError(
        ex: ExternalServiceException,
        customerId: String
    ): BusinessException {
        log.warn("Обработка ошибки {} для клиента {}", ex.statusCode, customerId)
        
        return when (ex.statusCode) {
            400 -> BusinessException.builder()
                .errorCode(LogicErrorCode.INVALID_REQUEST_FETCH_SUBSCRIPTIONS)
                .httpCode(HttpStatus.BAD_REQUEST)
                .params("customerId" to customerId)
                .logLevel(BusinessException.LogLevel.WARN)
                .cause(ex)
                .build()
            
            403 -> BusinessException.builder()
                .errorCode(LogicErrorCode.FORBIDDEN_ACCESS_SUBSCRIPTIONS)
                .httpCode(HttpStatus.FORBIDDEN)
                .params("customerId" to customerId)
                .logLevel(BusinessException.LogLevel.WARN)
                .cause(ex)
                .build()
            
            404 -> BusinessException.builder()
                .errorCode(LogicErrorCode.CUSTOMER_NOT_FOUND_IN_EXTERNAL_SERVICE)
                .httpCode(HttpStatus.NOT_FOUND)
                .params("customerId" to customerId)
                .logLevel(BusinessException.LogLevel.WARN)
                .cause(ex)
                .build()
            
            409 -> BusinessException.builder()
                .errorCode(LogicErrorCode.SUBSCRIPTIONS_TEMPORARILY_UNAVAILABLE)
                .httpCode(HttpStatus.CONFLICT)
                .params("customerId" to customerId)
                .logLevel(BusinessException.LogLevel.WARN)
                .cause(ex)
                .build()
            
            500 -> BusinessException.builder()
                .errorCode(LogicErrorCode.EXTERNAL_SERVICE_INTERNAL_ERROR)
                .httpCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .params("customerId" to customerId)
                .logLevel(BusinessException.LogLevel.WARN)
                .cause(ex)
                .build()
            
            else -> BusinessException.builder()
                .errorCode(LogicErrorCode.UNKNOWN_EXTERNAL_SERVICE_ERROR)
                .httpCode(HttpStatus.BAD_GATEWAY)
                .params("customerId" to customerId)
                .logLevel(BusinessException.LogLevel.WARN)
                .cause(ex)
                .build()
        }
    }
}

