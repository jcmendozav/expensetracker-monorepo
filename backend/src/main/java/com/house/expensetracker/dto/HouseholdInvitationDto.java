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
public class HouseholdInvitationDto {
    private String id;
    private String householdId;
    private String householdName;
    private String inviterId;
    private String inviterName;
    private String invitedEmail;
    private String role; // "CO_OWNER"
    private String status; // "PENDING", "ACCEPTED", "DECLINED", "CANCELLED"
    private Instant createdAt;
    private Instant updatedAt;
}

