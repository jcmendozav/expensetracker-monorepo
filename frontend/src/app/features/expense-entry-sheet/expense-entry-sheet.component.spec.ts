import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MAT_BOTTOM_SHEET_DATA, MatBottomSheetRef } from '@angular/material/bottom-sheet';
import { of, throwError } from 'rxjs';
import { ExpenseEntrySheetComponent } from './expense-entry-sheet.component';
import { BudgetService } from '../../core/services/budget.service';
import type { Budget } from '../../core/models/budget.model';

describe('ExpenseEntrySheetComponent', () => {
  let component: ExpenseEntrySheetComponent;
  let fixture: ComponentFixture<ExpenseEntrySheetComponent>;
  let mockBudgetService: jasmine.SpyObj<BudgetService>;
  let mockBottomSheetRef: jasmine.SpyObj<MatBottomSheetRef<ExpenseEntrySheetComponent>>;

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
    }
  ];

  beforeEach(async () => {
    mockBudgetService = jasmine.createSpyObj<BudgetService>('BudgetService', ['logTransaction']);
    mockBottomSheetRef = jasmine.createSpyObj<MatBottomSheetRef<ExpenseEntrySheetComponent>>('MatBottomSheetRef', ['dismiss']);

    await TestBed.configureTestingModule({
      imports: [ExpenseEntrySheetComponent],
      providers: [
        provideNoopAnimations(),
        { provide: BudgetService, useValue: mockBudgetService },
        { provide: MatBottomSheetRef, useValue: mockBottomSheetRef },
        {
          provide: MAT_BOTTOM_SHEET_DATA,
          useValue: {
            budgets: mockBudgets,
            defaultBudgetId: 'bdg-1'
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ExpenseEntrySheetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create with initial budget selected', () => {
    expect(component).toBeTruthy();
    expect(component.selectedBudgetId).toBe('bdg-1');
    expect(component.selectedBudgetCurrency()).toBe('PEN');
  });

  it('should submit transaction with integer cents and dismiss sheet', async () => {
    mockBudgetService.logTransaction.and.returnValue(of({
      id: 'tx-1',
      budgetId: 'bdg-1',
      householdId: 'hh-1',
      type: 'SPENDING',
      amountCents: 2550,
      balanceAfterCents: 77450,
      category: 'Market',
      note: 'Vegetables',
      createdBy: 'user-1',
      createdByName: 'User',
      transactionDate: '2026-01-01T00:00:00Z',
      createdAt: '2026-01-01T00:00:00Z',
      status: 'COMMITTED'
    }));

    component.amount = 25.50;
    component.category = 'Market';
    component.note = 'Vegetables';

    await component.submit();

    expect(mockBudgetService.logTransaction).toHaveBeenCalledWith('bdg-1', {
      type: 'SPENDING',
      amountCents: 2550,
      category: 'Market',
      note: 'Vegetables'
    });
    expect(mockBottomSheetRef.dismiss).toHaveBeenCalledWith(true);
  });

  it('should handle error when transaction fails', async () => {
    mockBudgetService.logTransaction.and.returnValue(throwError(() => new Error('Server error')));

    component.amount = 10;
    component.category = 'Snacks';

    await component.submit();

    expect(component.errorMessage()).toContain('Failed to save transaction');
    expect(component.isSubmitting()).toBeFalse();
  });
});
