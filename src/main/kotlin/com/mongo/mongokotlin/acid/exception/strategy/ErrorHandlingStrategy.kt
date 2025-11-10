package com.mongo.mongokotlin.acid.exception.strategy

import com.mongo.mongokotlin.acid.exception.BusinessException

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
     * @param params параметры для сообщения об ошибке
     * @return готовое BusinessException
     */
    fun buildException(cause: Throwable, params: Map<String, String>): BusinessException
}

