package com.mongo.mongokotlin.acid.domain.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Document(collection = "subscriptions")
data class SubscriptionEntity(

    @Id
    val id: ObjectId? = null,

    @Indexed(unique = true)
    val publicId: UUID,

    @Indexed
    val cus: String,

    @Indexed
    val offerId: String,

    val status: String,
    val balance: BigDecimal,
    val createdAt: LocalDateTime,
    var updatedAt: LocalDateTime,

    // Vendor related fields
    var vendorStatus: String? = null,
    var vendorBalance: BigDecimal? = null,
    var lastSyncTime: LocalDateTime? = null,
    var usageCount: Int? = null,
    var urlLogo: String? = null,
    var brand: String? = null
)