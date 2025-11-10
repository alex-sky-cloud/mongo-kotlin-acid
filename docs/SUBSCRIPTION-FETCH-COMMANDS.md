файл # Команды для тестирования функции получения подписок (Kotlin + Coroutines)

## 🚀 Запуск приложения

```bash
cd D:\git\!_kotlin_projects\mongo-kotlin-acid
gradlew bootRun
```

> **Примечание:** WireMock запустится автоматически на порту 8090 при старте приложения

---

## ✅ 1. Успешное получение подписок (200 OK)

```bash
curl -H "AUTH-USER-ID: customer-success" http://localhost:8080/api/subscriptions/fetch
```

**Ожидаемый результат:** 3 подписки (ACTIVE, ACTIVE, PENDING)

---

## ❌ 2. Bad Request - 400

```bash
curl -H "AUTH-USER-ID: customer-bad-request" http://localhost:8080/api/subscriptions/fetch
```

**Ожидаемая ошибка:** "Некорректный запрос к внешнему сервису"

---

## 🚫 3. Forbidden - 403

```bash
curl -H "AUTH-USER-ID: customer-forbidden" http://localhost:8080/api/subscriptions/fetch
```

**Ожидаемая ошибка:** "Доступ к подпискам запрещен"

---

## 🔍 4. Not Found - 404

```bash
curl -H "AUTH-USER-ID: customer-not-found" http://localhost:8080/api/subscriptions/fetch
```

**Ожидаемая ошибка:** "Клиент не найден во внешнем сервисе"

---

## ⚠️ 5. Conflict - 409

```bash
curl -H "AUTH-USER-ID: customer-conflict" http://localhost:8080/api/subscriptions/fetch
```

**Ожидаемая ошибка:** "Подписка временно не доступна"

---

## 💥 6. Internal Server Error - 500

```bash
curl -H "AUTH-USER-ID: customer-server-error" http://localhost:8080/api/subscriptions/fetch
```

**Ожидаемая ошибка:** "Внутренняя ошибка внешнего сервиса"

---

## 📝 7. Дефолтный ответ (любой другой ID)

```bash
curl -H "AUTH-USER-ID: my-custom-customer-123" http://localhost:8080/api/subscriptions/fetch
```

**Ожидаемый результат:** 1 дефолтная подписка

---

## ⛔ 8. Отсутствие обязательного заголовка (400)

```bash
curl http://localhost:8080/api/subscriptions/fetch
```

**Ожидаемая ошибка:** "Отсутствует обязательный заголовок: AUTH-USER-ID"

---

## ℹ️ 9. Информация о тестовых сценариях

```bash
curl http://localhost:8080/api/subscriptions/fetch/test-scenarios
```

**Результат:** Текстовое описание всех доступных тестовых сценариев

---

## 🔬 Проверка WireMock

### Проверка работы WireMock напрямую

```bash
# Успешный запрос
curl "http://localhost:8090/api/external/subscriptions?customerId=customer-success"

# Ошибка 404
curl "http://localhost:8090/api/external/subscriptions?customerId=customer-not-found"

# Ошибка 500
curl "http://localhost:8090/api/external/subscriptions?customerId=customer-server-error"
```

---

## 🧪 Bash скрипт для массового тестирования

```bash
#!/bin/bash
# Сохраните в test-all-scenarios.sh

scenarios=(
    "Success (200)|customer-success"
    "Bad Request (400)|customer-bad-request"
    "Forbidden (403)|customer-forbidden"
    "Not Found (404)|customer-not-found"
    "Conflict (409)|customer-conflict"
    "Server Error (500)|customer-server-error"
    "Default|any-customer-id"
)

for scenario in "${scenarios[@]}"; do
    IFS='|' read -r name customer_id <<< "$scenario"
    echo -e "\n=== Testing: $name ==="
    curl -H "AUTH-USER-ID: $customer_id" http://localhost:8080/api/subscriptions/fetch
    echo ""
    sleep 1
done

echo -e "\n=== Testing: No Header (400) ==="
curl http://localhost:8080/api/subscriptions/fetch
echo ""
```

Запуск:
```bash
chmod +x test-all-scenarios.sh
./test-all-scenarios.sh
```

---

## 🎯 Ожидаемые HTTP коды ответов

| Сценарий | Customer ID | HTTP Code | Описание |
|----------|------------|-----------|----------|
| Успех | customer-success | 200 | Список из 3 подписок |
| Bad Request | customer-bad-request | 400 | Некорректный запрос |
| Forbidden | customer-forbidden | 403 | Доступ запрещен |
| Not Found | customer-not-found | 404 | Клиент не найден |
| Conflict | customer-conflict | 409 | Подписка не доступна |
| Server Error | customer-server-error | 500 | Внутренняя ошибка |
| Default | любой другой | 200 | 1 дефолтная подписка |
| No Header | - | 400 | Отсутствует заголовок |

---

**Приложение работает на порту 8080 (Kotlin версия)**  
**WireMock работает на порту 8090**  
**Все команды выполняются в Git Bash**

