import { Component, inject, output, signal, ChangeDetectionStrategy, effect, input } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TransactionService } from '../../core/services/transaction.service';
import { Transaction } from '../../core/models/transaction.model';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-transaction-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './transaction-form.component.html',
  styleUrls: ['./transaction-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionFormComponent {
  private fb = inject(FormBuilder);
  private transactionService = inject(TransactionService);

  transactionAdded = output<void>();
  transaction = input<Transaction | null>(null);

  transactionForm = this.fb.group({
    description: ['', [Validators.required, Validators.minLength(3)]],
    originalAmount: [0, [Validators.required, Validators.min(0.01)]],
    originalCurrency: ['USD', Validators.required],
    transactionDate: [new Date().toISOString().split('T')[0], Validators.required],
    householdId: [1], // Default value
    categoryId: [101] // Default value
  });

  isSubmitting = signal(false);
  editTransactionId = signal<string | null>(null);

  constructor() {
    effect(() => {
      const tx = this.transaction();
      if (tx) {
        this.editTransactionId.set(tx.id || null);
        let formattedDate = '';
        if (tx.transactionDate) {
          const dateObj = new Date(tx.transactionDate);
          formattedDate = dateObj.toISOString().split('T')[0];
        }
        this.transactionForm.patchValue({ ...tx, transactionDate: formattedDate });
      } else {
        this.resetForm();
      }
    });
  }

  resetForm() {
    this.editTransactionId.set(null);
    this.transactionForm.reset({
      originalCurrency: 'USD',
      transactionDate: new Date().toISOString().split('T')[0],
      householdId: 1,
      categoryId: 101,
      originalAmount: 0,
      description: ''
    });
  }

  async onSubmit() {
    if (this.transactionForm.invalid) {
      return;
    }

    this.isSubmitting.set(true);
    const formValue = this.transactionForm.value;
    const dateString = formValue.transactionDate;
    let payload: any = { ...formValue };

    if (dateString) {
      payload.transactionDate = new Date(dateString + 'T00:00:00').getTime();
    }

    try {
      if (this.editTransactionId()) {
        await firstValueFrom(this.transactionService.updateTransaction(this.editTransactionId()!, payload as any));
      } else {
        await firstValueFrom(this.transactionService.createTransaction(payload as any));
      }
      this.finishSubmit();
    } catch (err) {
      console.error('Submit failed', err);
    } finally {
      this.isSubmitting.set(false);
    }
  }

  private finishSubmit() {
    this.resetForm();
    this.transactionAdded.emit();
  }
}