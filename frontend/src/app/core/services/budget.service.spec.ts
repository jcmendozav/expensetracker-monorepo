import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BudgetService } from './budget.service';
import type { Budget, CreateBudgetRequest, LiquiditySummary } from '../models/budget.model';
import type { LogTransactionRequest, TransferRequest } from '../models/ledger.model';
import { environment } from '../../../environments/environments';

describe('BudgetService', () => {
  let service: BudgetService;
  let httpTesting: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/v1/budgets`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BudgetService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(BudgetService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('createBudget should send POST request with payload', () => {
    const request: CreateBudgetRequest = {
      householdId: 'hh_1',
      title: 'Groceries',
      initialAmountCents: 100000,
      currency: 'PEN'
    };

    const mockResponse: Budget = {
      id: 'bdg_1',
      householdId: 'hh_1',
      title: 'Groceries',
      initialAmountCents: 100000,
      currentBalanceCents: 100000,
      currency: 'PEN',
      status: 'ACTIVE',
      ownerId: 'usr_1',
      createdAt: '2026-11-06T00:00:00Z',
      updatedAt: '2026-11-06T00:00:00Z'
    };

    service.createBudget(request).subscribe(res => {
      expect(res.id).toBe('bdg_1');
      expect(res.initialAmountCents).toBe(100000);
    });

    const req = httpTesting.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('getSummary should send GET request to /summary with householdId param', () => {
    const mockSummary: LiquiditySummary = {
      balancesByCurrency: { PEN: 50000 },
      activeCount: 1
    };

    service.getSummary('hh_1').subscribe(summary => {
      expect(summary.activeCount).toBe(1);
      expect(summary.balancesByCurrency['PEN']).toBe(50000);
    });

    const req = httpTesting.expectOne(`${baseUrl}/summary?householdId=hh_1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);
  });

  it('logTransaction should send POST request to /{budgetId}/transactions', () => {
    const payload: LogTransactionRequest = {
      type: 'SPENDING',
      amountCents: 4500,
      category: 'Food'
    };

    service.logTransaction('bdg_1', payload).subscribe(tx => {
      expect(tx.id).toBe('tx_1');
      expect(tx.amountCents).toBe(4500);
    });

    const req = httpTesting.expectOne(`${baseUrl}/bdg_1/transactions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 'tx_1', amountCents: 4500 });
  });

  it('voidTransaction should send POST request to /{budgetId}/transactions/{txId}/void', () => {
    service.voidTransaction('bdg_1', 'tx_1').subscribe(tx => {
      expect(tx.status).toBe('VOIDED');
    });

    const req = httpTesting.expectOne(`${baseUrl}/bdg_1/transactions/tx_1/void`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 'tx_1', status: 'VOIDED' });
  });

  it('transferFunds should send POST request to /transfers', () => {
    const transferReq: TransferRequest = {
      sourceBudgetId: 'b1',
      targetBudgetId: 'b2',
      sourceAmountCents: 10000
    };

    service.transferFunds(transferReq).subscribe(res => {
      expect(res.transferGroupId).toBe('grp_1');
    });

    const req = httpTesting.expectOne(`${baseUrl}/transfers`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(transferReq);
    req.flush({ transferGroupId: 'grp_1' });
  });
});
