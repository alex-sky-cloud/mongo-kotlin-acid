# Система получения подписок от внешнего сервиса (Kotlin + Coroutines)

## 📋 Описание

Реализована система получения списка подписок клиента от внешнего сервиса с использованием **Kotlin корутин** и **WireMock** для имитации внешнего API. Система корректно обрабатывает различные сценарии ответов, включая успешные и ошибочные ситуации.

## 🏗️ Архитектура решения

### Созданные компоненты:

#### 1. DTO классы (`domain/dto/subscriptionfetch/`)

##### Kotlin data classes - компактные и выразительные
- **`ExternalSubscriptionDto`** - DTO для одной подписки от внешнего сервиса
  ```kotlin
  data class ExternalSubscriptionDto(
      val subscriptionId: String,
      val customerId: String,
      val productId: String,
      val status: String,
      val startDate: LocalDateTime,
      val endDate: LocalDateTime,
      val price: Double,
      val billingPeriod: String
  )
  ```
  
- **`SubscriptionListResponseDto`** - DTO для списка подписок
  ```kotlin
  data class SubscriptionListResponseDto(
      val subscriptions: List<ExternalSubscriptionDto>,
      val total: Int,
      val message: String? = null
  )
  ```
  
- **`ErrorResponseDto`** - DTO для ошибок
  ```kotlin
  data class ErrorResponseDto(
      val errorCode: Int,
      val errorMessage: String,
      val details: String? = null
  )
  ```

#### 2. Клиент внешнего сервиса (`domain/service/subscriptionfetch/`)

##### `ExternalSubscriptionClient` - реактивный WebClient с suspend функциями
- Использует `suspend` функции для интеграции с корутинами
- WebClient с расширением `awaitBody<T>()` для неблокирующей работы
- Настроен на URL: `http://localhost:8090` (WireMock)
- Выполняет GET запросы с параметром `customerId`
- Обрабатывает ошибки WebClient и преобразует в `ExternalServiceException`
- Детальное логирование всех операций

```kotlin
suspend fun fetchSubscriptions(customerId: String): SubscriptionListResponseDto {
    return webClient.get()
        .uri("$externalServiceUrl/api/external/subscriptions?customerId={customerId}", customerId)
        .retrieve()
        .awaitBody<SubscriptionListResponseDto>()
}
```

##### `ExternalServiceException` - исключение для ошибок внешнего сервиса
```kotlin
class ExternalServiceException(
    val statusCode: Int,
    val statusMessage: String,
    val responseBody: String
) : RuntimeException("Ошибка внешнего сервиса: [$statusCode] $statusMessage")
```

#### 3. Сервисный слой (`domain/service/subscriptionfetch/`)

##### `SubscriptionFetchService` - бизнес-логика с корутинами
- Suspend функции для асинхронной обработки
- Обработка различных ошибочных сценариев:
  - 400 - "Некорректный запрос к внешнему сервису"
  - 403 - "Доступ к подпискам запрещен"
  - 404 - "Клиент не найден во внешнем сервисе"
  - 409 - "Подписка временно не доступна"
  - 500 - "Внутренняя ошибка внешнего сервиса"
- Преобразование технических ошибок в понятные бизнес-ошибки

```kotlin
suspend fun getCustomerSubscriptions(customerId: String): SubscriptionListResponseDto {
    return try {
        externalClient.fetchSubscriptions(customerId)
    } catch (ex: ExternalServiceException) {
        throw handleExternalServiceError(ex, customerId)
    }
}
```

##### `SubscriptionFetchException` - бизнес-исключение
```kotlin
class SubscriptionFetchException(
    val errorCode: Int,
    message: String,
    val details: String? = null
) : RuntimeException(message)
```

#### 4. Контроллер (`domain/controller/subscriptionfetch/`)

##### `SubscriptionFetchController` - REST endpoint с suspend функциями
- Endpoint: `GET /api/subscriptions/fetch`
- Использует заголовок `AUTH-USER-ID` для передачи ID клиента
- **Suspend функция** для работы с корутинами
- Валидация наличия обязательного заголовка
- Маппинг кодов ошибок в HTTP статусы
- Информационный endpoint: `GET /api/subscriptions/fetch/test-scenarios`

```kotlin
@GetMapping
suspend fun getCustomerSubscriptions(
    @RequestHeader(value = CUSTOMER_ID_HEADER, required = false) customerId: String?
): ResponseEntity<*> {
    // Реактивная обработка запроса
}
```

#### 5. Конфигурация WireMock (`config/wiremock/`)

##### `WireMockConfig` - настройка и запуск WireMock
- Запуск WireMock сервера при старте приложения
- Порт: 8090
- Автоматическая регистрация стабов
- Graceful shutdown при остановке приложения
- Использование `by lazy` для инициализации WebClient

##### `WireMockStubsConfig` - Kotlin object для стабов
- Настройка стабов для различных сценариев (8 сценариев)
- Использование Kotlin multiline strings (`""" """.trimIndent()`)
- Успешный ответ, Bad Request, Forbidden, Not Found, Conflict, Server Error
- Дефолтный ответ для любых других ID

## 🚀 Использование

### Запуск приложения

```bash
# Перейти в директорию проекта
cd D:\git\!_kotlin_projects\mongo-kotlin-acid

# Запустить приложение (WireMock запустится автоматически на порту 8090)
gradlew bootRun
```

### Тестовые сценарии

#### 1. ✅ Успешное получение подписок

```bash
curl -H "AUTH-USER-ID: customer-success" http://localhost:8080/api/subscriptions/fetch
```

**Ожидаемый ответ:** 200 OK
```json
{
  "subscriptions": [
    {
      "subscriptionId": "sub-001",
      "customerId": "customer-success",
      "productId": "premium-monthly",
      "status": "ACTIVE",
      "startDate": "2024-01-01T00:00:00",
      "endDate": "2025-01-01T00:00:00",
      "price": 29.99,
      "billingPeriod": "MONTHLY"
    },
    {
      "subscriptionId": "sub-002",
      "customerId": "customer-success",
      "productId": "basic-yearly",
      "status": "ACTIVE",
      "startDate": "2024-06-15T00:00:00",
      "endDate": "2025-06-15T00:00:00",
      "price": 99.99,
      "billingPeriod": "YEARLY"
    },
    {
      "subscriptionId": "sub-003",
      "customerId": "customer-success",
      "productId": "enterprise-monthly",
      "status": "PENDING",
      "startDate": "2024-11-01T00:00:00",
      "endDate": "2024-12-01T00:00:00",
      "price": 199.99,
      "billingPeriod": "MONTHLY"
    }
  ],
  "total": 3,
  "message": "Подписки успешно получены"
}
```

#### 2-6. Ошибочные сценарии

```bash
# 400 Bad Request
curl -H "AUTH-USER-ID: customer-bad-request" http://localhost:8080/api/subscriptions/fetch

# 403 Forbidden
curl -H "AUTH-USER-ID: customer-forbidden" http://localhost:8080/api/subscriptions/fetch

# 404 Not Found
curl -H "AUTH-USER-ID: customer-not-found" http://localhost:8080/api/subscriptions/fetch

# 409 Conflict
curl -H "AUTH-USER-ID: customer-conflict" http://localhost:8080/api/subscriptions/fetch

# 500 Internal Server Error
curl -H "AUTH-USER-ID: customer-server-error" http://localhost:8080/api/subscriptions/fetch
```

#### 7. Дефолтный ответ
```bash
curl -H "AUTH-USER-ID: any-customer-id" http://localhost:8080/api/subscriptions/fetch
```

#### 8. Отсутствие заголовка
```bash
curl http://localhost:8080/api/subscriptions/fetch
```

#### 9. Информация о тестовых сценариях
```bash
curl http://localhost:8080/api/subscriptions/fetch/test-scenarios
```

## 🔑 Ключевые особенности Kotlin реализации

### 1. Kotlin Coroutines - неблокирующая асинхронность

```kotlin
// Suspend функция автоматически работает асинхронно
suspend fun getCustomerSubscriptions(customerId: String): SubscriptionListResponseDto {
    return externalClient.fetchSubscriptions(customerId)
}
```

### 2. Data classes - компактный код

```kotlin
// Всего 9 строк вместо 100+ в Java!
data class ExternalSubscriptionDto(
    val subscriptionId: String,
    val customerId: String,
    val productId: String,
    val status: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val price: Double,
    val billingPeriod: String
)
```

### 3. Null-safety - безопасность на уровне языка

```kotlin
// Компилятор не даст забыть про null
@RequestHeader(value = CUSTOMER_ID_HEADER, required = false) customerId: String?

if (customerId.isNullOrBlank()) {
    // Обработка null
}
```

### 4. Smart casts и when expressions

```kotlin
private fun handleExternalServiceError(ex: ExternalServiceException, customerId: String) {
    val errorMessage = when (ex.statusCode) {
        400 -> "Некорректный запрос к внешнему сервису"
        403 -> "Доступ к подпискам запрещен"
        404 -> "Клиент не найден во внешнем сервисе"
        409 -> "Подписка временно не доступна"
        500 -> "Внутренняя ошибка внешнего сервиса"
        else -> "Ошибка при обращении к внешнему сервису"
    }
}
```

### 5. Extension functions - WebClient extensions

```kotlin
// Использование Kotlin extensions для WebClient
val response = webClient.get()
    .uri("...")
    .retrieve()
    .awaitBody<SubscriptionListResponseDto>()  // Kotlin extension!
```

### 6. String templates и multiline strings

```kotlin
// Multiline strings с интерполяцией
private fun getSuccessResponse() = """
    {
      "subscriptions": [
        {
          "subscriptionId": "sub-001",
          "customerId": "customer-success"
        }
      ]
    }
""".trimIndent()
```

### 7. Object declaration для конфигурации

```kotlin
// Singleton через object вместо static методов
object WireMockStubsConfig {
    fun configureStubs(wireMockServer: WireMockServer) {
        // Настройка стабов
    }
}
```

## 📊 Структура проекта (Kotlin)

```
src/main/kotlin/com/mongo/mongokotlin/acid/
├── config/
│   └── wiremock/
│       ├── WireMockConfig.kt             # Конфигурация WireMock
│       └── WireMockStubsConfig.kt        # Стабы (Kotlin object)
├── domain/
│   ├── controller/
│   │   └── subscriptionfetch/
│   │       └── SubscriptionFetchController.kt  # REST endpoint (suspend)
│   ├── dto/
│   │   └── subscriptionfetch/
│   │       ├── ExternalSubscriptionDto.kt      # data class
│   │       ├── SubscriptionListResponseDto.kt  # data class
│   │       └── ErrorResponseDto.kt             # data class
│   └── service/
│       └── subscriptionfetch/
│           ├── ExternalSubscriptionClient.kt   # WebClient (suspend)
│           ├── SubscriptionFetchService.kt     # Бизнес-логика (suspend)
│           ├── ExternalServiceException.kt     # Exception class
│           └── SubscriptionFetchException.kt   # Exception class
```

## 🔧 Конфигурация

### build.gradle.kts (Kotlin DSL)
```kotlin
dependencies {
    // Kotlin dependencies
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    
    // WireMock
    implementation("org.wiremock:wiremock-standalone:3.3.1")
}
```

### application.yml
```yaml
# Настройки внешнего сервиса подписок (WireMock)
external:
  subscription:
    service:
      url: http://localhost:8090
```

## ✅ Выполненные требования

- [x] Endpoint для получения списка подписок клиента
- [x] Сервисный слой с запросами к внешнему клиенту
- [x] Имитация внешнего клиента через WireMock (встроен в проект)
- [x] Использование WebClient для реактивных запросов
- [x] **Kotlin корутины (suspend функции)** для асинхронности
- [x] **Kotlin data classes** для DTO
- [x] Эмуляция успешного ответа со списком подписок
- [x] Эмуляция ошибок: 400, 403, 404, 409, 500
- [x] Стабы для всех сценариев
- [x] Отдельные каталоги на каждом слое
- [x] Детальное логирование
- [x] Использование заголовка AUTH-USER-ID

## 🎯 Преимущества Kotlin решения

1. **Компактность** - в 3-5 раз меньше кода чем в Java
2. **Null-safety** - исключены NullPointerException на этапе компиляции
3. **Корутины** - простая асинхронность без callback hell
4. **Data classes** - автоматические equals, hashCode, toString, copy
5. **Smart casts** - автоматическое приведение типов
6. **Extension functions** - расширение функциональности без наследования
7. **String templates** - удобная работа со строками
8. **When expressions** - мощная замена switch
9. **Default parameters** - меньше перегруженных методов
10. **Type inference** - компилятор выводит типы автоматически

## 📈 Сравнение с Java версией

| Характеристика | Java | Kotlin |
|---------------|------|--------|
| Строк кода DTO | ~100 | ~10 |
| Null-safety | Аннотации | Встроенная |
| Асинхронность | Reactive Mono | Suspend функции |
| Boilerplate код | Много | Минимум |
| Immutability | final везде | val по умолчанию |
| String операции | concat/format | String templates |

---

**Технологии:** Kotlin 1.9.25, Spring Boot 3.5.6, Kotlin Coroutines, WireMock 3.3.1  
**Автор:** AI Assistant  
**Дата:** 2025-11-10


