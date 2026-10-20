import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { TransactionFormComponent } from './transaction-form.component';
import { TransactionService } from '../../core/services/transaction.service';

describe('TransactionFormComponent', () => {
  let component: TransactionFormComponent;
  let fixture: ComponentFixture<TransactionFormComponent>;

  const mockTransactionService = {
    createTransaction: () => of({}),
    updateTransaction: () => of({}),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransactionFormComponent],
      providers: [
        { provide: TransactionService, useValue: mockTransactionService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
