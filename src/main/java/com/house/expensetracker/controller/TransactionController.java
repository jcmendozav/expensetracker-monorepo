package com.house.expensetracker.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody; // Standard Java security interface
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.house.expensetracker.dao.Transaction;
import com.house.expensetracker.dto.TransactionRequest;
import com.house.expensetracker.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Endpoint to create a new expense or income transaction. Accessible only
     * by authenticated users (handled by the Security Filter).
     *
     * @param request The data sent from the Angular frontend.
     * @param principal The security context object holding the verified user
     * information.
     * @return The ID of the newly created transaction.
     */
    @PostMapping
    public ResponseEntity<?> createTransaction(
            @RequestBody TransactionRequest request,
            // Principal holds the authenticated user details (the Firebase UID)
            Principal principal) {

        // 1. Get the verified user ID from the security context
        // In your setup (Task 1.2), the Security Filter will populate this with the Firebase UID
        String enteredByUserId = principal.getName();

        try {
            // 2. Delegate saving and conversion logic to the Service Layer
            // The service will handle conversion, mapping to entity, and Firestore save.
            String documentId = transactionService.saveTransactionFromRequest(request, enteredByUserId);

            // 3. Return a successful response with the new document ID
            //return new ResponseEntity<>("Transaction saved with ID: " + documentId, HttpStatus.CREATED);
            return ResponseEntity.ok(Map.of("message", "Transaction with ID " + documentId + " saved successfully"));
        } catch (ExecutionException | InterruptedException e) {
            // Handle exceptions during Firestore operation
            Thread.currentThread().interrupt();
            return new ResponseEntity<>("Failed to save transaction: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            // Handle business logic exceptions (e.g., currency rate missing)
            return new ResponseEntity<>("Error processing transaction: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Future: GET, PUT, and DELETE mappings will go here.
    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions(Principal principal) {
        try {
            // principal.getName() returns the Firebase UID verified by your Filter
            String userId = principal.getName();
            List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable("id") String id, Principal principal) {
        try {
            transactionService.deleteTransaction(id, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Double>> getDashboardSummary(Principal principal) {
        try {
            Double total = transactionService.getTotalSpent(principal.getName());
            return ResponseEntity.ok(Map.of("totalSpent", total));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(
            @PathVariable("id") String id,
            @RequestBody Transaction transaction,
            Principal principal) {
        try {
            transactionService.updateTransaction(id, transaction, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Transaction updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
