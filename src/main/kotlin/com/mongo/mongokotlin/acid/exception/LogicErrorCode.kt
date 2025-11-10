package com.mongo.mongokotlin.acid.exception

/**
 * Enum с кодами логических ошибок приложения
 * Коды используются для поиска сообщений в MessageSource (Cloud Messages)
 */
enum class LogicErrorCode(private val messageCode: String) : TypicalException {
    
    // Ошибки получения подписок от внешнего сервиса
    INVALID_REQUEST_FETCH_SUBSCRIPTIONS("error.subscription.fetch.invalid.request"),
    FORBIDDEN_ACCESS_SUBSCRIPTIONS("error.subscription.fetch.forbidden"),
    CUSTOMER_NOT_FOUND_IN_EXTERNAL_SERVICE("error.subscription.fetch.customer.not.found"),
    SUBSCRIPTIONS_TEMPORARILY_UNAVAILABLE("error.subscription.fetch.temporarily.unavailable"),
    EXTERNAL_SERVICE_INTERNAL_ERROR("error.subscription.fetch.external.service.error"),
    UNKNOWN_EXTERNAL_SERVICE_ERROR("error.subscription.fetch.unknown.error"),
    
    // Общие ошибки
    MISSING_AUTH_USER_ID_HEADER("error.auth.missing.user.id.header"),
    UNEXPECTED_ERROR("error.unexpected");
    
    override fun getType(): String = name
    
    override fun getMessage(): String = messageCode
}

