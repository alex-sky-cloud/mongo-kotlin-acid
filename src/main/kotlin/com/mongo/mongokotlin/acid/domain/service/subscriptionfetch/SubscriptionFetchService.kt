package com.mongo.mongokotlin.acid.domain.service.subscriptionfetch

import com.mongo.mongokotlin.acid.domain.dto.subscriptionfetch.SubscriptionListResponseDto
import com.mongo.mongokotlin.acid.exception.BusinessException
import com.mongo.mongokotlin.acid.exception.LogicErrorCode
import com.mongo.mongokotlin.acid.exception.strategy.ErrorHandlingStrategy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

/**
 * Сервис для получения подписок клиента через внешний сервис
 * Обрабатывает различные ошибки используя паттерн Strategy
 * Spring IoC автоматически инжектит Map<Int, ErrorHandlingStrategy>
 */
@Service
class SubscriptionFetchService(
    private val externalClient: ExternalSubscriptionClient,
    private val errorStrategyMap: Map<Int, ErrorHandlingStrategy>
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
     * Использует паттерн Strategy + Spring IoC Map для построения BusinessException
     * 
     * @param ex исключение от внешнего сервиса
     * @param customerId ID клиента для параметров сообщения
     * @return BusinessException с правильным кодом ошибки и параметрами
     */
    private fun handleExternalServiceError(
        ex: ExternalServiceException,
        customerId: String
    ): BusinessException {
        log.warn("⚠️ Обработка ошибки {} для клиента {}", ex.statusCode, customerId)
        
        // Получаем стратегию из Map (Spring IoC автоматически инжектит Map)
        val strategy = errorStrategyMap[ex.statusCode]
        
        // Если стратегия найдена - используем её
        return if (strategy != null) {
            strategy.buildException(
                cause = ex,
                params = mapOf("customerId" to customerId)
            )
        } else {
            // Если не найдена - строим дефолтную ошибку
            log.warn("⚠️ Стратегия для HTTP {} не найдена, используется UNKNOWN", ex.statusCode)
            BusinessException.builder()
                .errorCode(LogicErrorCode.UNKNOWN_EXTERNAL_SERVICE_ERROR)
                .httpCode(HttpStatus.BAD_GATEWAY)
                .params("customerId" to customerId)
                .logLevel(BusinessException.LogLevel.WARN)
                .cause(ex)
                .build()
        }
    }
}

