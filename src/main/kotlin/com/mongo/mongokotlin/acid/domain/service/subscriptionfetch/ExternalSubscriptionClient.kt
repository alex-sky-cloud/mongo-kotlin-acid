package com.mongo.mongokotlin.acid.domain.service.subscriptionfetch

import com.mongo.mongokotlin.acid.domain.dto.subscriptionfetch.SubscriptionListResponseDto
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody

/**
 * WebClient для запросов к внешнему сервису подписок
 * Использует suspend функции для интеграции с корутинами
 */
@Component
class ExternalSubscriptionClient(
    private val webClientBuilder: WebClient.Builder
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    @Value("\${external.subscription.service.url:http://localhost:8090}")
    private lateinit var externalServiceUrl: String
    
    private val webClient by lazy { webClientBuilder.build() }
    
    /**
     * Получить список подписок клиента от внешнего сервиса
     * 
     * @param customerId ID клиента
     * @return список подписок или исключение
     */
    suspend fun fetchSubscriptions(customerId: String): SubscriptionListResponseDto {
        val fullUrl = "$externalServiceUrl/api/external/subscriptions?customerId=$customerId"
        log.info("🔹 Запрос подписок для клиента: {} к URL: {}", customerId, fullUrl)
        
        return try {
            val response = webClient.get()
                .uri("$externalServiceUrl/api/external/subscriptions?customerId={customerId}", customerId)
                .retrieve()
                .awaitBody<SubscriptionListResponseDto>()
            
            log.info("✅ Успешно получены подписки для клиента: {}. Количество: {}", customerId, response.total)
            response
        } catch (ex: WebClientResponseException) {
            log.error("❌ WebClient ошибка {} для клиента: {}. Тело: {}", ex.statusCode, customerId, ex.responseBodyAsString)
            handleWebClientError(ex)
        } catch (ex: Exception) {
            log.error("❌ Непредвиденная ошибка при получении подписок для клиента: {}", customerId, ex)
            throw ex
        }
    }
    
    /**
     * Обработка ошибок WebClient с детальным логированием
     */
    private fun handleWebClientError(ex: WebClientResponseException): Nothing {
        val status = ex.statusCode as HttpStatus
        val errorBody = ex.responseBodyAsString
        
        log.error("Ошибка внешнего сервиса. Статус: {}, Тело ответа: {}", status.value(), errorBody)
        
        // Пробрасываем ошибку дальше для обработки в сервисном слое
        throw ExternalServiceException(
            statusCode = status.value(),
            statusMessage = status.reasonPhrase,
            responseBody = errorBody
        )
    }
}


