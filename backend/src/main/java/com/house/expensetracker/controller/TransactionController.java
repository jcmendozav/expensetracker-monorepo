package com.house.expensetracker.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.house.expensetracker.dto.LogTransactionRequest;
import com.house.expensetracker.dto.TransactionDto;
import com.house.expensetracker.dto.TransferRequest;
import com.house.expensetracker.dto.TransferResultDto;
import com.house.expensetracker.dto.UpdateTransactionMetadataRequest;
import com.house.expensetracker.security.AuthenticatedUser;
import com.house.expensetracker.service.TransactionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/budgets")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/{id}/transactions")
    public ResponseEntity<TransactionDto> logTransaction(
            @PathVariable("id") String budgetId,
            @Valid @RequestBody LogTransactionRequest request,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        TransactionDto committed = transactionService.commitTransaction(
                budgetId,
                request,
                caller.uid(),
                caller.displayName());
        return ResponseEntity.status(HttpStatus.CREATED).body(committed);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionDto>> listTransactions(
            @PathVariable("id") String budgetId,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        List<TransactionDto> items = transactionService.listTransactions(budgetId, limit, caller.uid());
        return ResponseEntity.ok(items);
    }

    @PatchMapping("/{id}/transactions/{txId}")
    public ResponseEntity<TransactionDto> updateTransactionMetadata(
            @PathVariable("id") String budgetId,
            @PathVariable("txId") String txId,
            @RequestBody UpdateTransactionMetadataRequest request,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        TransactionDto updated = transactionService.updateMetadata(budgetId, txId, request, caller.uid());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/transactions/{txId}/void")
    public ResponseEntity<TransactionDto> voidTransaction(
            @PathVariable("id") String budgetId,
            @PathVariable("txId") String txId,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        TransactionDto voided = transactionService.voidTransaction(budgetId, txId, caller.uid());
        return ResponseEntity.ok(voided);
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResultDto> transferFunds(
            @Valid @RequestBody TransferRequest request,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        TransferResultDto result = transactionService.transferFunds(
                request,
                caller.uid(),
                caller.displayName());
        return ResponseEntity.ok(result);
    }
}
