# Тестовые команды для Subscription API (Kotlin Coroutines)

## Предварительные требования

### 1. Запуск MongoDB

```bash
docker run -d --name mongodb-kotlin -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=root \
  -e MONGO_INITDB_ROOT_PASSWORD=root \
  mongo:7.0
```

### 2. Запуск приложения

```bash
cd D:\git\!_kotlin_projects\mongo-kotlin-acid
gradlew bootRun
```

Приложение запустится на порту **8080** (по умолчанию для Spring Boot).

## 🧪 Тестовые команды (curl)

### 1. Инициализация подписок

Создает 5 подписок:
```bash
curl -X POST "http://localhost:8080/api/subscriptions/init?count=5" \
  -H "AUTH-USER-ID: user123"
```

Создать 20 подписок:
```bash
curl -X POST "http://localhost:8080/api/subscriptions/init?count=20" \
  -H "AUTH-USER-ID: user123"
```

### 2. Получить все подписки (без синхронизации)

```bash
curl -X GET "http://localhost:8080/api/subscriptions" \
  -H "AUTH-USER-ID: user123"
```

Получить с фильтром по offerId:
```bash
curl -X GET "http://localhost:8080/api/subscriptions?offerId=OFFER-PREMIUM" \
  -H "AUTH-USER-ID: user123"
```

### 3. Синхронизация с вендором (главный тест!)

```bash
curl -X PUT "http://localhost:8080/api/subscriptions/sync" \
  -H "AUTH-USER-ID: user123"
```

С фильтром по offerId:
```bash
curl -X PUT "http://localhost:8080/api/subscriptions/sync?offerId=OFFER-PREMIUM" \
  -H "AUTH-USER-ID: user123"
```

**Что происходит:**
- Для каждой подписки запрашиваются данные от вендора с timeout 300мс
- ~80% подписок обновятся (быстрый ответ)
- ~20% подписок вернутся без обновления (timeout)
- Проверьте логи приложения для деталей!

### 4. Удалить все подписки

```bash
curl -X DELETE "http://localhost:8080/api/subscriptions/init" \
  -H "AUTH-USER-ID: user123"
```

## 🪟 PowerShell команды

### Инициализация
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/subscriptions/init?count=10" `
  -Method POST `
  -Headers @{"AUTH-USER-ID"="user123"}
```

### Получение подписок
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/subscriptions" `
  -Method GET `
  -Headers @{"AUTH-USER-ID"="user123"} | ConvertTo-Json
```

### Синхронизация с вендором
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/subscriptions/sync" `
  -Method PUT `
  -Headers @{"AUTH-USER-ID"="user123"} | ConvertTo-Json
```

### Удаление
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/subscriptions/init" `
  -Method DELETE `
  -Headers @{"AUTH-USER-ID"="user123"}
```

## 📋 Сценарии тестирования

### Тест 1: Полный цикл работы

```bash
# Шаг 1: Создать подписки
curl -X POST "http://localhost:8080/api/subscriptions/init?count=5" \
  -H "AUTH-USER-ID: alice"

# Шаг 2: Получить подписки (vendor поля должны быть null)
curl -X GET "http://localhost:8080/api/subscriptions" \
  -H "AUTH-USER-ID: alice" | jq

# Шаг 3: Синхронизировать с вендором
curl -X PUT "http://localhost:8080/api/subscriptions/sync" \
  -H "AUTH-USER-ID: alice" | jq

# Шаг 4: Получить подписки снова (vendor поля заполнены для ~80%)
curl -X GET "http://localhost:8080/api/subscriptions" \
  -H "AUTH-USER-ID: alice" | jq

# Шаг 5: Удалить все подписки
curl -X DELETE "http://localhost:8080/api/subscriptions/init" \
  -H "AUTH-USER-ID: alice"
```

### Тест 2: Множественные синхронизации

```bash
# Создать подписки
curl -X POST "http://localhost:8080/api/subscriptions/init?count=20" \
  -H "AUTH-USER-ID: bob"

# Синхронизация #1
echo "=== Синхронизация 1 ==="
curl -X PUT "http://localhost:8080/api/subscriptions/sync" \
  -H "AUTH-USER-ID: bob"

# Синхронизация #2
echo "=== Синхронизация 2 ==="
curl -X PUT "http://localhost:8080/api/subscriptions/sync" \
  -H "AUTH-USER-ID: bob"

# Синхронизация #3
echo "=== Синхронизация 3 ==="
curl -X PUT "http://localhost:8080/api/subscriptions/sync" \
  -H "AUTH-USER-ID: bob"
```

**Ожидается:**
- С каждой синхронизацией все больше подписок получат vendor данные
- В логах видно, какие запросы завершились успешно, а какие по timeout

### Тест 3: Разные пользователи

```bash
# Пользователь Alice
curl -X POST "http://localhost:8080/api/subscriptions/init?count=3" \
  -H "AUTH-USER-ID: alice"
curl -X PUT "http://localhost:8080/api/subscriptions/sync" \
  -H "AUTH-USER-ID: alice"

# Пользователь Bob
curl -X POST "http://localhost:8080/api/subscriptions/init?count=3" \
  -H "AUTH-USER-ID: bob"
curl -X PUT "http://localhost:8080/api/subscriptions/sync" \
  -H "AUTH-USER-ID: bob"

# Проверить изоляцию данных
echo "=== Подписки Alice ==="
curl -X GET "http://localhost:8080/api/subscriptions" \
  -H "AUTH-USER-ID: alice" | jq '.[] | {cus, offerId}'

echo "=== Подписки Bob ==="
curl -X GET "http://localhost:8080/api/subscriptions" \
  -H "AUTH-USER-ID: bob" | jq '.[] | {cus, offerId}'
```

### Тест 4: Фильтрация по offerId

```bash
# Создать подписки
curl -X POST "http://localhost:8080/api/subscriptions/init?count=15" \
  -H "AUTH-USER-ID: charlie"

# Синхронизировать только OFFER-PREMIUM
curl -X PUT "http://localhost:8080/api/subscriptions/sync?offerId=OFFER-PREMIUM" \
  -H "AUTH-USER-ID: charlie"

# Проверить OFFER-PREMIUM (должны быть обновлены)
curl -X GET "http://localhost:8080/api/subscriptions?offerId=OFFER-PREMIUM" \
  -H "AUTH-USER-ID: charlie" | jq '.[] | {offerId, vendorStatus}'

# Проверить OFFER-BASIC (НЕ должны быть обновлены)
curl -X GET "http://localhost:8080/api/subscriptions?offerId=OFFER-BASIC" \
  -H "AUTH-USER-ID: charlie" | jq '.[] | {offerId, vendorStatus}'
```

## 📊 Что смотреть в логах

После запроса на синхронизацию в логах появятся:

### Успешная синхронизация (быстрый ответ):
```
DEBUG c.m.m.a.d.s.VendorService : Имитируем быстрый ответ вендора для publicId: 550e8400-e29b-41d4-a716-446655440000 с задержкой 127мс
INFO  c.m.m.a.d.s.SubscriptionUpdateService : Успешно получены данные от вендора для publicId: 550e8400-e29b-41d4-a716-446655440000. Обновляем в БД
DEBUG c.m.m.a.d.s.SubscriptionUpdateService : Подписка обновлена в БД: publicId=550e8400-e29b-41d4-a716-446655440000
```

### Timeout (медленный ответ):
```
DEBUG c.m.m.a.d.s.VendorService : Имитируем медленный ответ вендора для publicId: 660e8400-e29b-41d4-a716-446655440001
ERROR c.m.m.a.d.s.SubscriptionUpdateService : Timeout при запросе к вендору для publicId: 660e8400-e29b-41d4-a716-446655440001. Возвращаем данные из БД без обновления
```

### Проброс CancellationException:
```
WARN  c.m.m.a.d.s.VendorService : Корутина была отменена для publicId: 770e8400-e29b-41d4-a716-446655440002
WARN  c.m.m.a.d.s.SubscriptionUpdateService : Корутина отменена для publicId: 770e8400-e29b-41d4-a716-446655440002
```

## 🔍 Проверка данных в MongoDB

```bash
# Подключиться к MongoDB
docker exec -it mongodb-kotlin mongosh -u root -p root --authenticationDatabase admin

# Переключиться на БД
use bank

# Посмотреть все подписки
db.subscriptions.find().pretty()

# Подсчитать подписки С vendor данными
db.subscriptions.countDocuments({vendorStatus: {$ne: null}})

# Подсчитать подписки БЕЗ vendor данных
db.subscriptions.countDocuments({vendorStatus: null})

# Посмотреть только vendor поля
db.subscriptions.find({}, {
    publicId: 1,
    cus: 1,
    offerId: 1,
    vendorStatus: 1,
    vendorBalance: 1,
    lastSyncTime: 1,
    usageCount: 1
}).pretty()

# Найти подписки конкретного пользователя
db.subscriptions.find({cus: "user123"}).pretty()

# Найти подписки с конкретным offerId
db.subscriptions.find({offerId: "OFFER-PREMIUM"}).pretty()
```

## ✅ Ожидаемые результаты

### После инициализации:
```json
{
  "id": "67123abc...",
  "publicId": "550e8400-e29b-41d4-a716-446655440000",
  "cus": "user123",
  "offerId": "OFFER-PREMIUM",
  "status": "ACTIVE",
  "balance": 1234.56,
  "createdAt": "2024-10-15T10:30:00",
  "updatedAt": "2025-10-29T12:00:00",
  "vendorStatus": null,
  "vendorBalance": null,
  "lastSyncTime": null,
  "usageCount": null,
  "urlLogo": null,
  "brand": null
}
```

### После синхронизации (успешной):
```json
{
  "id": "67123abc...",
  "publicId": "550e8400-e29b-41d4-a716-446655440000",
  "cus": "user123",
  "offerId": "OFFER-PREMIUM",
  "status": "ACTIVE",
  "balance": 1234.56,
  "createdAt": "2024-10-15T10:30:00",
  "updatedAt": "2025-10-29T12:05:00",
  "vendorStatus": "ACTIVE",
  "vendorBalance": 5432.10,
  "lastSyncTime": "2025-10-29T12:05:00",
  "usageCount": 156,
  "urlLogo": "https://logo.clearbit.com/netflix.com",
  "brand": "Netflix Inc."
}
```

### После синхронизации (timeout):
```json
{
  // Vendor поля остались null
  "vendorStatus": null,
  "vendorBalance": null,
  "lastSyncTime": null,
  "usageCount": null
}
```

## 🎯 Ключевые моменты для проверки

1. **Параллельность**: Все подписки обрабатываются независимо
2. **Timeout**: Запросы, которые длятся > 300мс, завершаются по timeout
3. **Не блокирует**: API возвращает ответ даже если все вендор запросы упали по timeout
4. **CancellationException**: Корректно пробрасывается и не "заражает" другие корутины
5. **Повторные запросы**: Можно синхронизировать повторно, чтобы обновить пропущенные

## 🐛 Отладка

### Если подписки не создаются:
- Проверьте MongoDB: `docker ps | grep mongodb`
- Проверьте логи приложения
- Проверьте connection string в `application.yml`

### Если все запросы падают по timeout:
- Это нормально! 20% запросов должны падать
- Попробуйте синхронизировать еще раз

### Если vendor поля всегда null:
- Проверьте логи - должны быть сообщения "Имитируем быстрый/медленный ответ"
- Убедитесь, что используете `/sync` endpoint, а не просто `/api/subscriptions`

## 🚀 Производительность

### Бенчмарк:
```bash
# Создать 100 подписок
time curl -X POST "http://localhost:8080/api/subscriptions/init?count=100" \
  -H "AUTH-USER-ID: perf-test"

# Синхронизировать (должно быть ~300-400мс)
time curl -X PUT "http://localhost:8080/api/subscriptions/sync" \
  -H "AUTH-USER-ID: perf-test"
```

**Ожидаемое время синхронизации:**
- 10 подписок: ~300мс
- 50 подписок: ~300мс
- 100 подписок: ~300-400мс

Время не растет линейно благодаря timeout и параллельной обработке!


