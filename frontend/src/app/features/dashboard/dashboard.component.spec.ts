import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { TransactionService } from '../../core/services/transaction.service';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  const mockTransactionService = {
    getDashboardSummary: () => of({ totalSpent: 0 }),
    getTransactions: () => of([]),
    createTransaction: () => of({}),
    deleteTransaction: () => of(undefined),
    updateTransaction: () => of({}),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TransactionService, useValue: mockTransactionService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
