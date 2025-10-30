package com.mongo.mongokotlin.acid.domain.dto

class TransferResponse(
    val message: String,
    val fromAccountBalance: Long,
    val toAccountBalance: Long
)

