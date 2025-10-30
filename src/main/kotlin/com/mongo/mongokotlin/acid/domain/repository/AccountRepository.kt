package com.mongo.mongokotlin.acid.domain.repository

import com.mongo.mongokotlin.acid.domain.model.AccountEntity
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository : CoroutineCrudRepository<AccountEntity, ObjectId> {
    
    suspend fun findByName(name: String): AccountEntity?
}


