package com.mongo.mongokotlin.acid

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MongoKotlinAcidApplication

fun main(args: Array<String>) {
    runApplication<MongoKotlinAcidApplication>(*args)
}
