package com.mongo.mongokotlin.acid.domain.service.subscriptionfetch

import com.mongo.mongokotlin.acid.domain.dto.subscriptionfetch.SubscriptionListResponseDto
import com.mongo.mongokotlin.acid.exception.BusinessException
import com.mongo.mongokotlin.acid.exception.ErrorContext
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
     * @param offerId ID предложения (опционально)
     * @param publicGatewayId ID платежного шлюза (опционально)
     * @return список подписок
     */
    suspend fun getCustomerSubscriptions(
        customerId: String,
        offerId: String? = null,
        publicGatewayId: String? = null
    ): SubscriptionListResponseDto {
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
            
            // Собираем все доступные бизнес-данные в блоке catch
            val context = ErrorContext(
                customerId = customerId,
                offerId = offerId,
                publicGatewayId = publicGatewayId,
                statusCode = ex.statusCode,
                statusMessage = ex.statusMessage,
                responseBody = ex.responseBody
            )
            
            throw handleExternalServiceError(ex, context)
        }
    }
    
    /**
     * Обработка ошибок от внешнего сервиса
     * Использует паттерн Strategy + Spring IoC Map для построения BusinessException
     * 
     * @param ex исключение от внешнего сервиса
     * @param context контекст с бизнес-данными для формирования сообщения об ошибке
     * @return BusinessException с правильным кодом ошибки и параметрами
     */
    private fun handleExternalServiceError(
        ex: ExternalServiceException,
        context: ErrorContext
    ): BusinessException {
        log.warn("⚠️ Обработка ошибки {} для клиента {}", ex.statusCode, context.customerId)
        
        // Получаем стратегию из Map (Spring IoC автоматически инжектит Map)
        val strategy = errorStrategyMap[ex.statusCode]
        
        // Если стратегия найдена - используем её
        return if (strategy != null) {
            strategy.buildException(
                cause = ex,
                context = context
            )
        } else {
            // Если не найдена - строим дефолтную ошибку
            log.warn("⚠️ Стратегия для HTTP {} не найдена, используется UNKNOWN", ex.statusCode)
            BusinessException.builder()
                .errorCode(LogicErrorCode.UNKNOWN_EXTERNAL_SERVICE_ERROR)
                .httpCode(HttpStatus.BAD_GATEWAY)
                .params("customerId" to context.customerId!!)
                .logLevel(BusinessException.LogLevel.WARN)
                .cause(ex)
                .build()
        }
    }
}

