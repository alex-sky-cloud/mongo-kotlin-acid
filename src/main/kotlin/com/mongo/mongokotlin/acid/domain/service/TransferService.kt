package com.mongo.mongokotlin.acid.domain.service

import com.mongo.mongokotlin.acid.domain.dto.TransferResponse
import com.mongo.mongokotlin.acid.domain.model.AccountEntity
import com.mongo.mongokotlin.acid.domain.repository.AccountRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

private const val TRANSFER_AMOUNT = 100L
private const val INSUFFICIENT_FUNDS_MESSAGE = "Клиент с именем %s не имеет достаточно средств"

@Service
class TransferService(
    private val accountRepository: AccountRepository,
    private val mongoTemplate: ReactiveMongoTemplate
) {

    @Transactional
    suspend fun transferMoney(fromAccountName: String, toAccountName: String, delay: Long): TransferResponse {
        val fromAccount = findAccountByName(fromAccountName)
        val toAccount = findAccountByName(toAccountName)

        delay(delay)

        validateSufficientFunds(fromAccount, fromAccountName)
        
        val updatedFromAccount = debitAccount(fromAccount, TRANSFER_AMOUNT)
        val updatedToAccount = creditAccount(toAccount, TRANSFER_AMOUNT)
        
        saveAccount(updatedFromAccount)
        saveAccount(updatedToAccount)
        
        return createTransferResponse(updatedFromAccount, updatedToAccount)
    }

    private suspend fun findAccountByName(name: String): AccountEntity {
        return accountRepository.findByName(name) 
            ?: throw RuntimeException("Аккаунт с именем $name не найден")
    }

    private fun validateSufficientFunds(account: AccountEntity, accountName: String) {
        if (account.sum < TRANSFER_AMOUNT) {
            throw RuntimeException(INSUFFICIENT_FUNDS_MESSAGE.format(accountName))
        }
    }

    private fun debitAccount(account: AccountEntity, amount: Long): AccountEntity {
        return account.copy(sum = account.sum - amount)
    }

    private fun creditAccount(account: AccountEntity, amount: Long): AccountEntity {
        return account.copy(sum = account.sum + amount)
    }

    private suspend fun saveAccount(account: AccountEntity) {
        accountRepository.save(account)
    }

    private fun createTransferResponse(fromAccount: AccountEntity, toAccount: AccountEntity): TransferResponse {
        return TransferResponse(
            message = "Перевод выполнен успешно",
            fromAccountBalance = fromAccount.sum,
            toAccountBalance = toAccount.sum
        )
    }
}

