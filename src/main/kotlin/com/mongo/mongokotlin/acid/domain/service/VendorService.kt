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
     * Имитация запроса к внешнему вендору с корутинами.
     * В 80% случаев возвращает ответ в пределах 200мс.
     * В 20% случаев добавляет задержку в 1 секунду (имитация медленного ответа).
     */
    suspend fun fetchVendorData(publicId: UUID): SubscriptionVendorDto {
        // Рандомно определяем, будет ли задержка
        val shouldDelay = Random.nextInt(100) < 20 // 20% вероятность задержки
        
        if (shouldDelay) {
            log.debug("Имитируем медленный ответ вендора для publicId: {}", publicId)
            delay(1000) // 1 секунда задержки
        } else {
            // Быстрый ответ (в пределах 200мс)
            val delayMs = Random.nextLong(50, 200)
            log.debug("Имитируем быстрый ответ вендора для publicId: {} с задержкой {}мс", publicId, delayMs)
            delay(delayMs)
        }
        
        return generateVendorData(publicId)
    }

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

    /**
     * Пакетный запрос к вендору с имитацией различных сценариев.
     * 
     * Сценарии (вероятности):
     * - 65% - быстрый успешный ответ (50-200мс)
     * - 20% - медленный ответ (1000мс, вызовет таймаут)
     * - 5%  - возврат дубликата publicId (обрабатывается distinctBy)
     * - 5%  - возврат несуществующего publicId (не обновится)
     * - 5%  - возврат null в обязательном поле (вызовет ошибку БД)
     */
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
        
        val result = publicIds.map { generateVendorData(it) }.toMutableList()
        
        // 5% вероятность: добавляем дубликат publicId (имитация ошибки вендора)
        if (result.isNotEmpty() && Random.nextInt(100) < 5) {
            val duplicate = result.first().copy()
            result.add(duplicate)
            log.warn("⚠️ Имитация: вендор вернул дубликат publicId: {}", duplicate.publicId)
        }
        
        // 5% вероятность: добавляем несуществующий publicId
        if (Random.nextInt(100) < 5) {
            val fakePublicId = UUID.randomUUID()
            result.add(generateVendorData(fakePublicId))
            log.warn("⚠️ Имитация: вендор вернул несуществующий publicId: {}", fakePublicId)
        }
        
        // 5% вероятность: возвращаем CORRUPTED status (имитация некорректных данных от вендора)
        if (result.isNotEmpty() && Random.nextInt(100) < 5) {
            val corruptedData = result.first().copy(vendorStatus = "CORRUPTED")
            result[0] = corruptedData
            log.warn("⚠️ Имитация: вендор вернул CORRUPTED status для publicId: {}", corruptedData.publicId)
        }
        
        return result
    }
}

