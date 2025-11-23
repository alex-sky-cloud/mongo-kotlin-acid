package com.mongo.mongokotlin.acid.domain.repository

import com.mongo.mongokotlin.acid.domain.model.SubscriptionEntity
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SubscriptionRepository : CoroutineCrudRepository<SubscriptionEntity, ObjectId> {
    
    /**
     * Derived method: поиск всех подписок клиента по полю cus
     * Возвращает Flow для реактивной обработки
     */
    fun findByCus(cus: String): Flow<SubscriptionEntity>
    
    /**
     * Derived method: поиск подписки по уникальному publicId
     * Возвращает nullable, так как подписка может не существовать
     */
    suspend fun findByPublicId(publicId: UUID): SubscriptionEntity?
    
    /**
     * Нативный запрос: поиск всех подписок по списку publicId
     * Используется для batch операций
     */
    @org.springframework.data.mongodb.repository.Query("{ 'publicId': { \$in: ?0 } }")
    suspend fun findByPublicIdIn(publicIds: List<UUID>): List<SubscriptionEntity>
    
    /**
     * Нативный запрос: поиск подписок клиента по cus и списку publicId
     * Используется для оптимизированного поиска существующих подписок
     */
    @org.springframework.data.mongodb.repository.Query("{ 'cus': ?0, 'publicId': { \$in: ?1 } }")
    suspend fun findByCusAndPublicIdIn(cus: String, publicIds: List<UUID>): List<SubscriptionEntity>
}

