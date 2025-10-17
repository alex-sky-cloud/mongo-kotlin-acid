package com.mongo.mongokotlin.acid.domain.service

import com.mongo.mongokotlin.acid.domain.dto.AccountDto
import com.mongo.mongokotlin.acid.domain.model.AccountEntity
import com.mongo.mongokotlin.acid.domain.repository.AccountRepository
import kotlinx.coroutines.flow.toList
import net.datafaker.Faker
import org.springframework.stereotype.Service
import java.util.*

private const val MIN_ACCOUNT_SUM = 1000L
private const val MAX_ACCOUNT_SUM = 1000000L
private const val START_INDEX = 1

@Service
class AccountService(
    private val accountRepository: AccountRepository
) {

    private val faker = Faker(Locale("ru"))

    suspend fun createAccounts(count: Int): List<AccountDto> {
        val accounts = (START_INDEX..count).map {
            AccountEntity(
                name = faker.name().fullName(),
                sum = faker.number().numberBetween(MIN_ACCOUNT_SUM, MAX_ACCOUNT_SUM)
            )
        }

        val savedAccounts = accountRepository.saveAll(accounts).toList()

        return savedAccounts.map { entity ->
            AccountDto(
                name = entity.name,
                sum = entity.sum
            )
        }
    }
}


