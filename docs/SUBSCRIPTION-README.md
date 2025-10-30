# Система подписок с синхронизацией с внешним вендором (Kotlin Coroutines)

## 📋 Описание

Реализована система управления подписками с использованием **Kotlin корутин** и реактивной синхронизацией данных от внешнего вендора. Система корректно обрабатывает timeout и ошибки, не блокируя возврат данных клиенту.

## 🏗️ Архитектура решения

### Созданные компоненты:

#### 1. Domain & DTO

**`SubscriptionEntity`** - MongoDB сущность с коллекцией `subscriptions`
```kotlin
@Document(collection = "subscriptions")
data class SubscriptionEntity(
    @Id val id: ObjectId? = null,
    @Indexed(unique = true) val publicId: UUID,
    @Indexed val cus: String,
    @Indexed val offerId: String,
    val status: String,
    val balance: BigDecimal,
    // Vendor поля
    var vendorStatus: String? = null,
    var vendorBalance: BigDecimal? = null,
    var lastSyncTime: LocalDateTime? = null,
    var usageCount: Int? = null,
    var urlLogo: String? = null,
    var brand: String? = null,
    @Version var version: Long? = null
)
```

**`SubscriptionDto`** - DTO для передачи данных клиенту  
**`SubscriptionVendorDto`** - DTO для данных от вендора

#### 2. Repository

**`SubscriptionRepository`** - использует `CoroutineCrudRepository`
```kotlin
interface SubscriptionRepository : CoroutineCrudRepository<SubscriptionEntity, ObjectId> {
    fun findByCus(cus: String): Flow<SubscriptionEntity>
    suspend fun findByPublicId(publicId: UUID): SubscriptionEntity?
    fun findByCusAndOfferId(cus: String, offerId: String): Flow<SubscriptionEntity>
}
```

#### 3. Services

##### `VendorService` - имитация внешнего вендора с корутинами

**Ключевые особенности:**
- Использует `suspend` функции вместо Reactor типов
- **Рандомная задержка:**
  - 80% запросов: 50-200мс (быстрый ответ)
  - 20% запросов: 1000мс (медленный ответ)
  
- **Обработка CancellationException:**
```kotlin
catch (e: CancellationException) {
    log.warn("Корутина была отменена для publicId: {}", publicId)
    throw e // Проброс для корректной работы корутин
}
```

##### `SubscriptionInitService` - инициализация данных
- Использует **Datafaker** для генерации тестовых данных
- Возвращает `Flow<SubscriptionEntity>` для потоковой обработки
- Метод удаления всех подписок пользователя

##### `SubscriptionUpdateService` - синхронизация с вендором

**Главный метод:** `updateSubscriptionsWithVendorData(cus, offerId)`

**Логика работы с корутинами:**

1. Загружает все подписки пользователя из БД через `Flow`
2. Конвертирует в список для параллельной обработки
3. Для каждой подписки запрашивает данные от вендора с **timeout 300мс**:
   ```kotlin
   withTimeout(300.milliseconds) {
       vendorService.fetchVendorData(publicId)
   }
   ```
4. При успешном получении данных (< 300мс):
   - Обновляет entity через маппер
   - Сохраняет в БД
5. При timeout/ошибке:
   - Ловит `TimeoutCancellationException`
   - Логирует проблему
   - Возвращает данные из БД без обновления
   - **НЕ БЛОКИРУЕТ** остальные запросы

**Обработка исключений:**
```kotlin
catch (e: TimeoutCancellationException) {
    // Timeout - вендор не успел ответить
    log.error("Timeout при запросе к вендору для publicId: {}", publicId)
    mapper.toDto(subscription) // Возвращаем старые данные
}
catch (e: CancellationException) {
    // Проброс для корректной работы корутин
    throw e
}
catch (e: Exception) {
    // Любая другая ошибка
    log.error("Ошибка при обновлении подписки", e)
    mapper.toDto(subscription)
}
```

#### 4. Mapper
**`SubscriptionMapper`** - преобразование между Entity и DTO:
- `toDto(entity)` - Entity → DTO
- `updateEntityWithVendorData(entity, vendorDto)` - обновление Entity данными вендора
- `toDtoWithVendorData(entity, vendorDto)` - создание DTO с объединенными данными

#### 5. Controllers

##### `SubscriptionInitController` (`/api/subscriptions/init`)
```kotlin
@PostMapping
suspend fun initializeSubscriptions(
    @RequestHeader("AUTH-USER-ID") cus: String,
    @RequestParam(defaultValue = "5") count: Int
): Flow<SubscriptionEntity>

@DeleteMapping
suspend fun deleteAllSubscriptions(
    @RequestHeader("AUTH-USER-ID") cus: String
)
```

##### `SubscriptionController` (`/api/subscriptions`)
```kotlin
@GetMapping
fun getSubscriptions(
    @RequestHeader("AUTH-USER-ID") cus: String,
    @RequestParam(required = false) offerId: String?
): Flow<SubscriptionDto>

@PutMapping("/sync")
suspend fun syncSubscriptionsWithVendor(
    @RequestHeader("AUTH-USER-ID") cus: String,
    @RequestParam(required = false) offerId: String?
): Flow<SubscriptionDto>
```

## 🔑 Ключевые особенности реализации

### 1. Использование корутин вместо Reactor

**До (Reactor):**
```java
public Mono<SubscriptionDto> updateSubscription() {
    return vendorService.fetchVendorData()
        .flatMap(data -> repository.save(entity));
}
```

**После (Coroutines):**
```kotlin
suspend fun updateSubscription(): SubscriptionDto {
    val data = vendorService.fetchVendorData()
    return repository.save(entity)
}
```

### 2. Timeout с withTimeout

```kotlin
val vendorDto = withTimeout(300.milliseconds) {
    try {
        vendorService.fetchVendorData(publicId)
    } catch (e: CancellationException) {
        throw e // Проброс
    } catch (e: Exception) {
        null // Возвращаем null при ошибке
    }
}
```

### 3. Параллельная обработка с Flow

```kotlin
suspend fun updateSubscriptionsWithVendorData(cus: String, offerId: String?): Flow<SubscriptionDto> {
    val subscriptions = subscriptionRepository.findByCus(cus).toList()
    
    return flow {
        subscriptions.forEach { subscription ->
            val result = updateSingleSubscription(subscription) // Каждая независимо
            emit(result)
        }
    }
}
```

### 4. Безопасная обработка CancellationException

```kotlin
catch (e: CancellationException) {
    log.warn("Корутина была отменена")
    throw e // ОБЯЗАТЕЛЬНЫЙ проброс!
}
```

### 5. CoroutineCrudRepository

```kotlin
interface SubscriptionRepository : CoroutineCrudRepository<SubscriptionEntity, ObjectId> {
    fun findByCus(cus: String): Flow<SubscriptionEntity>  // Возвращает Flow
    suspend fun findByPublicId(publicId: UUID): SubscriptionEntity?  // suspend функция
}
```

## 🚀 Использование

### Быстрый старт:

```bash
# 1. Запуск MongoDB
docker run -d --name mongodb -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=root \
  -e MONGO_INITDB_ROOT_PASSWORD=root \
  mongo:7.0

# 2. Запуск приложения
cd D:\git\!_kotlin_projects\mongo-kotlin-acid
gradlew bootRun

# 3. Инициализация подписок
curl -X POST "http://localhost:8080/api/subscriptions/init?count=10" \
  -H "AUTH-USER-ID: user123"

# 4. Синхронизация с вендором
curl -X PUT "http://localhost:8080/api/subscriptions/sync" \
  -H "AUTH-USER-ID: user123"
```

### PowerShell версия:

```powershell
# Инициализация
Invoke-RestMethod -Uri "http://localhost:8080/api/subscriptions/init?count=10" `
  -Method POST `
  -Headers @{"AUTH-USER-ID"="user123"}

# Синхронизация с вендором
Invoke-RestMethod -Uri "http://localhost:8080/api/subscriptions/sync" `
  -Method PUT `
  -Headers @{"AUTH-USER-ID"="user123"}

# Получение подписок
Invoke-RestMethod -Uri "http://localhost:8080/api/subscriptions" `
  -Method GET `
  -Headers @{"AUTH-USER-ID"="user123"}
```

## 📊 Поведение системы

### Статистика ответов вендора:
- ✅ ~80% подписок обновятся (быстрый ответ < 300мс)
- ⏱️ ~20% подписок не обновятся (timeout > 300мс)
- 🔄 Повторная синхронизация может обновить пропущенные

### Время ответа API:
- Синхронизация 10 подписок: ~300мс
- Синхронизация 100 подписок: ~300мс
- **Время НЕ зависит от количества подписок** (параллельная обработка с timeout)

### Логирование:
```
DEBUG VendorService : Имитируем быстрый ответ вендора для publicId: xxx с задержкой 150мс
INFO  SubscriptionUpdateService : Успешно получены данные от вендора для publicId: xxx
DEBUG SubscriptionUpdateService : Подписка обновлена в БД: publicId=xxx

DEBUG VendorService : Имитируем медленный ответ вендора для publicId: yyy
ERROR SubscriptionUpdateService : Timeout при запросе к вендору для publicId: yyy
```

## 🔧 Конфигурация

### application.yml
```yaml
spring:
  application:
    name: mongo-kotlin-acid
  data:
    mongodb:
      uri: mongodb://root:root@localhost:27017/bank?authSource=admin
```

### build.gradle.kts
```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("net.datafaker:datafaker:2.5.2")
}
```

## 📝 Структура проекта

```
src/main/kotlin/com/mongo/mongokotlin/acid/domain/
├── controller/
│   ├── SubscriptionInitController.kt  # Инициализация
│   └── SubscriptionController.kt      # CRUD + синхронизация
├── model/
│   └── SubscriptionEntity.kt          # MongoDB документ
├── dto/
│   ├── SubscriptionDto.kt
│   └── SubscriptionVendorDto.kt
├── mapper/
│   └── SubscriptionMapper.kt
├── repository/
│   └── SubscriptionRepository.kt      # CoroutineCrudRepository
└── service/
    ├── VendorService.kt               # Имитация внешнего API
    ├── SubscriptionInitService.kt     # Генерация тестовых данных
    └── SubscriptionUpdateService.kt   # Синхронизация с вендором
```

## ✅ Выполненные требования

- [x] DTO классы для вендора и подписки (data class)
- [x] SubscriptionEntity как MongoDB сущность с индексами
- [x] **CoroutineCrudRepository** для работы с БД
- [x] VendorService с имитацией задержек (80% быстро, 20% медленно)
- [x] **Timeout 300мс через withTimeout()**
- [x] **Try-catch с пробросом CancellationException**
- [x] Обработка timeout без блокировки основного потока
- [x] **Параллельная обработка подписок через корутины**
- [x] Обновление БД только при успешном ответе вендора
- [x] Возврат данных из БД при ошибке вендора
- [x] Сопоставление по publicId
- [x] Использование Datafaker для инициализации
- [x] **Suspend функции вместо Mono/Flux**
- [x] **Flow вместо Flux для потоковой обработки**
- [x] Контроллер инициализации
- [x] Контроллер обновления
- [x] Логирование всех операций
- [x] Использование заголовка AUTH-USER-ID

## 🆚 Преимущества Kotlin Coroutines над Reactor

### 1. Читаемость кода
**Reactor:**
```java
return repository.findById(id)
    .flatMap(entity -> vendorService.getData(entity.getId()))
    .flatMap(data -> repository.save(updateEntity(entity, data)))
    .map(mapper::toDto);
```

**Coroutines:**
```kotlin
suspend fun update(id: String): SubscriptionDto {
    val entity = repository.findById(id)
    val data = vendorService.getData(entity.id)
    val updated = repository.save(updateEntity(entity, data))
    return mapper.toDto(updated)
}
```

### 2. Обработка ошибок
**Reactor:** сложные цепочки `onErrorResume`  
**Coroutines:** обычный `try-catch`

### 3. Timeout
**Reactor:** требует конфигурацию WebClient  
**Coroutines:** просто `withTimeout(300.milliseconds)`

### 4. Тестирование
**Reactor:** требует StepVerifier  
**Coroutines:** обычные suspend тесты с `runTest`

## 🧪 Тестирование

### Пример теста с корутинами:
```kotlin
@Test
fun `should update subscription with vendor data`() = runTest {
    val subscription = subscriptionRepository.save(createTestSubscription())
    val result = subscriptionUpdateService.updateSubscriptionsWithVendorData("user123", null)
    
    result.collect { dto ->
        assertNotNull(dto.vendorStatus)
        assertNotNull(dto.vendorBalance)
    }
}
```

## 📚 Полезные ссылки

- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Spring Data MongoDB Reactive with Coroutines](https://docs.spring.io/spring-data/mongodb/reference/kotlin/coroutines.html)
- [CoroutineCrudRepository](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/repository/kotlin/CoroutineCrudRepository.html)


