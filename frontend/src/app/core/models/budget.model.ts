export interface Budget {
  id: string;
  householdId: string;
  title: string;
  initialAmountCents: number;
  currentBalanceCents: number;
  currency: string;
  status: 'PLANNED' | 'ACTIVE' | 'LOW_BALANCE' | 'DEPLETED' | 'ARCHIVED';
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBudgetRequest {
  householdId: string;
  title: string;
  initialAmountCents: number;
  currency: string;
}

export interface UpdateBudgetRequest {
  title?: string;
  initialAmountCents?: number;
  status?: string;
}

export interface BudgetListResponse {
  items: Budget[];
  nextCursor?: string;
}

export interface LiquiditySummary {
  balancesByCurrency: Record<string, number>;
  activeCount: number;
}

