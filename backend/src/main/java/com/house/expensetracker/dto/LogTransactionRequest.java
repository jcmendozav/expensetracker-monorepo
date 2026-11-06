package com.house.expensetracker.dto;

import java.time.Instant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogTransactionRequest {

    @NotBlank(message = "Transaction type is required (SPENDING or INCOME)")
    private String type; // "SPENDING", "INCOME"

    @NotNull(message = "Amount in cents is required")
    @Min(value = 1, message = "Amount must be greater than zero")
    private Long amountCents;

    @NotBlank(message = "Category is required")
    private String category;

    private String note;

    private Instant transactionDate;
}

