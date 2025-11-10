package com.mongo.mongokotlin.acid.config.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Конфигурация стабов для WireMock
 * Эмулирует различные сценарии ответов внешнего сервиса подписок
 * JSON ответы загружаются из файлов
 */
object WireMockStubsConfig {
    
    private val log = LoggerFactory.getLogger(javaClass)
    private const val RESPONSES_DIR = "src/main/resources/wiremock/responses"
    
    fun configureStubs(wireMockServer: WireMockServer) {
        log.info("Настройка WireMock стабов...")
        
        // 400 Bad Request - неверный запрос
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/external/subscriptions"))
                .withQueryParam("customerId", equalTo("customer-bad-request"))
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(loadJsonFromFile("error-400.json"))
                )
        )
        
        // Успешный ответ со списком подписок
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/external/subscriptions"))
                .withQueryParam("customerId", equalTo("customer-success"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(loadJsonFromFile("success-response.json"))
                )
        )
        
        // 403 Forbidden - доступ запрещен
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/external/subscriptions"))
                .withQueryParam("customerId", equalTo("customer-forbidden"))
                .willReturn(
                    aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withBody(loadJsonFromFile("error-403.json"))
                )
        )
        
        // 404 Not Found - клиент не найден
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/external/subscriptions"))
                .withQueryParam("customerId", equalTo("customer-not-found"))
                .willReturn(
                    aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody(loadJsonFromFile("error-404.json"))
                )
        )
        
        // 409 Conflict - подписка не доступна
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/external/subscriptions"))
                .withQueryParam("customerId", equalTo("customer-conflict"))
                .willReturn(
                    aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody(loadJsonFromFile("error-409.json"))
                )
        )
        
        // 500 Internal Server Error
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/external/subscriptions"))
                .withQueryParam("customerId", equalTo("customer-server-error"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(loadJsonFromFile("error-500.json"))
                )
        )
        
        log.info("✅ WireMock стабы успешно настроены. Всего стабов: 6 (success, 400, 403, 404, 409, 500)")
        log.info("🔹 Тестовый customerId для 400: customer-bad-request")
        log.info("🔹 Тестовый customerId для success: customer-success")
    }
    
    /**
     * Загрузка JSON из файла
     */
    private fun loadJsonFromFile(fileName: String): String {
        return try {
            val path = Paths.get(RESPONSES_DIR, fileName)
            Files.readString(path)
        } catch (e: Exception) {
            log.error("Ошибка при загрузке файла $fileName: ${e.message}")
            throw RuntimeException("Не удалось загрузить JSON файл: $fileName", e)
        }
    }
}

