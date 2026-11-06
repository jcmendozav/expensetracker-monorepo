import { Component, Inject, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_BOTTOM_SHEET_DATA, MatBottomSheetRef } from '@angular/material/bottom-sheet';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { Budget } from '../../core/models/budget.model';
import { BudgetService } from '../../core/services/budget.service';
import { LogTransactionRequest } from '../../core/models/ledger.model';

export interface ExpenseEntrySheetData {
  budgets: Budget[];
  defaultBudgetId?: string;
}

@Component({
  selector: 'app-expense-entry-sheet',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatButtonToggleModule
  ],
  template: `
    <div class="sheet-container">
      <div class="drag-handle"></div>
      <div class="sheet-header">
        <h2>LOG TRANSACTION</h2>
        <mat-button-toggle-group [(ngModel)]="type" name="txType" aria-label="Transaction Type">
          <mat-button-toggle value="SPENDING">Expense</mat-button-toggle>
          <mat-button-toggle value="INCOME">+ Top-up</mat-button-toggle>
        </mat-button-toggle-group>
      </div>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Budget</mat-label>
        <mat-select [(ngModel)]="selectedBudgetId" name="budgetSelect">
          @for (budget of data.budgets; track budget.id) {
            <mat-option [value]="budget.id">
              {{ budget.title }} ({{ budget.currency }} {{ budget.currentBalanceCents / 100 | number:'1.2-2' }} remaining)
            </mat-option>
          }
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width amount-field">
        <mat-label>Amount</mat-label>
        <input
          matInput
          type="number"
          step="0.01"
          min="0.01"
          inputmode="decimal"
          [(ngModel)]="amount"
          name="amountInput"
          placeholder="0.00"
          required
          autofocus />
        <span matTextPrefix class="amount-prefix">{{ selectedBudgetCurrency() }}&nbsp;</span>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Category</mat-label>
        <input matInput [(ngModel)]="category" name="categoryInput" placeholder="e.g. Groceries, Plumbing" required />
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Note (Optional)</mat-label>
        <input matInput [(ngModel)]="note" name="noteInput" placeholder="Details or vendor" />
      </mat-form-field>

      @if (errorMessage()) {
        <div class="error-banner">{{ errorMessage() }}</div>
      }

      <div class="actions">
        <button mat-button type="button" (click)="cancel()">Cancel</button>
        <button
          mat-flat-button
          color="primary"
          type="button"
          [disabled]="isSubmitting() || !amount || amount <= 0 || !selectedBudgetId || !category"
          (click)="submit()">
          {{ isSubmitting() ? 'Saving...' : 'Confirm & Save Transaction (Tap 3)' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .sheet-container {
      padding: 16px 20px 24px;
      max-width: 500px;
      margin: 0 auto;
    }
    .drag-handle {
      width: 40px;
      height: 4px;
      background: #ccc;
      border-radius: 2px;
      margin: 0 auto 12px;
    }
    .sheet-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    .sheet-header h2 {
      margin: 0;
      font-size: 1.1rem;
      font-weight: 600;
    }
    .full-width {
      width: 100%;
      margin-bottom: 8px;
    }
    .amount-field input {
      font-size: 1.4rem;
      font-weight: 600;
    }
    .amount-prefix {
      font-weight: 600;
      font-size: 1.2rem;
    }
    .actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 12px;
    }
    .error-banner {
      color: #b00020;
      font-size: 0.85rem;
      margin-bottom: 8px;
    }
  `]
})
export class ExpenseEntrySheetComponent {
  private bottomSheetRef = inject(MatBottomSheetRef<ExpenseEntrySheetComponent>);
  private budgetService = inject(BudgetService);

  type: 'SPENDING' | 'INCOME' = 'SPENDING';
  selectedBudgetId = '';
  amount: number | null = null;
  category = 'General';
  note = '';

  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  constructor(@Inject(MAT_BOTTOM_SHEET_DATA) public data: ExpenseEntrySheetData) {
    if (data.defaultBudgetId) {
      this.selectedBudgetId = data.defaultBudgetId;
    } else if (data.budgets.length > 0) {
      this.selectedBudgetId = data.budgets[0].id;
    }
  }

  selectedBudgetCurrency(): string {
    const budget = this.data.budgets.find(b => b.id === this.selectedBudgetId);
    return budget ? budget.currency : '';
  }

  cancel(): void {
    this.bottomSheetRef.dismiss(null);
  }

  submit(): void {
    if (!this.selectedBudgetId || !this.amount || this.amount <= 0 || !this.category) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const amountCents = Math.round(this.amount * 100);
    const request: LogTransactionRequest = {
      type: this.type,
      amountCents,
      category: this.category,
      note: this.note.trim() || undefined
    };

    this.budgetService.logTransaction(this.selectedBudgetId, request).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.bottomSheetRef.dismiss(res);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to log transaction. Please try again.');
      }
    });
  }
}

