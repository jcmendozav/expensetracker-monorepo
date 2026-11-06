import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { BudgetService } from '../../core/services/budget.service';
import type { Budget, UpdateBudgetRequest } from '../../core/models/budget.model';

@Component({
  selector: 'app-budget-settings',
  imports: [
    FormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  template: `
    <div class="page-container">
      @if (isLoading()) {
        <div class="loading-state">Loading budget settings...</div>
      } @else if (!budget()) {
        <mat-card appearance="outlined" class="settings-card">
          <mat-card-content>
            <p>Budget not found.</p>
            <a mat-flat-button color="primary" routerLink="/dashboard">Back to dashboard</a>
          </mat-card-content>
        </mat-card>
      } @else {
        <mat-card appearance="outlined" class="settings-card">
          <mat-card-header>
            <mat-card-title>Edit budget settings</mat-card-title>
            <mat-card-subtitle>Update title and target capital allocation.</mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <form (ngSubmit)="saveSettings()">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Budget title</mat-label>
                <input matInput [(ngModel)]="title" name="budgetTitle" required />
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Adjust capital allocation ({{ budget()!.currency }})</mat-label>
                <input
                  matInput
                  type="number"
                  step="0.01"
                  min="0.01"
                  inputmode="decimal"
                  [(ngModel)]="amount"
                  name="budgetAmount"
                  required />
              </mat-form-field>

              @if (errorMessage()) {
                <div class="error-banner" role="alert">{{ errorMessage() }}</div>
              }
              @if (successMessage()) {
                <div class="success-banner" role="status">{{ successMessage() }}</div>
              }

              <div class="danger-zone">
                <h4>Danger zone</h4>
                <p>Archiving this budget will remove it from active liquidity calculations.</p>
                <button
                  mat-stroked-button
                  color="warn"
                  type="button"
                  [disabled]="isArchiving()"
                  (click)="archiveBudget()">
                  {{ isArchiving() ? 'Archiving...' : 'Archive budget' }}
                </button>
              </div>

              <div class="card-actions">
                <a mat-button [routerLink]="['/budgets', budget()!.id]">Cancel</a>
                <button
                  mat-flat-button
                  color="primary"
                  type="submit"
                  [disabled]="isSaving() || !title.trim() || !amount || amount <= 0">
                  {{ isSaving() ? 'Saving...' : 'Save changes' }}
                </button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .page-container {
      display: flex;
      justify-content: center;
      padding: 32px 16px;
    }
    .settings-card {
      max-width: 540px;
      width: 100%;
      padding: 16px;
    }
    .full-width {
      width: 100%;
      margin-bottom: 12px;
    }
    .danger-zone {
      border: 1px solid #fecaca;
      background: #fff5f5;
      border-radius: 8px;
      padding: 16px;
      margin: 20px 0;
    }
    .danger-zone h4 {
      margin: 0 0 6px;
      color: #b91c1c;
      font-size: 0.95rem;
    }
    .danger-zone p {
      font-size: 0.85rem;
      color: #7f1d1d;
      margin: 0 0 12px;
    }
    .error-banner {
      background: #fee2e2;
      color: #b91c1c;
      padding: 8px 12px;
      border-radius: 6px;
      margin-bottom: 16px;
      font-size: 0.85rem;
    }
    .success-banner {
      background: #dcfce7;
      color: #15803d;
      padding: 8px 12px;
      border-radius: 6px;
      margin-bottom: 16px;
      font-size: 0.85rem;
    }
    .card-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
    .loading-state {
      padding: 40px;
      text-align: center;
      color: #64748b;
    }
  `]
})
export class BudgetSettingsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly budgetService = inject(BudgetService);
  private readonly router = inject(Router);

  readonly budget = signal<Budget | null>(null);
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly isArchiving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  budgetId = '';
  title = '';
  amount: number | null = null;

  ngOnInit(): void {
    this.budgetId = this.route.snapshot.paramMap.get('id') ?? '';
    if (this.budgetId) {
      this.loadBudget();
    } else {
      this.isLoading.set(false);
    }
  }

  loadBudget(): void {
    this.isLoading.set(true);
    this.budgetService.getBudget(this.budgetId).subscribe({
      next: (b) => {
        this.budget.set(b);
        this.title = b.title;
        this.amount = b.initialAmountCents / 100;
        this.isLoading.set(false);
      },
      error: () => {
        this.budget.set(null);
        this.isLoading.set(false);
      }
    });
  }

  saveSettings(): void {
    const b = this.budget();
    if (!b || !this.title.trim() || !this.amount || this.amount <= 0) return;

    this.isSaving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const updateReq: UpdateBudgetRequest = {
      title: this.title.trim(),
      initialAmountCents: Math.round(this.amount * 100)
    };

    this.budgetService.updateBudget(b.id, updateReq).subscribe({
      next: (updated) => {
        this.budget.set(updated);
        this.isSaving.set(false);
        this.successMessage.set('Budget updated successfully.');
        this.router.navigate(['/budgets', b.id]);
      },
      error: (err: unknown) => {
        this.isSaving.set(false);
        if (err instanceof HttpErrorResponse) {
          this.errorMessage.set(err.error?.message ?? err.message ?? 'Failed to update budget.');
        } else {
          this.errorMessage.set('Failed to update budget.');
        }
      }
    });
  }

  archiveBudget(): void {
    const b = this.budget();
    if (!b) return;
    const confirmed = window.confirm('Are you sure you want to archive this budget?');
    if (!confirmed) return;

    this.isArchiving.set(true);
    this.budgetService.archiveBudget(b.id).subscribe({
      next: () => {
        this.isArchiving.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.isArchiving.set(false);
        this.errorMessage.set('Failed to archive budget.');
      }
    });
  }
}
