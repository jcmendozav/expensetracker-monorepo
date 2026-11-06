package com.house.expensetracker.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetLowBalanceEvent {
    private String householdId;
    private String budgetId;
    private String budgetTitle;
    private Long currentBalanceCents;
    private Long initialAmountCents;
    private String actorId;
}

