package com.mongo.mongokotlin.acid.domain.dto

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class SubscriptionVendorDto(
    val publicId: UUID,
    val vendorStatus: String,
    val vendorBalance: BigDecimal,
    val lastSyncTime: LocalDateTime,
    val usageCount: Int,
    val urlLogo: String,
    val brand: String
)