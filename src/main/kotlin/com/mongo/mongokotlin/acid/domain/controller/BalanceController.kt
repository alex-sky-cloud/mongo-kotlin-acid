package com.mongo.mongokotlin.acid.domain.controller

import com.mongo.mongokotlin.acid.domain.service.BalanceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/balance")
class BalanceController(
    private val balanceService: BalanceService
) {

    @GetMapping("/total")
    suspend fun getTotalBalance(): String {
        return balanceService.getTotalBalance()
    }
}



