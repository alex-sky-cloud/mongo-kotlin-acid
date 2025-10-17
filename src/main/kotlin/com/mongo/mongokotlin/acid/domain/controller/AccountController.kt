package com.mongo.mongokotlin.acid.domain.controller

import com.mongo.mongokotlin.acid.domain.service.AccountService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accounts")
class AccountController(
    private val accountService: AccountService
) {

    @PostMapping("/create/{count}")
    suspend fun createAccounts(
        @PathVariable count: Int
    ): String {
        return accountService.createAccounts(count)
    }
}


