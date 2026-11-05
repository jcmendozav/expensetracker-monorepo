package com.house.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdMemberDto {
    private String userId;
    private String role; // "PRIMARY_OWNER", "CO_OWNER"
    private String displayName;
    private String email;
    private String status; // "ACTIVE", "SUSPENDED", "REMOVED"
}

