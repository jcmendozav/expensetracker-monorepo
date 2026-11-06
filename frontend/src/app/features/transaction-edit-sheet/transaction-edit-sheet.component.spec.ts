import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MAT_BOTTOM_SHEET_DATA, MatBottomSheetRef } from '@angular/material/bottom-sheet';
import { of, throwError } from 'rxjs';
import { TransactionEditSheetComponent } from './transaction-edit-sheet.component';
import { BudgetService } from '../../core/services/budget.service';
import type { LedgerTransaction } from '../../core/models/ledger.model';

describe('TransactionEditSheetComponent', () => {
  let component: TransactionEditSheetComponent;
  let fixture: ComponentFixture<TransactionEditSheetComponent>;
  let mockBudgetService: jasmine.SpyObj<BudgetService>;
  let mockBottomSheetRef: jasmine.SpyObj<MatBottomSheetRef<TransactionEditSheetComponent>>;

  const mockTransaction: LedgerTransaction = {
    id: 'tx-1',
    budgetId: 'bdg-1',
    householdId: 'hh-1',
    type: 'SPENDING',
    amountCents: 2500,
    balanceAfterCents: 75000,
    category: 'Plumbing',
    note: 'Pipe fittings',
    createdBy: 'user-1',
    createdByName: 'Carlos',
    transactionDate: '2026-01-01T00:00:00Z',
    createdAt: '2026-01-01T00:00:00Z',
    status: 'COMMITTED'
  };

  beforeEach(async () => {
    mockBudgetService = jasmine.createSpyObj<BudgetService>('BudgetService', [
      'updateTransactionMetadata',
      'voidTransaction'
    ]);
    mockBottomSheetRef = jasmine.createSpyObj<MatBottomSheetRef<TransactionEditSheetComponent>>('MatBottomSheetRef', [
      'dismiss'
    ]);

    await TestBed.configureTestingModule({
      imports: [TransactionEditSheetComponent],
      providers: [
        provideNoopAnimations(),
        { provide: BudgetService, useValue: mockBudgetService },
        { provide: MatBottomSheetRef, useValue: mockBottomSheetRef },
        {
          provide: MAT_BOTTOM_SHEET_DATA,
          useValue: {
            transaction: mockTransaction,
            budgetId: 'bdg-1',
            currency: 'PEN'
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionEditSheetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with transaction data', () => {
    expect(component).toBeTruthy();
    expect(component.category).toBe('Plumbing');
    expect(component.note).toBe('Pipe fittings');
  });

  it('should save metadata and dismiss', async () => {
    mockBudgetService.updateTransactionMetadata.and.returnValue(of(mockTransaction));

    component.category = 'Hardware';
    component.note = 'Tools';

    await component.saveMetadata();

    expect(mockBudgetService.updateTransactionMetadata).toHaveBeenCalledWith('bdg-1', 'tx-1', {
      category: 'Hardware',
      note: 'Tools'
    });
    expect(mockBottomSheetRef.dismiss).toHaveBeenCalledWith({ updated: true });
  });

  it('should void transaction when confirmed', async () => {
    spyOn(window, 'confirm').and.returnValue(true);
    mockBudgetService.voidTransaction.and.returnValue(of({
      ...mockTransaction,
      status: 'VOIDED'
    }));

    await component.voidTransaction();

    expect(mockBudgetService.voidTransaction).toHaveBeenCalledWith('bdg-1', 'tx-1');
    expect(mockBottomSheetRef.dismiss).toHaveBeenCalledWith({ voided: true });
  });

  it('should not void transaction when window.confirm is cancelled', async () => {
    spyOn(window, 'confirm').and.returnValue(false);

    await component.voidTransaction();

    expect(mockBudgetService.voidTransaction).not.toHaveBeenCalled();
    expect(mockBottomSheetRef.dismiss).not.toHaveBeenCalled();
  });
});
