# Задача: Реализация синхронизации подписок с внешним сервисом

<context>
Проект на Kotlin + Spring Boot с реактивной MongoDB.
Стек технологий:
- Kotlin Coroutines
- Spring Boot (реактивный подход)
- MongoDB Reactive (без mongoTemplate)
- CoroutineCrudRepository с derived methods и нативными запросами
- WebClient для внешних HTTP-запросов
</context>

<entity>
Основная сущность:

```kotlin
@Document(collection = "subscriptions")
data class SubscriptionEntity(
@Id
val id: ObjectId? = null,
@Indexed(unique = true)
val publicId: UUID,  // КЛЮЧЕВОЕ ПОЛЕ для идентификации

@Indexed
val cus: String,  // ID клиента

@Indexed
val offerId: String,

val status: String,
val balance: BigDecimal,
val createdAt: LocalDateTime,
var updatedAt: LocalDateTime,

// Поля от вендора
var vendorStatus: String? = null,
var vendorBalance: BigDecimal? = null,
var lastSyncTime: LocalDateTime? = null,
var usageCount: Int? = null,
var urlLogo: String? = null,
var brand: String? = null

```

```kotlin
</entity>

<existing_components>
1. Repository:
@Repository
interface SubscriptionRepository : CoroutineCrudRepository<SubscriptionEntity, ObjectId> {
// Используй derived methods или @Query аннотации
}



```

2. Внешний клиент (уже реализован):
```kotlin
@Component
class ExternalSubscriptionClient(
private val webClientBuilder: WebClient.Builder
) {
suspend fun fetchSubscriptions(customerId: String): SubscriptionListResponseDto {
val fullUrl = "$externalServiceUrl/api/external/subscriptions?customerId=$customerId"
// Возвращает список подписок с статусами: 'active', 'trial', 'cancelled', 'terminated'
}
}
```

3. Сервис для реализации:
```kotlin
@Service
class SubscriptionFetchService(
private val externalClient: ExternalSubscriptionClient,
private val errorStrategyMap: Map<Int, ErrorHandlingStrategy>
) {
// Здесь реализовать логику синхронизации
}
```
</existing_components>

<task>
Создай **3-4 различных реализации** синхронизации подписок, каждая в отдельном endpoint:

**Бизнес-логика синхронизации:**
1. Получить данные от внешнего сервиса для конкретного клиента (по customerId/cus)
2. Загрузить существующие подписки из MongoDB по полю `cus`
3. Сравнить данные по уникальному полю `publicId`
4. Обновить существующие записи (те, где `publicId` совпадает)
5. Создать новые записи для подписок, которых нет в базе
6. Обеспечить корректное заполнение полей `updatedAt` и `lastSyncTime`

**Требования к реализациям:**
- Каждая реализация должна демонстрировать **разный подход** к работе с данными
- Использовать только `CoroutineCrudRepository` (без mongoTemplate)
- Применять derived methods или @Query с нативными запросами
- Все операции должны быть suspend функциями
- Использовать Kotlin Flow где уместно
- Контроллер для каждой реализации с отдельным endpoint
</task>

<implementation_approaches>
Предложи разные подходы, например:
1. **Подход с batch операциями** (saveAll)
2. **Подход с Flow и реактивными операторами**
3. **Подход с custom repository methods** и derived queries
4. **Подход с транзакционной обработкой** (если применимо)
</implementation_approaches>

<deliverables>
Для каждой реализации создай:

1. **Контроллер** с endpoint
2. **Метод в сервисе** с полной бизнес-логикой
3. **Необходимые методы в Repository** (derived или с @Query)
4. **Markdown документацию** (файл `APPROACH_N.md`) со следующей структурой:

## Структура Markdown файла:

Подход N: [Название подхода]
Содержание
Описание

Архитектура

Реализация

Запросы к БД

Особенности Kotlin

Преимущества и недостатки

Архитектура

@startuml
!theme plain
[Диаграмма последовательности или компонентов]
@enduml


Реализация
Контроллер
kotlin
[Полный код с комментариями]
Сервис
kotlin
[Полный код с построчными комментариями]
Repository
kotlin
[Методы с пояснениями синтаксиса derived methods или @Query]
Запросы к БД
Метод 1: [название]
Derived Method / @Query:

kotlin
[код метода]
Что происходит:

[Пояснение работы метода]

[Какой MongoDB запрос генерируется]

[Особенности индексов]

Пример MongoDB запроса:

json
{ "cus": "customer123" }
Метод 2: [название]
[аналогично]

Особенности Kotlin
1. Suspend функции
[Объяснение зачем нужны suspend, как работают]

2. Kotlin Flow
[Если используется - объяснение операторов]

3. [Другие особенности]
[Scope functions, Elvis operator, и т.д.]

Преимущества и недостатки
Преимущества:

[список]

Недостатки:

[список]

Когда использовать:

[рекомендации]

text
</deliverables>

<output_format>
Предоставь:
1. Полный код для каждой реализации (Controller, Service, Repository)
2. Отдельный .md файл для каждого подхода с детальными пояснениями
3. PlantUML диаграммы встроенные в markdown (не отдельными файлами)
4. Акцент на объяснение:
   - Синтаксиса Kotlin (suspend, Flow, coroutines)
   - Derived methods и их преобразование в MongoDB запросы
   - Нативных @Query аннотаций
   - Реактивных паттернов
</output_format>

<constraints>
- Использовать ТОЛЬКО CoroutineCrudRepository
- НЕ использовать mongoTemplate
- Все методы должны быть suspend функциями
- Идентификация подписок СТРОГО по полю `publicId` (уникальный UUID)
- Поиск подписок клиента по полю `cus`
- Статусы подписок: 'active', 'trial', 'cancelled', 'terminated'
</constraints>

<examples>
Пример derived method для поиска по cus:
suspend fun findByCus(cus: String): Flow<SubscriptionEntity>

text

Пример нативного запроса:
@Query("{ 'publicId': ?0 }")
suspend fun findByPublicId(publicId: UUID): SubscriptionEntity?

text
</examples>

Создай максимально подробные, понятные реализации с акцентом на обучение и best practices для Kotlin + Spring Boot Reactive + MongoDB.
Пояснения к промпту
Этот промпт структурирован по CARE framework (Context, Ask, Rules, Examples), который рекомендован для Claude:​

XML-теги <context>, <task>, <constraints> помогают Claude четко разделить разные части инструкции​

Конкретные примеры кода показывают желаемый формат​

Пошаговая структура для документации облегчает выполнение задачи​

Позитивные формулировки (что делать) вместо негативных (чего не делать)​

Четкие deliverables с шаблоном markdown файла​

Промпт готов для использо