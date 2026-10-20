import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { TransactionListComponent } from './transaction-list.component';
import { TransactionService } from '../../core/services/transaction.service';

describe('TransactionListComponent', () => {
  let component: TransactionListComponent;
  let fixture: ComponentFixture<TransactionListComponent>;

  const mockTransactionService = {
    getTransactions: () => of([]),
    deleteTransaction: () => of(undefined),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransactionListComponent],
      providers: [
        { provide: TransactionService, useValue: mockTransactionService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
