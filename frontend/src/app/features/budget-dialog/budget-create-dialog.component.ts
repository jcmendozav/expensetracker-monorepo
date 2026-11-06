import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { BudgetService } from '../../core/services/budget.service';
import type { CreateBudgetRequest } from '../../core/models/budget.model';

export interface BudgetCreateDialogData {
  householdId: string;
  availableCurrencies: string[];
}

@Component({
  selector: 'app-budget-create-dialog',
  imports: [
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>Create budget</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Budget title</mat-label>
        <input matInput [(ngModel)]="title" name="budgetTitle" placeholder="e.g. Groceries & Market" required />
      </mat-form-field>

      <div class="row">
        <mat-form-field appearance="outline" class="amount-field">
          <mat-label>Initial capital allocation</mat-label>
          <input
            matInput
            type="number"
            step="0.01"
            min="0.01"
            inputmode="decimal"
            [(ngModel)]="amount"
            name="initialAmount"
            placeholder="0.00"
            required />
        </mat-form-field>

        <mat-form-field appearance="outline" class="currency-field">
          <mat-label>Currency</mat-label>
          <mat-select [(ngModel)]="currency" name="currencySelect">
            @for (c of data.availableCurrencies; track c) {
              <mat-option [value]="c">{{ c }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </div>

      @if (errorMessage()) {
        <div class="error-banner" role="alert">{{ errorMessage() }}</div>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="cancel()">Cancel</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="isSubmitting() || !title.trim() || !amount || amount <= 0"
        (click)="submit()">
        {{ isSubmitting() ? 'Saving...' : 'Save budget' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width {
      width: 100%;
      margin-bottom: 8px;
    }
    .row {
      display: flex;
      gap: 12px;
    }
    .amount-field {
      flex: 2;
    }
    .currency-field {
      flex: 1;
    }
    .error-banner {
      color: #b00020;
      font-size: 0.85rem;
      margin-top: 8px;
    }
  `]
})
export class BudgetCreateDialogComponent {
  readonly dialogRef = inject(MatDialogRef<BudgetCreateDialogComponent>);
  readonly data: BudgetCreateDialogData = inject(MAT_DIALOG_DATA);
  private readonly budgetService = inject(BudgetService);

  title = '';
  amount: number | null = null;
  currency = 'PEN';

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    if (this.data?.availableCurrencies && this.data.availableCurrencies.length > 0) {
      this.currency = this.data.availableCurrencies[0];
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  submit(): void {
    if (!this.title.trim() || !this.amount || this.amount <= 0) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const request: CreateBudgetRequest = {
      householdId: this.data.householdId,
      title: this.title.trim(),
      initialAmountCents: Math.round(this.amount * 100),
      currency: this.currency
    };

    this.budgetService.createBudget(request).subscribe({
      next: (b) => {
        this.isSubmitting.set(false);
        this.dialogRef.close(b);
      },
      error: (err: unknown) => {
        this.isSubmitting.set(false);
        if (err instanceof HttpErrorResponse) {
          this.errorMessage.set(err.error?.message ?? err.message ?? 'Failed to create budget.');
        } else {
          this.errorMessage.set('Failed to create budget.');
        }
      }
    });
  }
}
