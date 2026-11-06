import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { BudgetService } from '../../core/services/budget.service';
import { HouseholdService } from '../../core/services/household.service';
import type { CreateBudgetRequest } from '../../core/models/budget.model';

@Component({
  selector: 'app-budget-create',
  imports: [
    FormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule
  ],
  template: `
    <div class="page-container">
      <mat-card appearance="outlined" class="form-card">
        <mat-card-header>
          <mat-card-title>Create budget</mat-card-title>
          <mat-card-subtitle>Allocate funds to a specific category or goal.</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form (ngSubmit)="submit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Budget title</mat-label>
              <input
                matInput
                [(ngModel)]="title"
                name="title"
                placeholder="e.g. Groceries & Market"
                required />
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
                  name="amount"
                  placeholder="0.00"
                  required />
              </mat-form-field>

              <mat-form-field appearance="outline" class="currency-field">
                <mat-label>Currency</mat-label>
                <mat-select [(ngModel)]="currency" name="currency">
                  @for (c of availableCurrencies(); track c) {
                    <mat-option [value]="c">{{ c }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
            </div>

            @if (errorMessage()) {
              <div class="error-banner" role="alert">{{ errorMessage() }}</div>
            }

            <div class="card-actions">
              <a mat-button routerLink="/dashboard">Cancel</a>
              <button
                mat-flat-button
                color="primary"
                type="submit"
                [disabled]="isSubmitting() || !title.trim() || !amount || amount <= 0">
                {{ isSubmitting() ? 'Saving...' : 'Save budget' }}
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container {
      display: flex;
      justify-content: center;
      padding: 32px 16px;
    }
    .form-card {
      max-width: 520px;
      width: 100%;
      padding: 16px;
    }
    .full-width {
      width: 100%;
      margin-bottom: 12px;
    }
    .row {
      display: flex;
      gap: 12px;
      margin-bottom: 12px;
    }
    .amount-field {
      flex: 2;
    }
    .currency-field {
      flex: 1;
    }
    .error-banner {
      background: #fee2e2;
      color: #b91c1c;
      padding: 8px 12px;
      border-radius: 6px;
      margin-bottom: 16px;
      font-size: 0.85rem;
    }
    .card-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 16px;
    }
  `]
})
export class BudgetCreateComponent implements OnInit {
  private readonly budgetService = inject(BudgetService);
  private readonly householdService = inject(HouseholdService);
  private readonly router = inject(Router);

  title = '';
  amount: number | null = null;
  currency = 'PEN';
  householdId = signal<string | null>(null);
  availableCurrencies = signal<string[]>(['PEN', 'USD']);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.householdService.getCurrentHousehold().subscribe({
      next: (hh) => {
        this.householdId.set(hh.id);
        if (hh.baseCurrencies && hh.baseCurrencies.length > 0) {
          this.availableCurrencies.set(hh.baseCurrencies);
          this.currency = hh.baseCurrencies[0];
        }
      },
      error: () => {
        this.errorMessage.set('Please create a household first before creating a budget.');
      }
    });
  }

  submit(): void {
    const hhId = this.householdId();
    if (!hhId || !this.title.trim() || !this.amount || this.amount <= 0) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const request: CreateBudgetRequest = {
      householdId: hhId,
      title: this.title.trim(),
      initialAmountCents: Math.round(this.amount * 100),
      currency: this.currency
    };

    this.budgetService.createBudget(request).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.router.navigate(['/dashboard']);
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
