package com.mongo.mongokotlin.acid.exception.strategy

import com.mongo.mongokotlin.acid.exception.BusinessException
import com.mongo.mongokotlin.acid.exception.ErrorContext

/**
 * Интерфейс стратегии для обработки ошибок внешнего сервиса
 * Паттерн Strategy + Spring IoC для автоматической регистрации реализаций в Map
 * 
 * Каждая реализация - отдельный класс @Component
 * Spring автоматически соберёт их в Map<Int, ErrorHandlingStrategy> через Config
 */
interface ErrorHandlingStrategy {
    
    /**
     * HTTP статус код для идентификации стратегии
     * Используется как ключ в Map<Int, ErrorHandlingStrategy>
     */
    fun getStatusCode(): Int
    
    /**
     * Построить BusinessException для данной ошибки
     * 
     * @param cause исходное исключение от внешнего сервиса
     * @param context контекст с nullable полями - каждая стратегия выбирает нужные
     * @return готовое BusinessException
     */
    fun buildException(cause: Throwable, context: ErrorContext): BusinessException
}

