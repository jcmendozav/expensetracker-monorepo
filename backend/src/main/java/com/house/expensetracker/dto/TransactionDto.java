package com.house.expensetracker.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private String id;
    private String householdId;
    private String budgetId;
    private String idempotencyKey;
    private String type; // "SPENDING", "INCOME", "ADJUSTMENT", "TRANSFER_OUT", "TRANSFER_IN"
    private String status; // "COMMITTED", "VOIDED"
    private Long amountCents;
    private Long balanceAfterCents;
    private String category;
    private String note;
    private String linkedBudgetId;
    private String transferGroupId;
    private Double exchangeRate;
    private Instant transactionDate;
    private String createdBy;
    private String createdByName;
    private Instant createdAt;
}

