export interface LedgerTransaction {
  id: string;
  householdId: string;
  budgetId: string;
  idempotencyKey?: string;
  type: 'SPENDING' | 'INCOME' | 'ADJUSTMENT' | 'TRANSFER_OUT' | 'TRANSFER_IN';
  status: 'COMMITTED' | 'VOIDED';
  amountCents: number;
  balanceAfterCents: number;
  category: string;
  note?: string;
  linkedBudgetId?: string;
  transferGroupId?: string;
  exchangeRate?: number;
  transactionDate: string;
  createdBy: string;
  createdByName: string;
  createdAt: string;
}

export interface LogTransactionRequest {
  type: 'SPENDING' | 'INCOME';
  amountCents: number;
  category: string;
  note?: string;
  transactionDate?: string;
}

export interface UpdateTransactionMetadataRequest {
  category?: string;
  note?: string;
}

export interface TransferRequest {
  sourceBudgetId: string;
  targetBudgetId: string;
  sourceAmountCents: number;
  exchangeRate?: number;
  note?: string;
}

export interface TransferResult {
  transferGroupId: string;
  sourceTransaction: LedgerTransaction;
  targetTransaction: LedgerTransaction;
  sourceRemainingBalanceCents: number;
  targetRemainingBalanceCents: number;
}

