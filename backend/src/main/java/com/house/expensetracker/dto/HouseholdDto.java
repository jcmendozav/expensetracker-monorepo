package com.house.expensetracker.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdDto {
    private String id;
    private String name;
    private String status; // "ONBOARDING", "ACTIVE", "FROZEN", "ARCHIVED"
    private List<String> baseCurrencies;
    private Map<String, Long> balancesByCurrency;
    private String ownerId;
    private Map<String, HouseholdMemberDto> members;
    private List<String> memberIds;
    private Instant createdAt;
    private Instant updatedAt;
}

