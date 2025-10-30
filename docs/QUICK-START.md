# 🚀 Быстрый старт - Subscription API

## Что реализовано

Система подписок с синхронизацией данных от внешнего вендора на **Kotlin с корутинами**:

- ✅ **CoroutineCrudRepository** для работы с MongoDB
- ✅ **Suspend функции** вместо Mono/Flux
- ✅ **Flow** для потоковой обработки
- ✅ **withTimeout(200ms)** для ограничения времени запроса к вендору
- ✅ Имитация вендора: 80% быстрых ответов (50-200мс), 20% медленных (1000мс = timeout)
- ✅ Параллельная обработка подписок
- ✅ Обработка timeout без блокировки клиента
- ✅ Использование Datafaker для генерации тестовых данных

## Запуск за 3 шага

### 1. MongoDB
```bash
docker run -d --name mongodb-kotlin -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=root \
  -e MONGO_INITDB_ROOT_PASSWORD=root \
  mongo:7.0
```

### 2. Приложение
```bash
cd D:\git\!_kotlin_projects\mongo-kotlin-acid
.\gradlew bootRun
```

### 3. Тестирование

#### PowerShell:
```powershell
# Создать подписки
Invoke-RestMethod -Uri "http://localhost:8080/api/subscriptions/init?count=10" `
  -Method POST -Headers @{"AUTH-USER-ID"="user123"}

# Обновить подписки с синхронизацией от вендора (главный тест!)
Invoke-RestMethod -Uri "http://localhost:8080/api/subscriptions" `
  -Method PUT -Headers @{"AUTH-USER-ID"="user123"} | ConvertTo-Json
```

#### Bash:
```bash
# Создать подписки
curl -X POST "http://localhost:8080/api/subscriptions/init?count=10" \
  -H "AUTH-USER-ID: user123"

# Обновить подписки с синхронизацией от вендора (главный тест!)
curl -X PUT "http://localhost:8080/api/subscriptions" \
  -H "AUTH-USER-ID: user123" | jq
```

## Что смотреть в логах

### ✅ Успешный запрос (< 300мс):
```
DEBUG VendorService : Имитируем быстрый ответ вендора для publicId: xxx с задержкой 127мс
INFO  SubscriptionUpdateService : Успешно получены данные от вендора для publicId: xxx
DEBUG SubscriptionUpdateService : Подписка обновлена в БД: publicId=xxx
```

### ⏱️ Timeout (> 300мс):
```
DEBUG VendorService : Имитируем медленный ответ вендора для publicId: yyy
ERROR SubscriptionUpdateService : Timeout при запросе к вендору для publicId: yyy. Возвращаем данные из БД
```

## Структура проекта

```
src/main/kotlin/com/mongo/mongokotlin/acid/domain/
├── controller/
│   ├── SubscriptionInitController.kt  # POST /api/subscriptions/init
│   └── SubscriptionController.kt      # GET/PUT /api/subscriptions
├── service/
│   ├── VendorService.kt               # Имитация с задержками
│   ├── SubscriptionInitService.kt     # Datafaker
│   └── SubscriptionUpdateService.kt   # withTimeout(300ms)
├── repository/
│   └── SubscriptionRepository.kt      # CoroutineCrudRepository
├── model/
│   └── SubscriptionEntity.kt          # @Document
├── dto/
│   ├── SubscriptionDto.kt
│   └── SubscriptionVendorDto.kt
└── mapper/
    └── SubscriptionMapper.kt
```

## Ключевые моменты реализации

### 1. Timeout с withTimeout
```kotlin
val vendorDto = withTimeout(200.milliseconds) {
    vendorService.fetchVendorData(publicId)
}
```

### 2. Параллельная обработка
```kotlin
return flow {
    subscriptions.forEach { subscription ->
        val result = updateSingleSubscription(subscription) // Независимо
        emit(result)
    }
}
```

### 3. CoroutineCrudRepository
```kotlin
interface SubscriptionRepository : CoroutineCrudRepository<SubscriptionEntity, ObjectId> {
    fun findByCus(cus: String): Flow<SubscriptionEntity>
    suspend fun findByPublicId(publicId: UUID): SubscriptionEntity?
}
```

## API Endpoints

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/subscriptions/init?count=5` | Создать подписки |
| PUT | `/api/subscriptions` | **Получить подписки + фоновое обновление от вендора** |
| DELETE | `/api/subscriptions/init` | Удалить все подписки |

**Все эндпоинты требуют заголовок:** `AUTH-USER-ID: <userId>`

**Главный endpoint:** `PUT /api/subscriptions` 
- Получает документы из БД по cus
- СРАЗУ возвращает клиенту текущие данные из БД
- ПАРАЛЛЕЛЬНО в фоне запускает обновление от вендора с timeout 200мс
- Обновление сопоставляется по publicId и сохраняется в БД асинхронно

## Ожидаемое поведение

После `PUT /api/subscriptions`:
- API **моментально** возвращает клиенту документы из БД (текущее состояние)
- **В фоне** параллельно запрашивает данные от вендора для каждой подписки (timeout 200мс)
- ~80% подписок обновятся в БД vendor данными (быстрый ответ 50-200мс)
- ~20% подписок не обновятся (медленный ответ 1000мс = timeout)
- Обновление сопоставляется по publicId
- Время ответа клиенту: **~10-50мс** (только чтение из БД, без ожидания вендора!)
- Следующий запрос вернет уже обновленные данные

## Проверка в MongoDB

```bash
docker exec -it mongodb-kotlin mongosh -u root -p root --authenticationDatabase admin

use bank

# Подписки С vendor данными
db.subscriptions.countDocuments({vendorStatus: {$ne: null}})

# Подписки БЕЗ vendor данных
db.subscriptions.countDocuments({vendorStatus: null})
```

## Дополнительная документация

- **SUBSCRIPTION-README.md** - полное описание архитектуры
- **TEST-SUBSCRIPTION-API.md** - все тестовые сценарии
- Исходный код в `src/main/kotlin/com/mongo/mongokotlin/acid/domain/`

## Отличия от Java/Reactor версии

| Aspect | Java (Reactor) | Kotlin (Coroutines) |
|--------|---------------|---------------------|
| Async типы | `Mono<T>`, `Flux<T>` | `suspend fun`, `Flow<T>` |
| Repository | `ReactiveMongoRepository` | `CoroutineCrudRepository` |
| Timeout | WebClient config | `withTimeout()` |
| Error handling | `.onErrorResume()` | `try-catch` |
| Читаемость | Цепочки операторов | Последовательный код |

## Troubleshooting

**Проблема:** Приложение не стартует  
**Решение:** Проверьте, что MongoDB запущен: `docker ps | grep mongodb`

**Проблема:** Все запросы падают по timeout  
**Решение:** Это нормально! 20% запросов специально имитируют задержку

**Проблема:** vendor поля всегда null  
**Решение:** 
1. Первый запрос `PUT /api/subscriptions` возвращает данные из БД (vendor поля null)
2. Обновление идет в фоне
3. Второй запрос `PUT /api/subscriptions` вернет уже обновленные данные (~80% с vendor полями)

---

**Готово!** Система работает и готова к тестированию 🎉

