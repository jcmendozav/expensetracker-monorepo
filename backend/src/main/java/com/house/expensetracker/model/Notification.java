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
public class Notification {
    private String id;
    private String householdId;
    private String recipientId;
    private String actorId;
    private String budgetId;
    private String budgetTitle;
    private String invitationId;
    private String type; // "HOUSEHOLD_INVITATION", "BUDGET_CREATED", "BUDGET_DEPLETED", "LOW_BALANCE_WARNING", "EXPENSE_LOGGED", "FUNDS_ADDED"
    private String title;
    private String message;
    private boolean isRead;
    private Timestamp expiresAt; // 90 days TTL
    private Timestamp createdAt;
}

