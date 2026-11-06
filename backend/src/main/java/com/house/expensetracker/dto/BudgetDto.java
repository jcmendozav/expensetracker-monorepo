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
public class BudgetDto {
    private String id;
    private String householdId;
    private String title;
    private Long initialAmountCents;
    private Long currentBalanceCents;
    private String currency;
    private String status; // "PLANNED", "ACTIVE", "LOW_BALANCE", "DEPLETED", "ARCHIVED"
    private String ownerId;
    private Instant createdAt;
    private Instant updatedAt;
}

