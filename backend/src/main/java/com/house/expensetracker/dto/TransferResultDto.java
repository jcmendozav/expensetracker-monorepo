package com.house.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResultDto {
    private String transferGroupId;
    private TransactionDto sourceTransaction;
    private TransactionDto targetTransaction;
    private Long sourceRemainingBalanceCents;
    private Long targetRemainingBalanceCents;
}

