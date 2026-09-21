package com.house.expensetracker.dto;

import lombok.Data;

@Data
public class TransactionRequest {

    // Basic fields from the frontend form
    private Integer householdId;
    private Integer categoryId;
    private Double originalAmount; // Received as a double from Angular
    private String originalCurrency;
    private Long transactionDate;
    private String description;
}
