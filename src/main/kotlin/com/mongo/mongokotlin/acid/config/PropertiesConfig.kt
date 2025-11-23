package com.mongo.mongokotlin.acid.config

import com.mongo.mongokotlin.acid.config.properties.ErrorStrategiesProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Конфигурация для включения @ConfigurationProperties классов
 */
@Configuration
@EnableConfigurationProperties(ErrorStrategiesProperties::class)
class PropertiesConfig




