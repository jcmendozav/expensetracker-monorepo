import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { TransferModalComponent } from './transfer-modal.component';
import { BudgetService } from '../../core/services/budget.service';
import type { Budget } from '../../core/models/budget.model';
import type { LedgerTransaction, TransferResult } from '../../core/models/ledger.model';

describe('TransferModalComponent', () => {
  let component: TransferModalComponent;
  let fixture: ComponentFixture<TransferModalComponent>;
  let mockBudgetService: jasmine.SpyObj<BudgetService>;
  let mockDialogRef: jasmine.SpyObj<MatDialogRef<TransferModalComponent>>;

  const mockBudgets: Budget[] = [
    {
      id: 'bdg-1',
      householdId: 'hh-1',
      ownerId: 'user-1',
      title: 'Groceries',
      currency: 'PEN',
      initialAmountCents: 100000,
      currentBalanceCents: 80000,
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z'
    },
    {
      id: 'bdg-2',
      householdId: 'hh-1',
      ownerId: 'user-1',
      title: 'Savings',
      currency: 'USD',
      initialAmountCents: 50000,
      currentBalanceCents: 50000,
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z'
    }
  ];

  beforeEach(async () => {
    mockBudgetService = jasmine.createSpyObj<BudgetService>('BudgetService', ['transferFunds']);
    mockDialogRef = jasmine.createSpyObj<MatDialogRef<TransferModalComponent>>('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [TransferModalComponent],
      providers: [
        provideNoopAnimations(),
        { provide: BudgetService, useValue: mockBudgetService },
        { provide: MatDialogRef, useValue: mockDialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            budgets: mockBudgets,
            defaultSourceBudgetId: 'bdg-1'
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TransferModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with source and target budgets', () => {
    expect(component).toBeTruthy();
    expect(component.sourceBudgetId).toBe('bdg-1');
    expect(component.targetBudgetId).toBe('bdg-2');
    expect(component.isCrossCurrency()).toBeTrue();
  });

  it('should submit transfer with integer minor units and close dialog on success', () => {
    const mockTx: LedgerTransaction = {
      id: 'tx-100',
      householdId: 'hh-1',
      budgetId: 'bdg-1',
      type: 'TRANSFER_OUT',
      status: 'COMMITTED',
      amountCents: 5000,
      balanceAfterCents: 75000,
      category: 'Transfer',
      transactionDate: '2026-01-01T00:00:00Z',
      createdBy: 'user-1',
      createdByName: 'User',
      createdAt: '2026-01-01T00:00:00Z'
    };

    const mockResult: TransferResult = {
      transferGroupId: 'tg-100',
      sourceTransaction: mockTx,
      targetTransaction: { ...mockTx, id: 'tx-101', budgetId: 'bdg-2', type: 'TRANSFER_IN' },
      sourceRemainingBalanceCents: 75000,
      targetRemainingBalanceCents: 51333
    };

    mockBudgetService.transferFunds.and.returnValue(of(mockResult));

    component.sourceAmount = 50;
    component.exchangeRate = 0.2667;
    component.note = 'Test transfer';

    component.submit();

    expect(mockBudgetService.transferFunds).toHaveBeenCalledWith({
      sourceBudgetId: 'bdg-1',
      targetBudgetId: 'bdg-2',
      sourceAmountCents: 5000,
      exchangeRate: 0.2667,
      note: 'Test transfer'
    });
    expect(mockDialogRef.close).toHaveBeenCalledWith(mockResult);
  });

  it('should set error message on HTTP error', () => {
    const errorResponse = new HttpErrorResponse({
      status: 400,
      error: { message: 'Insufficient funds' }
    });
    mockBudgetService.transferFunds.and.returnValue(throwError(() => errorResponse));

    component.sourceAmount = 50;
    component.submit();

    expect(component.errorMessage()).toBe('Insufficient funds');
    expect(component.isSubmitting()).toBeFalse();
  });
});
