package com.mongo.mongokotlin.acid.domain.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id

import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "accounts")
data class AccountEntity(

    @Id
    val id: ObjectId? = null,
    @Indexed(unique = true)
    val name: String,
    val sum: Long
)