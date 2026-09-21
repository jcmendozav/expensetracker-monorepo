export interface Transaction {
  id?: string; // Optional because new transactions don't have an ID yet
  householdId: number;
  categoryId: number;
  originalAmount: number;
  originalCurrency: string;
  transactionDate: number;
  description: string;
  userId?: string; // Populated by the backend
}