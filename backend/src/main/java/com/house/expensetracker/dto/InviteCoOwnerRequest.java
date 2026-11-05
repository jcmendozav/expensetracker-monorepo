package com.house.expensetracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteCoOwnerRequest {

    @NotBlank(message = "Invited email is required")
    @Email(message = "Invalid email format")
    private String invitedEmail;
}

