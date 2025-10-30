package com.mongo.mongokotlin.acid.domain.repository

import com.mongo.mongokotlin.acid.domain.model.SubscriptionEntity
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SubscriptionRepository : CoroutineCrudRepository<SubscriptionEntity, ObjectId> {
    
    fun findByCus(cus: String): Flow<SubscriptionEntity>
}

