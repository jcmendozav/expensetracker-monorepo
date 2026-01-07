import { Component, inject, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TransactionService } from '../../core/services/transaction.service';
import { Transaction } from '../../core/models/transaction.model'; // Import your model

@Component({
  selector: 'app-transaction-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transaction-form.component.html',
  styleUrls: ['./transaction-form.component.scss']
})
export class TransactionFormComponent {
  private fb = inject(FormBuilder);
  private transactionService = inject(TransactionService);

  // Notify the parent component when a transaction is added
  @Output() transactionAdded = new EventEmitter<void>();

  transactionForm = this.fb.group({
    description: ['', [Validators.required, Validators.minLength(3)]],
    originalAmount: [0, [Validators.required, Validators.min(0.01)]],
    originalCurrency: ['USD', Validators.required],
    transactionDate: [new Date().toISOString().split('T')[0], Validators.required],
    householdId: [1], // Default value
    categoryId: [101] // Default value
  });

  isSubmitting = false;

  // Track the ID of the transaction being edited
  editTransactionId: string | null = null;

  // Method called by Parent (Dashboard) to fill the form
  setFormData(transaction: Transaction) {
    this.editTransactionId = transaction.id || null;

    // 1. Convert milliseconds (number) to "yyyy-MM-dd" string
    let formattedDate = '';
    if (transaction.transactionDate) {
      // Create a Date object from the milliseconds
      const dateObj = new Date(transaction.transactionDate);
      // specific format required by <input type="date">
      formattedDate = dateObj.toISOString().split('T')[0];
    }

    this.transactionForm.patchValue({
      description: transaction.description,
      originalAmount: transaction.originalAmount,
      originalCurrency: transaction.originalCurrency,
      transactionDate: formattedDate, // <--- Use the formatted string here
      householdId: transaction.householdId,
      categoryId: transaction.categoryId
    });
  }

  // Method to cancel edit and clear form
  cancelEdit() {
    this.editTransactionId = null;
    this.transactionForm.reset({
      originalCurrency: 'USD',
      transactionDate: new Date().toISOString().split('T')[0],
      householdId: 1,
      categoryId: 101,
      originalAmount: 0
    });
  }

  onSubmit() {
    if (this.transactionForm.valid) {
      this.isSubmitting = true;

      // 1. Create a raw object from the form values
      const formValue = this.transactionForm.value;

      // 2. Convert the date string ("2025-12-28") to milliseconds (1766...)
      // We check if transactionDate exists to avoid null errors
      const dateString = formValue.transactionDate;
      let payload: any = { ...formValue }; // Create a copy to modify

      if (dateString) {
        // Create a date object and get the timestamp (Long)
        // Note: This creates a date at midnight in the user's local time
        payload.transactionDate = new Date(dateString + 'T00:00:00').getTime();
      }

      // 3. Send the MODIFIED payload (payload), not the raw form (this.transactionForm.value)
      if (this.editTransactionId) {
        // --- UPDATE MODE ---
        // Cast to 'any' or your Transaction interface
        this.transactionService.updateTransaction(this.editTransactionId, payload as any).subscribe({
          next: () => {
            this.finishSubmit('Transaction updated!');
          },
          error: (err) => {
            console.error('Update failed', err);
            this.isSubmitting = false;
          }
        });
      } else {
        // --- CREATE MODE ---
        this.transactionService.createTransaction(payload as any).subscribe({
          next: () => {
            this.finishSubmit('Transaction added!');
          },
          error: (err) => {
            console.error('Create failed', err);
            this.isSubmitting = false;
          }
        });
      }
    }
  }

  // Helper to clean up after success
  private finishSubmit(msg: string) {
    alert(msg);
    this.cancelEdit(); // Resets form and mode
    this.isSubmitting = false;
    this.transactionAdded.emit(); // Tell dashboard to refresh
  }
}