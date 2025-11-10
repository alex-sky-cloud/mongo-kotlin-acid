package com.mongo.mongokotlin.acid.config

import org.springframework.boot.context.properties.ConfigurationProperties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@ConfigurationProperties(prefix = "vendor")
data class VendorProperties(
    val timeoutMs: Long = 200
) {
    val timeout: Duration
        get() = timeoutMs.milliseconds
}



