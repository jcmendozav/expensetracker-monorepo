package com.house.expensetracker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBudgetRequest {

    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @Min(value = 1, message = "Initial amount must be greater than zero")
    private Long initialAmountCents;

    private String status; // "ACTIVE", "ARCHIVED", "PLANNED"
}

