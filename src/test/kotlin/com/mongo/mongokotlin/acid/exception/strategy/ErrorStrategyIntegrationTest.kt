package com.mongo.mongokotlin.acid.exception.strategy

import com.mongo.mongokotlin.acid.config.ErrorStrategyConfig
import com.mongo.mongokotlin.acid.config.properties.ErrorStrategiesProperties
import com.mongo.mongokotlin.acid.exception.LogicErrorCode
import com.mongo.mongokotlin.acid.exception.strategy.impl.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Интеграционный тест для проверки работы стратегий обработки ошибок
 * Тестирует только необходимые компоненты без полного контекста приложения
 * Использует src/test/resources/application.yml для конфигурации
 */
@SpringBootTest(classes = [ErrorStrategyIntegrationTest.TestConfig::class])
class ErrorStrategyIntegrationTest {

    /**
     * Минимальная конфигурация для теста
     * Загружает только классы участвующие в создании стратегий
     */
    @Configuration
    @EnableConfigurationProperties(ErrorStrategiesProperties::class)
    @ComponentScan(basePackages = ["com.mongo.mongokotlin.acid.exception.strategy.impl"])
    class TestConfig : ErrorStrategyConfig()

    @Autowired
    private lateinit var errorStrategyMap: Map<Int, ErrorHandlingStrategy>

    @Autowired
    private lateinit var properties: ErrorStrategiesProperties

    @Autowired
    private lateinit var badRequestStrategy: BadRequestErrorStrategy

    @Autowired
    private lateinit var forbiddenStrategy: ForbiddenErrorStrategy

    @Autowired
    private lateinit var notFoundStrategy: NotFoundErrorStrategy

    @Autowired
    private lateinit var conflictStrategy: ConflictErrorStrategy

    @Autowired
    private lateinit var internalServerErrorStrategy: InternalServerErrorStrategy

    @Test
    fun `должен загрузить ErrorStrategiesProperties из тестовых properties`() {
        // Given & When - Spring загружает конфигурацию

        // Then
        assertNotNull(properties)
        assertEquals(400, properties.badRequest)
        assertEquals(403, properties.forbidden)
        assertEquals(404, properties.notFound)
        assertEquals(409, properties.conflict)
        assertEquals(500, properties.internalServerError)
    }

    @Test
    fun `должен создать все бины стратегий`() {
        // Given & When - Spring создаёт бины

        // Then
        assertNotNull(badRequestStrategy)
        assertNotNull(forbiddenStrategy)
        assertNotNull(notFoundStrategy)
        assertNotNull(conflictStrategy)
        assertNotNull(internalServerErrorStrategy)
    }

    @Test
    fun `каждая стратегия должна возвращать правильный statusCode из properties`() {
        // Given & When
        val badRequestCode = badRequestStrategy.getStatusCode()
        val forbiddenCode = forbiddenStrategy.getStatusCode()
        val notFoundCode = notFoundStrategy.getStatusCode()
        val conflictCode = conflictStrategy.getStatusCode()
        val internalServerErrorCode = internalServerErrorStrategy.getStatusCode()

        // Then
        assertEquals(400, badRequestCode)
        assertEquals(403, forbiddenCode)
        assertEquals(404, notFoundCode)
        assertEquals(409, conflictCode)
        assertEquals(500, internalServerErrorCode)
    }

    @Test
    fun `должен создать Map bean со всеми стратегиями`() {
        // Given & When - Spring создаёт Map через ErrorStrategyConfig

        // Then
        assertNotNull(errorStrategyMap)
        assertEquals(5, errorStrategyMap.size)
        
        assertTrue(errorStrategyMap.containsKey(400))
        assertTrue(errorStrategyMap.containsKey(403))
        assertTrue(errorStrategyMap.containsKey(404))
        assertTrue(errorStrategyMap.containsKey(409))
        assertTrue(errorStrategyMap.containsKey(500))
    }

    @Test
    fun `Map должна содержать правильные экземпляры стратегий`() {
        // Given & When
        val strategy400 = errorStrategyMap[400]
        val strategy403 = errorStrategyMap[403]
        val strategy404 = errorStrategyMap[404]
        val strategy409 = errorStrategyMap[409]
        val strategy500 = errorStrategyMap[500]

        // Then
        assertTrue(strategy400 is BadRequestErrorStrategy)
        assertTrue(strategy403 is ForbiddenErrorStrategy)
        assertTrue(strategy404 is NotFoundErrorStrategy)
        assertTrue(strategy409 is ConflictErrorStrategy)
        assertTrue(strategy500 is InternalServerErrorStrategy)
    }

    @Test
    fun `BadRequestErrorStrategy должна строить правильное исключение`() {
        // Given
        val cause = RuntimeException("Test error")
        val context = com.mongo.mongokotlin.acid.exception.ErrorContext(customerId = "test-customer-123")

        // When
        val exception = badRequestStrategy.buildException(cause, context)

        // Then
        assertNotNull(exception)
        assertEquals(LogicErrorCode.INVALID_REQUEST_FETCH_SUBSCRIPTIONS, exception.logicErrorCode)
        assertEquals(HttpStatus.BAD_REQUEST, exception.httpStatus)
        assertEquals("test-customer-123", exception.params["customerId"])
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `ForbiddenErrorStrategy должна строить правильное исключение`() {
        // Given
        val cause = RuntimeException("Test error")
        val context = com.mongo.mongokotlin.acid.exception.ErrorContext(customerId = "test-customer-456")

        // When
        val exception = forbiddenStrategy.buildException(cause, context)

        // Then
        assertNotNull(exception)
        assertEquals(LogicErrorCode.FORBIDDEN_ACCESS_SUBSCRIPTIONS, exception.logicErrorCode)
        assertEquals(HttpStatus.FORBIDDEN, exception.httpStatus)
        assertEquals("test-customer-456", exception.params["customerId"])
    }

    @Test
    fun `NotFoundErrorStrategy должна строить правильное исключение`() {
        // Given
        val cause = RuntimeException("Test error")
        val context = com.mongo.mongokotlin.acid.exception.ErrorContext(customerId = "test-customer-789")

        // When
        val exception = notFoundStrategy.buildException(cause, context)

        // Then
        assertNotNull(exception)
        assertEquals(LogicErrorCode.CUSTOMER_NOT_FOUND_IN_EXTERNAL_SERVICE, exception.logicErrorCode)
        assertEquals(HttpStatus.NOT_FOUND, exception.httpStatus)
        assertEquals("test-customer-789", exception.params["customerId"])
    }

    @Test
    fun `ConflictErrorStrategy должна строить правильное исключение`() {
        // Given
        val cause = RuntimeException("Test error")
        val context = com.mongo.mongokotlin.acid.exception.ErrorContext(customerId = "test-customer-111")

        // When
        val exception = conflictStrategy.buildException(cause, context)

        // Then
        assertNotNull(exception)
        assertEquals(LogicErrorCode.SUBSCRIPTIONS_TEMPORARILY_UNAVAILABLE, exception.logicErrorCode)
        assertEquals(HttpStatus.CONFLICT, exception.httpStatus)
        assertEquals("test-customer-111", exception.params["customerId"])
    }

    @Test
    fun `InternalServerErrorStrategy должна строить правильное исключение`() {
        // Given
        val cause = RuntimeException("Test error")
        val context = com.mongo.mongokotlin.acid.exception.ErrorContext(customerId = "test-customer-222")

        // When
        val exception = internalServerErrorStrategy.buildException(cause, context)

        // Then
        assertNotNull(exception)
        assertEquals(LogicErrorCode.EXTERNAL_SERVICE_INTERNAL_ERROR, exception.logicErrorCode)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.httpStatus)
        assertEquals("test-customer-222", exception.params["customerId"])
    }

    @Test
    fun `Spring IoC должен автоматически зарегистрировать все стратегии в Map`() {
        // Given - Spring создаёт контекст

        // When - Проверяем что все стратегии автоматически попали в Map
        val registeredStrategies = errorStrategyMap.values

        // Then
        assertEquals(5, registeredStrategies.size)
        
        val strategyClasses = registeredStrategies.map { it::class }
        assertTrue(strategyClasses.contains(BadRequestErrorStrategy::class))
        assertTrue(strategyClasses.contains(ForbiddenErrorStrategy::class))
        assertTrue(strategyClasses.contains(NotFoundErrorStrategy::class))
        assertTrue(strategyClasses.contains(ConflictErrorStrategy::class))
        assertTrue(strategyClasses.contains(InternalServerErrorStrategy::class))
    }
}

