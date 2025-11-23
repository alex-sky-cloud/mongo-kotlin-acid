package com.mongo.mongokotlin.acid.domain.controller

import com.mongo.mongokotlin.acid.domain.dto.TransferResponse
import com.mongo.mongokotlin.acid.domain.service.TransferService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/transfer")
class TransferController(
    private val transferService: TransferService
) {

    @PostMapping("/{fromAccountName}/{toAccountName}/{delay}")
    suspend fun transferMoney(
        @PathVariable fromAccountName: String,
        @PathVariable toAccountName: String,
        @PathVariable delay: Long,
    ): TransferResponse {
        return transferService.transferMoney(fromAccountName, toAccountName, delay)
    }
}



