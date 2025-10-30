package com.mongo.mongokotlin.acid.domain.service

import com.mongo.mongokotlin.acid.domain.repository.AccountRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class BalanceService(
    private val accountRepository: AccountRepository
) {

    suspend fun getTotalBalance(): String {
        val allAccounts = accountRepository.findAll().toList()
        val totalSum = allAccounts.sumOf { it.sum }
        
        return "Общая сумма на счетах: $totalSum"
    }
}

