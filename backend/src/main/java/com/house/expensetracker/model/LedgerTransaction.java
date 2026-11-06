package com.house.expensetracker.model;

import com.google.cloud.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerTransaction {
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
    private Timestamp transactionDate;
    private String createdBy;
    private String createdByName;
    private Timestamp createdAt;
}

