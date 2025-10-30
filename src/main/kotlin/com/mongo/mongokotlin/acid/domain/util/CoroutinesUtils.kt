package com.mongo.mongokotlin.acid.domain.util

import kotlinx.coroutines.CancellationException

/**
 * Безопасно выполняет блок кода с обработкой ошибок, НЕ поглощая CancellationException.
 * 
 * Аналог стандартного `runCatching`, но с корректной обработкой отмены корутин:
 * - Если блок выполнился успешно — возвращает `Result.success(value)`
 * - Если блок выбросил `CancellationException` — **пробрасывает её дальше** (не поглощает)
 * - Если блок выбросил любое другое исключение — возвращает `Result.failure(exception)`
 * 
 * **Для обычных (не suspend) блоков кода.**
 * 
 * @param block Блок кода для выполнения
 * @return Result<R> — обёрнутый результат или ошибка
 * 
 * Пример:
 * ```kotlin
 * val result = runCatchingCancellable {
 *     expensiveCalculation()
 * }
 * result.onSuccess { println("Успех: $it") }
 *       .onFailure { println("Ошибка: $it") }
 * ```
 */
inline fun <R> runCatchingCancellable(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        if (e is CancellationException) {
            throw e
        } else {
            Result.failure(e)
        }
    }
}

/**
 * Безопасно выполняет suspend блок кода с обработкой ошибок, НЕ поглощая CancellationException.
 * 
 * Аналог стандартного `runCatching`, но для suspend функций с корректной обработкой отмены корутин:
 * - Если блок выполнился успешно — возвращает `Result.success(value)`
 * - Если блок выбросил `CancellationException` — **пробрасывает её дальше** (не поглощает)
 * - Если блок выбросил любое другое исключение — возвращает `Result.failure(exception)`
 * 
 * **Для suspend блоков кода (асинхронные операции, корутины).**
 * 
 * @param block Suspend блок кода для выполнения
 * @return Result<R> — обёрнутый результат или ошибка
 * 
 * Пример:
 * ```kotlin
 * val result = runCatchingCancellableSuspend {
 *     apiClient.fetchData()
 * }
 * result.onSuccess { data -> processData(data) }
 *       .onFailure { error -> log.error("API error", error) }
 * ```
 */
suspend inline fun <R> runCatchingCancellableSuspend(crossinline block: suspend () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        if (e is CancellationException) {
            throw e
        } else {
            Result.failure(e)
        }
    }
}


