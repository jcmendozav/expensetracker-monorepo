package com.house.expensetracker.dto;

import java.util.List;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHouseholdRequest {

    @NotBlank(message = "Household name is required")
    @Size(min = 2, max = 100, message = "Household name must be between 2 and 100 characters")
    private String name;

    @NotEmpty(message = "At least one base currency is required")
    private List<String> baseCurrencies;

    @Email(message = "Invalid partner email format")
    private String partnerEmail;
}

