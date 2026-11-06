package com.house.expensetracker.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiquiditySummaryResponse {
    private Map<String, Long> balancesByCurrency;
    private int activeCount;
}

