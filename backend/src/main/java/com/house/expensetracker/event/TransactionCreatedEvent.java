package com.house.expensetracker.event;

import com.house.expensetracker.dto.TransactionDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedEvent {
    private TransactionDto transaction;
    private String budgetTitle;
    private Long currentBalanceCents;
    private Long initialAmountCents;
    private String budgetStatus;
}

