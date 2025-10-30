package com.mongo.mongokotlin.acid

import com.mongo.mongokotlin.acid.config.VendorProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(VendorProperties::class)
class MongoKotlinAcidApplication

fun main(args: Array<String>) {
    runApplication<MongoKotlinAcidApplication>(*args)
}
