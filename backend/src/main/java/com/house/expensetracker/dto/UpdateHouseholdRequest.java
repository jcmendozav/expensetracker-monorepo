package com.house.expensetracker.dto;

import java.util.List;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHouseholdRequest {

    @Size(min = 2, max = 100, message = "Household name must be between 2 and 100 characters")
    private String name;

    private List<String> baseCurrencies;

    private String status;
}

