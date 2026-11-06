import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { MAT_BOTTOM_SHEET_DATA, MatBottomSheetRef } from '@angular/material/bottom-sheet';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { firstValueFrom } from 'rxjs';
import type { Budget } from '../../core/models/budget.model';
import { BudgetService } from '../../core/services/budget.service';
import type { LogTransactionRequest } from '../../core/models/ledger.model';

export interface ExpenseEntrySheetData {
  budgets: Budget[];
  defaultBudgetId?: string;
}

@Component({
  selector: 'app-expense-entry-sheet',
  imports: [
    FormsModule,
    DecimalPipe,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatButtonToggleModule
  ],
  template: `
    <div class="sheet-container">
      <div class="drag-handle" aria-hidden="true"></div>
      <div class="sheet-header">
        <h2>Log Transaction</h2>
        <mat-button-toggle-group [(ngModel)]="type" name="txType" aria-label="Transaction Type">
          <mat-button-toggle value="SPENDING">Expense</mat-button-toggle>
          <mat-button-toggle value="INCOME">Top-up</mat-button-toggle>
        </mat-button-toggle-group>
      </div>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Budget</mat-label>
        <mat-select [(ngModel)]="selectedBudgetId" name="budgetSelect">
          @for (budget of data.budgets; track budget.id) {
            <mat-option [value]="budget.id">
              {{ budget.title }} ({{ budget.currency }} {{ (budget.currentBalanceCents / 100) | number:'1.2-2' }} remaining)
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
          required />
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
        <div class="error-banner" role="alert">{{ errorMessage() }}</div>
      }

      <div class="actions">
        <button mat-button type="button" (click)="cancel()">Cancel</button>
        <button
          mat-flat-button
          color="primary"
          type="button"
          [disabled]="isSubmitting() || !amount || amount <= 0 || !selectedBudgetId || !category"
          (click)="submit()">
          {{ isSubmitting() ? 'Saving...' : 'Confirm & Save' }}
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
      background: #cbd5e1;
      border-radius: 2px;
      margin: 0 auto 16px;
    }
    .sheet-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
    }
    .sheet-header h2 {
      margin: 0;
      font-size: 1.15rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .full-width {
      width: 100%;
      margin-bottom: 12px;
    }
    .amount-field input {
      font-size: 1.4rem;
      font-weight: 600;
    }
    .amount-prefix {
      font-weight: 700;
      color: #64748b;
    }
    .error-banner {
      background: #fee2e2;
      color: #b91c1c;
      padding: 8px 12px;
      border-radius: 6px;
      margin-bottom: 16px;
      font-size: 0.85rem;
    }
    .actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 8px;
    }
  `]
})
export class ExpenseEntrySheetComponent {
  readonly data = inject<ExpenseEntrySheetData>(MAT_BOTTOM_SHEET_DATA);
  private readonly bottomSheetRef = inject(MatBottomSheetRef<ExpenseEntrySheetComponent>);
  private readonly budgetService = inject(BudgetService);

  type: 'SPENDING' | 'INCOME' = 'SPENDING';
  selectedBudgetId: string = this.data.defaultBudgetId ?? (this.data.budgets.length > 0 ? this.data.budgets[0].id : '');
  amount: number | null = null;
  category = '';
  note = '';

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  selectedBudgetCurrency(): string {
    const b = this.data.budgets.find(item => item.id === this.selectedBudgetId);
    return b ? b.currency : 'PEN';
  }

  async submit(): Promise<void> {
    if (!this.selectedBudgetId || !this.amount || this.amount <= 0 || !this.category.trim()) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const amountCents = Math.round(this.amount * 100);

    const request: LogTransactionRequest = {
      type: this.type,
      amountCents,
      category: this.category.trim(),
      note: this.note.trim() ? this.note.trim() : undefined
    };

    try {
      await firstValueFrom(this.budgetService.logTransaction(this.selectedBudgetId, request));
      this.bottomSheetRef.dismiss(true);
    } catch (err: unknown) {
      this.errorMessage.set('Failed to save transaction. Please check your network and balance.');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  cancel(): void {
    this.bottomSheetRef.dismiss(false);
  }
}
