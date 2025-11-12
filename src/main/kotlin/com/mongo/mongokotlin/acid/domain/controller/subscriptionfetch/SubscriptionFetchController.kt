package com.mongo.mongokotlin.acid.domain.controller.subscriptionfetch

import com.mongo.mongokotlin.acid.domain.dto.subscriptionfetch.SubscriptionListResponseDto
import com.mongo.mongokotlin.acid.domain.service.subscriptionfetch.SubscriptionFetchService
import com.mongo.mongokotlin.acid.exception.BusinessException
import com.mongo.mongokotlin.acid.exception.LogicErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Контроллер для получения списка подписок клиента от внешнего сервиса
 * Использует suspend функции для работы с корутинами
 * Обработка ошибок делегируется в @RestControllerAdvice
 */
@RestController
@RequestMapping("/api/subscriptions/fetch")
class SubscriptionFetchController(
    private val subscriptionFetchService: SubscriptionFetchService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    companion object {
        private const val CUSTOMER_ID_HEADER = "AUTH-USER-ID"
    }
    
    /**
     * Получить список подписок клиента от внешнего сервиса
     * 
     * GET /api/subscriptions/fetch
     * Header: AUTH-USER-ID - ID клиента
     * 
     * Возможные ответы:
     * - 200 OK - успешное получение подписок
     * - 400 Bad Request - некорректный запрос
     * - 403 Forbidden - доступ запрещен
     * - 404 Not Found - клиент не найден
     * - 409 Conflict - подписка не доступна
     * - 500 Internal Server Error - внутренняя ошибка сервера
     * 
     * @param customerId ID клиента из заголовка AUTH-USER-ID
     * @return список подписок или ошибка
     */
    @GetMapping
    suspend fun getCustomerSubscriptions(
        @RequestHeader(value = CUSTOMER_ID_HEADER, required = false) customerId: String?
    ): ResponseEntity<SubscriptionListResponseDto> {
        // Валидация наличия customerId
        if (customerId.isNullOrBlank()) {
            log.warn("Запрос без заголовка {}", CUSTOMER_ID_HEADER)
            throw BusinessException.builder()
                .errorCode(LogicErrorCode.MISSING_AUTH_USER_ID_HEADER)
                .httpCode(HttpStatus.BAD_REQUEST)
                .logLevel(BusinessException.LogLevel.WARN)
                .build()
        }
        
        log.info("Получен запрос на получение подписок для клиента: {}", customerId)
        
        val response = subscriptionFetchService.getCustomerSubscriptions(customerId)
        log.info("Успешно получены подписки для клиента: {}. Количество: {}", customerId, response.total)
        
        return ResponseEntity.ok(response)
    }
    
    /**
     * Информационный endpoint для тестирования различных сценариев
     * 
     * GET /api/subscriptions/fetch/test-scenarios
     * 
     * @return информация о тестовых сценариях
     */
    @GetMapping("/test-scenarios")
    suspend fun getTestScenarios(): ResponseEntity<String> {
        val info = """
            Тестовые сценарии для endpoint /api/subscriptions/fetch:
            
            1. Успешное получение подписок:
               curl -H "AUTH-USER-ID: customer-success" http://localhost:8080/api/subscriptions/fetch
            
            2. Ошибка 400 Bad Request:
               curl -H "AUTH-USER-ID: customer-bad-request" http://localhost:8080/api/subscriptions/fetch
            
            3. Ошибка 403 Forbidden:
               curl -H "AUTH-USER-ID: customer-forbidden" http://localhost:8080/api/subscriptions/fetch
            
            4. Ошибка 404 Not Found:
               curl -H "AUTH-USER-ID: customer-not-found" http://localhost:8080/api/subscriptions/fetch
            
            5. Ошибка 409 Conflict:
               curl -H "AUTH-USER-ID: customer-conflict" http://localhost:8080/api/subscriptions/fetch
            
            6. Ошибка 500 Internal Server Error:
               curl -H "AUTH-USER-ID: customer-server-error" http://localhost:8080/api/subscriptions/fetch
            
            7. Дефолтный ответ (любой другой customerId):
               curl -H "AUTH-USER-ID: any-customer-id" http://localhost:8080/api/subscriptions/fetch
            
            8. Отсутствие заголовка (ошибка 400):
               curl http://localhost:8080/api/subscriptions/fetch
        """.trimIndent()
        
        return ResponseEntity.ok(info)
    }
}

