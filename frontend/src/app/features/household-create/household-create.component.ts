import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { HouseholdService } from '../../core/services/household.service';
import type { CreateHouseholdRequest } from '../../core/models/household.model';

@Component({
  selector: 'app-household-create',
  imports: [
    FormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule
  ],
  template: `
    <div class="page-container">
      <mat-card appearance="outlined" class="form-card">
        <mat-card-header>
          <mat-card-title>Create your household</mat-card-title>
          <mat-card-subtitle>Set up a shared space for joint budgeting and expenses.</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form (ngSubmit)="submit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Household name</mat-label>
              <input
                matInput
                [(ngModel)]="name"
                name="name"
                placeholder="e.g. Mendoza Family"
                required />
            </mat-form-field>

            <div class="currency-group">
              <label class="group-label">Base currencies (select all that apply):</label>
              <div class="checkbox-row">
                <mat-checkbox [(ngModel)]="currencies.PEN" name="pen">PEN (Peruvian Sol)</mat-checkbox>
                <mat-checkbox [(ngModel)]="currencies.USD" name="usd">USD (US Dollar)</mat-checkbox>
                <mat-checkbox [(ngModel)]="currencies.EUR" name="eur">EUR (Euro)</mat-checkbox>
                <mat-checkbox [(ngModel)]="currencies.COP" name="cop">COP (Colombian Peso)</mat-checkbox>
              </div>
            </div>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Invite partner as co-owner (optional)</mat-label>
              <input
                matInput
                type="email"
                [(ngModel)]="partnerEmail"
                name="partnerEmail"
                placeholder="partner@example.com" />
            </mat-form-field>

            @if (errorMessage()) {
              <div class="error-banner" role="alert">{{ errorMessage() }}</div>
            }

            <div class="card-actions">
              <a mat-button routerLink="/dashboard">Cancel</a>
              <button
                mat-flat-button
                color="primary"
                type="submit"
                [disabled]="isSubmitting() || !name.trim() || selectedCurrencies().length === 0">
                {{ isSubmitting() ? 'Creating...' : 'Create household' }}
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
    .currency-group {
      margin-bottom: 16px;
    }
    .group-label {
      display: block;
      font-size: 0.85rem;
      font-weight: 500;
      color: #334155;
      margin-bottom: 8px;
    }
    .checkbox-row {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
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
export class HouseholdCreateComponent {
  private readonly householdService = inject(HouseholdService);
  private readonly router = inject(Router);

  name = 'Mendoza Family';
  currencies = {
    PEN: true,
    USD: true,
    EUR: false,
    COP: false
  };
  partnerEmail = '';

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  selectedCurrencies(): string[] {
    const keys: (keyof typeof this.currencies)[] = ['PEN', 'USD', 'EUR', 'COP'];
    return keys.filter(k => this.currencies[k]);
  }

  submit(): void {
    const baseCurrencies = this.selectedCurrencies();
    if (!this.name.trim() || baseCurrencies.length === 0) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const request: CreateHouseholdRequest = {
      name: this.name.trim(),
      baseCurrencies,
      partnerEmail: this.partnerEmail.trim() ? this.partnerEmail.trim() : undefined
    };

    this.householdService.createHousehold(request).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err: unknown) => {
        this.isSubmitting.set(false);
        if (err instanceof HttpErrorResponse) {
          this.errorMessage.set(err.error?.message ?? err.message ?? 'Failed to create household.');
        } else {
          this.errorMessage.set('Failed to create household.');
        }
      }
    });
  }
}
