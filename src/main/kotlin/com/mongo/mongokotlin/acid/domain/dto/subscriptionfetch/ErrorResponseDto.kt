package com.mongo.mongokotlin.acid.domain.dto.subscriptionfetch

import java.util.*

/**
 * DTO для структурированного ответа с ошибкой
 * Формат соответствует корпоративному стандарту
 */
data class ErrorResponseDto(
    val errorId: String = UUID.randomUUID().toString().replace("-", ""),
    val errorCode: String,
    val level: String = "ERROR",
    val messages: Map<String, String>
) {
    companion object {
        fun create(
            errorCode: String,
            messageRu: String,
            errorId: String = UUID.randomUUID().toString().replace("-", "")
        ): ErrorResponseDto {
            return ErrorResponseDto(
                errorId = errorId,
                errorCode = errorCode,
                level = "ERROR",
                messages = mapOf("ru" to messageRu)
            )
        }
    }
}

