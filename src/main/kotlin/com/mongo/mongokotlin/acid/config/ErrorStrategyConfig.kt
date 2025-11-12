package com.mongo.mongokotlin.acid.config

import com.mongo.mongokotlin.acid.exception.strategy.ErrorHandlingStrategy
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.stream.Collectors

/**
 * Конфигурация для автоматической регистрации стратегий обработки ошибок в Map
 * 
 * Spring IoC автоматически:
 * 1. Находит все @Component реализации ErrorHandlingStrategy
 * 2. Инжектит их как List<ErrorHandlingStrategy>
 * 3. Создаёт Map<Int, ErrorHandlingStrategy> с ключом = statusCode
 * 4. Регистрирует Map как Bean для использования в сервисах
 */
@Configuration
class ErrorStrategyConfig {
    
    private val log = LoggerFactory.getLogger(javaClass)
    
    /**
     * Создаёт Map bean для быстрого поиска стратегии по HTTP статус коду
     * 
     * Spring автоматически инжектит все реализации ErrorHandlingStrategy
     * и собирает их в Map где ключ = statusCode, значение = стратегия
     * 
     * @param strategies Set всех ErrorHandlingStrategy бинов (автоинжекция Spring)
     * @return Map<Int, ErrorHandlingStrategy> для использования в сервисах
     */
    @Bean
    fun errorStrategyMap(strategies: Set<ErrorHandlingStrategy>): Map<Int, ErrorHandlingStrategy> {
        log.info("🔧 Регистрация стратегий обработки ошибок в Map...")
        
        val strategyMap = strategies.stream()
            .collect(
                Collectors.toMap(
                    { strategy -> strategy.getStatusCode() },  // keyMapper: HTTP код как ключ (400, 403, 404...)
                    { strategy -> strategy },                   // valueMapper: стратегия как значение
                    { existing, duplicate ->  // mergeFunction: fail-fast при дубликатах
                        throw IllegalStateException(
                            "Duplicate error strategy for HTTP ${existing.getStatusCode()}: " +
                            "${existing.javaClass.simpleName} and ${duplicate.javaClass.simpleName}"
                        )
                    }
                )
            )
        
        log.info("✅ Зарегистрировано {} стратегий:", strategyMap.size)
        strategyMap.forEach { (code, strategy) ->
            log.info("   ➤ HTTP {} -> {}", code, strategy.javaClass.simpleName)
        }
        
        return strategyMap
    }
}

