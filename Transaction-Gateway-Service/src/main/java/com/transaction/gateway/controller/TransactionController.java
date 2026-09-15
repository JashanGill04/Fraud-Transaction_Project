package com.transaction.gateway.controller;

import com.transaction.gateway.dto.TransactionRequest;
import com.transaction.gateway.dto.TransactionResponse;
import com.transaction.gateway.entity.Transaction;
import com.transaction.gateway.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request) {

        TransactionResponse response = transactionService.processTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping
    public ResponseEntity<List<Transaction>> getTransaction(){
        List<Transaction> list = transactionService.getTransactions();
        return ResponseEntity.status(HttpStatus.OK).body(list);

    }

}