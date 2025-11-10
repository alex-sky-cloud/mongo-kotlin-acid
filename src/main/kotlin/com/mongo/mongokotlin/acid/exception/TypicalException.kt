package com.mongo.mongokotlin.acid.exception

/**
 * Базовый интерфейс для всех типов исключений в приложении
 */
interface TypicalException {
    
    /**
     * Тип исключения (код ошибки)
     */
    fun getType(): String
    
    /**
     * Сообщение об ошибке с возможностью параметризации
     */
    fun getMessage(): String
}

