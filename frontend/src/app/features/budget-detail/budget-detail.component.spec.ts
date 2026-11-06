import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { MatBottomSheet } from '@angular/material/bottom-sheet';
import { of } from 'rxjs';
import { BudgetDetailComponent } from './budget-detail.component';
import { BudgetService } from '../../core/services/budget.service';
import type { Budget } from '../../core/models/budget.model';
import type { LedgerTransaction } from '../../core/models/ledger.model';

describe('BudgetDetailComponent', () => {
  let component: BudgetDetailComponent;
  let fixture: ComponentFixture<BudgetDetailComponent>;
  let mockBudgetService: jasmine.SpyObj<BudgetService>;
  let mockBottomSheet: jasmine.SpyObj<MatBottomSheet>;

  const mockBudget: Budget = {
    id: 'bdg-1',
    householdId: 'hh-1',
    ownerId: 'user-1',
    title: 'Groceries',
    currency: 'PEN',
    initialAmountCents: 100000,
    currentBalanceCents: 75000,
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  };

  const mockTx: LedgerTransaction = {
    id: 'tx-1',
    budgetId: 'bdg-1',
    householdId: 'hh-1',
    type: 'SPENDING',
    amountCents: 2500,
    balanceAfterCents: 75000,
    category: 'Market',
    createdBy: 'user-1',
    createdByName: 'User',
    transactionDate: '2026-01-01T00:00:00Z',
    createdAt: '2026-01-01T00:00:00Z',
    status: 'COMMITTED'
  };

  beforeEach(async () => {
    mockBudgetService = jasmine.createSpyObj<BudgetService>('BudgetService', [
      'getBudget',
      'listTransactions'
    ]);
    mockBottomSheet = jasmine.createSpyObj<MatBottomSheet>('MatBottomSheet', ['open']);

    mockBudgetService.getBudget.and.returnValue(of(mockBudget));
    mockBudgetService.listTransactions.and.returnValue(of([mockTx]));

    await TestBed.configureTestingModule({
      imports: [BudgetDetailComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: BudgetService, useValue: mockBudgetService },
        { provide: MatBottomSheet, useValue: mockBottomSheet },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? 'bdg-1' : null)
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BudgetDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load budget and transactions on init', () => {
    expect(component).toBeTruthy();
    expect(component.budget()?.id).toBe('bdg-1');
    expect(component.transactions().length).toBe(1);
    expect(component.progressPercentage()).toBe(75);
  });
});
