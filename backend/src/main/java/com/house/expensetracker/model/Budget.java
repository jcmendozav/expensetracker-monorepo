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
public class Budget {
    private String id;
    private String householdId;
    private String title;
    private Long initialAmountCents;
    private Long currentBalanceCents;
    private String currency;
    private String status; // "PLANNED", "ACTIVE", "LOW_BALANCE", "DEPLETED", "ARCHIVED"
    private String ownerId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public static String deriveStatus(long currentBalanceCents, long initialAmountCents, String currentStatus) {
        if ("ARCHIVED".equalsIgnoreCase(currentStatus)) {
            return "ARCHIVED";
        }
        if ("PLANNED".equalsIgnoreCase(currentStatus) && currentBalanceCents == 0) {
            return "PLANNED";
        }
        if (currentBalanceCents <= 0) {
            return "DEPLETED";
        }
        // Balance drops below 10%
        if (initialAmountCents > 0 && (currentBalanceCents * 100 < initialAmountCents * 10)) {
            return "LOW_BALANCE";
        }
        return "ACTIVE";
    }

    public static boolean isActiveStatus(String status) {
        return "ACTIVE".equalsIgnoreCase(status) || "LOW_BALANCE".equalsIgnoreCase(status);
    }
}

