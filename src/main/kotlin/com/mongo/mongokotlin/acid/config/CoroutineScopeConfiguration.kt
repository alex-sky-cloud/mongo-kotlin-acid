package com.mongo.mongokotlin.acid.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineScopeConfiguration {

    @Bean
    fun payCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
}




