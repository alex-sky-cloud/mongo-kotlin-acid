# Быстрый старт: Добавление новой ошибки

## 🚀 3 простых шага

### ✅ Шаг 1: LogicErrorCode
```kotlin
// LogicErrorCode.kt
RESOURCE_GONE(
    code = "RESOURCE_GONE",
    httpStatus = HttpStatus.GONE,
    messageKey = "error.subscription.fetch.gone"
)
```

### ✅ Шаг 2: ErrorStrategiesProperties
```kotlin
// ErrorStrategiesProperties.kt
@ConfigurationProperties(prefix = "error.strategies")
data class ErrorStrategiesProperties(
    val badRequest: Int = 400,
    val forbidden: Int = 403,
    val gone: Int = 410  // ← НОВОЕ
)
```

### ✅ Шаг 3: application.yml + Strategy класс

**application.yml:**
```yaml
error:
  strategies:
    gone: 410  # ← НОВОЕ

error.subscription.fetch.gone: "Ресурс для {customerId} больше не доступен"
```

**GoneErrorStrategy.kt:**
```kotlin
@Component
class GoneErrorStrategy(
    private val properties: ErrorStrategiesProperties
) : ErrorHandlingStrategy {
    
    override fun getStatusCode(): Int = properties.gone
    
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

## 🎯 Готово!

Spring автоматически:
1. ✅ Создаст GoneErrorStrategy bean
2. ✅ Зарегистрирует в Map<410, Strategy>
3. ✅ Сервис сможет использовать

**Никаких изменений в:**
- ❌ ErrorStrategyConfig
- ❌ SubscriptionFetchService
- ❌ Других классах

---

📖 **Подробная документация:** [ERROR-HANDLING-ARCHITECTURE.md](ERROR-HANDLING-ARCHITECTURE.md)

