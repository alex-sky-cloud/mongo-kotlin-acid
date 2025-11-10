# Как добавить новую ошибку

> **📖 Смотрите также:**
> - [ERROR-HANDLING-ARCHITECTURE.md](ERROR-HANDLING-ARCHITECTURE.md) - Полная архитектура с диаграммами
> - [QUICK-START-NEW-ERROR.md](QUICK-START-NEW-ERROR.md) - Быстрый старт

---

## 🎯 Пример: добавление ошибки 410 Gone

### Шаг 1: Добавить код в LogicErrorCode enum

```kotlin
// LogicErrorCode.kt
enum class LogicErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    private val messageKey: String
) : TypicalException {
    
    // ... существующие коды ...
    
    // Новая ошибка 410
    RESOURCE_GONE(
        code = "RESOURCE_GONE",
        httpStatus = HttpStatus.GONE,
        messageKey = "error.subscription.fetch.gone"
    );
    
    override fun getMessage(): String = messageKey
    override fun getType(): String = code
}
```

### Шаг 2: Добавить сообщение в application.yml

```yaml
# application.yml

# Настройка кодов ошибок для стратегий
error:
  strategies:
    bad-request: 400
    forbidden: 403
    not-found: 404
    conflict: 409
    internal-server-error: 500
    gone: 410  # ⬅️ ДОБАВИЛИ НОВЫЙ КОД

# Cloud Messages
error.subscription.fetch.gone: "Ресурс подписки для пользователя {customerId} больше не доступен"  # ⬅️ ДОБАВИЛИ СООБЩЕНИЕ
```

### Шаг 3: Создать класс стратегии

```kotlin
// GoneErrorStrategy.kt
package com.mongo.mongokotlin.acid.exception.strategy.impl

import com.mongo.mongokotlin.acid.exception.BusinessException
import com.mongo.mongokotlin.acid.exception.LogicErrorCode
import com.mongo.mongokotlin.acid.exception.strategy.ErrorHandlingStrategy
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/**
 * Стратегия обработки ошибки Gone
 * HTTP код читается из application.yml: error.strategies.gone
 */
@Component
class GoneErrorStrategy(
    @Value("\${error.strategies.gone}") private val statusCode: Int
) : ErrorHandlingStrategy {
    
    override fun getStatusCode(): Int = statusCode
    
    override fun buildException(cause: Throwable, params: Map<String, String>): BusinessException {
        return BusinessException.builder()
            .errorCode(LogicErrorCode.RESOURCE_GONE)
            .httpCode(HttpStatus.GONE)
            .params(*params.map { it.key to it.value }.toTypedArray())
            .logLevel(BusinessException.LogLevel.WARN)
            .cause(cause)
            .build()
    }
}
```

### Шаг 4: Готово! ✅

**Spring автоматически:**
1. ✅ Найдёт `@Component GoneErrorStrategy`
2. ✅ Инжектит значение `410` из `error.strategies.gone`
3. ✅ Зарегистрирует в `Map<Int, ErrorHandlingStrategy>`
4. ✅ Сервис сможет использовать: `errorStrategyMap[410]`

**Никаких изменений в:**
- ❌ ErrorStrategyConfig
- ❌ SubscriptionFetchService
- ❌ Других стратегиях

---

## 📋 Что происходит при старте приложения

```
Spring Boot запускается
    ↓
1. Читает application.yml
   error.strategies.gone = 410
    ↓
2. Создаёт @Component GoneErrorStrategy
   Конструктор получает @Value("${error.strategies.gone}") → 410
    ↓
3. ErrorStrategyConfig.errorStrategyMap() вызывается
   Spring инжектит List<ErrorHandlingStrategy>
   List содержит: [BadRequest, Forbidden, NotFound, Conflict, InternalServer, Gone]
    ↓
4. Config собирает Map через stream().collect()
   Map = {
     400 -> BadRequestErrorStrategy,
     403 -> ForbiddenErrorStrategy,
     404 -> NotFoundErrorStrategy,
     409 -> ConflictErrorStrategy,
     500 -> InternalServerErrorStrategy,
     410 -> GoneErrorStrategy  ⬅️ АВТОМАТИЧЕСКИ ДОБАВЛЕНО
   }
    ↓
5. Map регистрируется как @Bean
    ↓
6. SubscriptionFetchService инжектит Map
    ↓
7. При ошибке 410:
   errorStrategyMap[410] → GoneErrorStrategy
   strategy.buildException(...) → BusinessException
    ↓
8. ExceptionApiHandler ловит BusinessException
   Формирует ErrorResponseDto с сообщением из yml
```

---

## 🎨 Преимущества подхода

### 1. Декларативность
```yaml
# Все коды ошибок в одном месте
error:
  strategies:
    bad-request: 400
    forbidden: 403
    gone: 410  # Легко увидеть все поддерживаемые коды
```

### 2. Гибкость
Можно изменить код без перекомпиляции:
```yaml
# Было
error:
  strategies:
    conflict: 409

# Стало (если нужно поменять)
error:
  strategies:
    conflict: 429  # Too Many Requests вместо Conflict
```

### 3. Автоматическая регистрация
```kotlin
@Component  // ⬅️ Всё что нужно - Spring сам найдёт
class GoneErrorStrategy(
    @Value("\${error.strategies.gone}") private val statusCode: Int  // ⬅️ Автоинжекция
)
```

### 4. Нет дублирования
Вместо hardcoded значений в каждом классе:
```kotlin
// ❌ Плохо - hardcoded
override fun getStatusCode(): Int = 410

// ✅ Хорошо - из конфига
override fun getStatusCode(): Int = statusCode
```

---

## 🧪 Тестирование новой ошибки

### 1. Добавить WireMock стаб

```kotlin
// WireMockStubsConfig.kt
wireMockServer.stubFor(
    get(urlPathEqualTo("/api/external/subscriptions"))
        .withQueryParam("customerId", equalTo("customer-gone"))
        .willReturn(
            aResponse()
                .withStatus(410)
                .withHeader("Content-Type", "application/json")
                .withBody("""{"errorCode": 410, "errorMessage": "Gone"}""")
        )
)
```

### 2. Протестировать через curl

```bash
curl -H "AUTH-USER-ID: customer-gone" http://localhost:8080/api/subscriptions/fetch
```

### 3. Ожидаемый ответ

```json
{
  "errorId": "abc123...",
  "errorCode": "RESOURCE_GONE",
  "level": "ERROR",
  "messages": {
    "ru": "Ресурс подписки для пользователя customer-gone больше не доступен"
  }
}
```

---

## 📊 Сравнение: До и После

### ❌ До (hardcoded)

```kotlin
@Component
class GoneErrorStrategy : ErrorHandlingStrategy {
    override fun getStatusCode(): Int = 410  // Hardcoded
}
```

**Проблемы:**
- Нужно менять код для изменения значения
- Нет централизованного места с кодами
- Сложно увидеть все поддерживаемые коды

### ✅ После (конфигурируемо)

```yaml
# application.yml - всё в одном месте
error:
  strategies:
    gone: 410
```

```kotlin
@Component
class GoneErrorStrategy(
    @Value("\${error.strategies.gone}") private val statusCode: Int  // Из конфига
) : ErrorHandlingStrategy {
    override fun getStatusCode(): Int = statusCode
}
```

**Преимущества:**
- ✅ Централизованная конфигурация
- ✅ Легко изменить без перекомпиляции
- ✅ Все коды видны в yml
- ✅ Spring IoC автоматически инжектит

---

## 🔄 Spring IoC в действии

```
application.yml
  error.strategies.gone: 410
         ↓
    Spring @Value
         ↓
GoneErrorStrategy(statusCode = 410)
         ↓
   Spring @Component
         ↓
List<ErrorHandlingStrategy>
         ↓
ErrorStrategyConfig.errorStrategyMap()
         ↓
Map<Int, ErrorHandlingStrategy>
  {410 -> GoneErrorStrategy}
         ↓
SubscriptionFetchService
  errorStrategyMap[410].buildException()
```

---

## ✅ Итого: 3 простых шага

1. **Enum** - добавить в `LogicErrorCode`
2. **YAML** - добавить код и сообщение в `application.yml`
3. **Класс** - создать `@Component` с `@Value`

**Spring сделает всё остальное!** 🚀

---

**Дата:** 2025-11-10  
**Паттерн:** Strategy + IoC + @Value

