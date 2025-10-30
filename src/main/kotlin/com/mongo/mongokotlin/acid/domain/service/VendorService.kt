package com.mongo.mongokotlin.acid.domain.service

import com.mongo.mongokotlin.acid.domain.dto.SubscriptionVendorDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import net.datafaker.Faker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

@Service
class VendorService {
    
    private val log = LoggerFactory.getLogger(javaClass)
    private val faker = Faker()

    /**
     * Генерирует мок-данные от вендора
     */
    private fun generateVendorData(publicId: UUID): SubscriptionVendorDto {
        val statuses = listOf("ACTIVE", "SUSPENDED", "PENDING", "EXPIRED")
        val logos = listOf(
            "https://logo.clearbit.com/netflix.com",
            "https://logo.clearbit.com/spotify.com",
            "https://logo.clearbit.com/youtube.com",
            "https://logo.clearbit.com/amazon.com"
        )
        
        return SubscriptionVendorDto(
            publicId = publicId,
            vendorStatus = statuses.random(),
            vendorBalance = BigDecimal.valueOf(faker.number().randomDouble(2, 100, 10000)),
            lastSyncTime = LocalDateTime.now(),
            usageCount = faker.number().numberBetween(0, 1000),
            urlLogo = logos.random(),
            brand = faker.company().name()
        )
    }

    suspend fun fetchVendorDataForCus(cus: String, publicIds: Collection<UUID>): List<SubscriptionVendorDto> {
        val shouldDelay = Random.nextInt(100) < 20
        if (shouldDelay) {
            log.debug("Имитируем медленный пакетный ответ вендора для cus: {}", cus)
            delay(1000)
        } else {
            val delayMs = Random.nextLong(50, 200)
            log.debug("Имитируем быстрый пакетный ответ вендора для cus: {} с задержкой {}мс", cus, delayMs)
            delay(delayMs)
        }
        return publicIds.map { generateVendorData(it) }
    }
}
