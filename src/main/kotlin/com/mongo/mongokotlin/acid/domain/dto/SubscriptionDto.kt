package com.mongo.mongokotlin.acid.domain.dto

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class SubscriptionDto(
    val id: String?,
    val publicId: UUID,
    val cus: String,
    val offerId: String,
    val status: String,
    val balance: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val vendorStatus: String?,
    val vendorBalance: BigDecimal?,
    val lastSyncTime: LocalDateTime?,
    val usageCount: Int?,
    val urlLogo: String?,
    val brand: String?
)