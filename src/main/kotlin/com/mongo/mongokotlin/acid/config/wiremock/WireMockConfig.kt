package com.mongo.mongokotlin.acid.config.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

/**
 * Конфигурация для запуска WireMock сервера при старте приложения
 */
@Configuration
class WireMockConfig {
    
    private val log = LoggerFactory.getLogger(javaClass)
    
    companion object {
        const val WIREMOCK_PORT = 8090
        const val WIREMOCK_FILES_DIR = "src/main/resources/wiremock"
    }
    
    private lateinit var server: WireMockServer
    
    @PostConstruct
    fun startWireMock() {
        server = WireMockServer(
            WireMockConfiguration.options()
                .port(WIREMOCK_PORT)
                .usingFilesUnderDirectory(WIREMOCK_FILES_DIR)
        )
        
        server.start()
        log.info("WireMock сервер запущен на порту: $WIREMOCK_PORT")
        
        // Регистрация стабов
        WireMockStubsConfig.configureStubs(server)
    }
    
    @PreDestroy
    fun stopWireMock() {
        if (::server.isInitialized && server.isRunning) {
            server.stop()
            log.info("WireMock сервер остановлен")
        }
    }
}

