import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Budget,
  BudgetListResponse,
  CreateBudgetRequest,
  LiquiditySummary,
  UpdateBudgetRequest
} from '../models/budget.model';
import {
  LedgerTransaction,
  LogTransactionRequest,
  TransferRequest,
  TransferResult,
  UpdateTransactionMetadataRequest
} from '../models/ledger.model';
import { environment } from '../../../environments/environments';

@Injectable({
  providedIn: 'root'
})
export class BudgetService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/v1/budgets`;

  createBudget(request: CreateBudgetRequest): Observable<Budget> {
    return this.http.post<Budget>(this.baseUrl, request);
  }

  listBudgets(householdId: string, status?: string): Observable<BudgetListResponse> {
    let params = new HttpParams().set('householdId', householdId);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<BudgetListResponse>(this.baseUrl, { params });
  }

  getSummary(householdId: string): Observable<LiquiditySummary> {
    const params = new HttpParams().set('householdId', householdId);
    return this.http.get<LiquiditySummary>(`${this.baseUrl}/summary`, { params });
  }

  getBudgetById(id: string): Observable<Budget> {
    return this.http.get<Budget>(`${this.baseUrl}/${id}`);
  }

  updateBudget(id: string, request: UpdateBudgetRequest): Observable<Budget> {
    return this.http.patch<Budget>(`${this.baseUrl}/${id}`, request);
  }

  logTransaction(budgetId: string, request: LogTransactionRequest): Observable<LedgerTransaction> {
    return this.http.post<LedgerTransaction>(`${this.baseUrl}/${budgetId}/transactions`, request);
  }

  listTransactions(budgetId: string, limit = 50): Observable<LedgerTransaction[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<LedgerTransaction[]>(`${this.baseUrl}/${budgetId}/transactions`, { params });
  }

  updateTransactionMetadata(
    budgetId: string,
    txId: string,
    request: UpdateTransactionMetadataRequest
  ): Observable<LedgerTransaction> {
    return this.http.patch<LedgerTransaction>(`${this.baseUrl}/${budgetId}/transactions/${txId}`, request);
  }

  voidTransaction(budgetId: string, txId: string): Observable<LedgerTransaction> {
    return this.http.post<LedgerTransaction>(`${this.baseUrl}/${budgetId}/transactions/${txId}/void`, null);
  }

  transferFunds(request: TransferRequest): Observable<TransferResult> {
    return this.http.post<TransferResult>(`${this.baseUrl}/transfers`, request);
  }
}

