package com.house.expensetracker.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.cloud.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Household {
    private String id;
    private String name;
    private String status; // "ONBOARDING", "ACTIVE", "FROZEN", "ARCHIVED"
    @Builder.Default
    private List<String> baseCurrencies = new ArrayList<>();
    @Builder.Default
    private Map<String, Long> balancesByCurrency = new HashMap<>();
    private String ownerId;
    @Builder.Default
    private Map<String, HouseholdMember> members = new HashMap<>();
    @Builder.Default
    private List<String> memberIds = new ArrayList<>();
    private Timestamp createdAt;
    private Timestamp updatedAt;
}

