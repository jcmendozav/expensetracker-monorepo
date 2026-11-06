package com.house.expensetracker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank(message = "Source budget ID is required")
    private String sourceBudgetId;

    @NotBlank(message = "Target budget ID is required")
    private String targetBudgetId;

    @NotNull(message = "Source amount in cents is required")
    @Min(value = 1, message = "Amount must be greater than zero")
    private Long sourceAmountCents;

    @Positive(message = "Exchange rate must be positive")
    private Double exchangeRate;

    private String note;
}

