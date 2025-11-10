# Интеграционные тесты стратегий обработки ошибок

## Назначение

Интеграционные тесты проверяют корректность работы **паттерна Strategy + Spring IoC** для обработки ошибок.

## ErrorStrategyIntegrationTest

### Что тестируется

1. **@ConfigurationProperties биндинг**
   - Spring корректно читает `error.strategies.*` из test properties
   - Значения правильно биндятся к полям `ErrorStrategiesProperties`

2. **Создание бинов стратегий**
   - Spring находит все `@Component` классы стратегий
   - Каждая стратегия создаётся как bean
   - `ErrorStrategiesProperties` инжектится в конструкторы

3. **Автоматическая регистрация в Map**
   - `ErrorStrategyConfig` собирает все стратегии в `Map<Int, Strategy>`
   - Map содержит правильные ключи (HTTP коды)
   - Map содержит правильные экземпляры стратегий

4. **Построение BusinessException**
   - Каждая стратегия корректно строит `BusinessException`
   - Правильный `LogicErrorCode`
   - Правильный `HttpStatus`
   - Параметры корректно передаются

### Минимальный контекст

Тест загружает **только необходимые компоненты:**

**src/test/resources/application.yml:**
```yaml
error:
  strategies:
    badRequest: 400
    forbidden: 403
    notFound: 404
    conflict: 409
    internalServerError: 500
```

**Тестовый класс:**
```kotlin
@SpringBootTest(classes = [ErrorStrategyIntegrationTest.TestConfig::class])
class ErrorStrategyIntegrationTest {
    
    @Configuration
    @EnableConfigurationProperties(ErrorStrategiesProperties::class)
    @ComponentScan(basePackages = ["com.mongo.mongokotlin.acid.exception.strategy.impl"])
    class TestConfig : ErrorStrategyConfig()
}
```

**Загружается:**
- ✅ `ErrorStrategiesProperties` - класс свойств
- ✅ `ErrorStrategyConfig` - конфигурация Map
- ✅ Все стратегии из `exception.strategy.impl`

**НЕ загружается:**
- ❌ MongoDB
- ❌ WireMock
- ❌ Controllers
- ❌ Services
- ❌ Другие компоненты приложения

### Запуск тестов

```bash
# Все тесты
./gradlew test

# Только интеграционные тесты стратегий
./gradlew test --tests ErrorStrategyIntegrationTest

# С подробным выводом
./gradlew test --tests ErrorStrategyIntegrationTest --info
```

### Пример теста

```kotlin
@Test
fun `Spring IoC должен автоматически зарегистрировать все стратегии в Map`() {
    // Given - Spring создаёт контекст
    
    // When - Проверяем что все стратегии автоматически попали в Map
    val registeredStrategies = errorStrategyMap.values
    
    // Then
    assertEquals(5, registeredStrategies.size)
    
    val strategyClasses = registeredStrategies.map { it::class }
    assertTrue(strategyClasses.contains(BadRequestErrorStrategy::class))
    assertTrue(strategyClasses.contains(ForbiddenErrorStrategy::class))
    // ...
}
```

## Что подтверждают тесты

✅ **Spring IoC работает корректно**
- Автоматически находит все `@Component` стратегии
- Собирает их в `List<ErrorHandlingStrategy>`
- Передаёт в `ErrorStrategyConfig.errorStrategyMap()`

✅ **@ConfigurationProperties работает**
- Значения из yml корректно биндятся
- Properties инжектятся в стратегии

✅ **Паттерн Strategy реализован правильно**
- Все стратегии реализуют общий интерфейс
- Каждая стратегия строит правильное исключение

✅ **Map<Int, Strategy> создаётся автоматически**
- Ключи соответствуют HTTP кодам
- Значения - правильные экземпляры

---

**Дата:** 2025-11-10  
**Тип:** Integration Tests  
**Фокус:** Strategy Pattern + Spring IoC

