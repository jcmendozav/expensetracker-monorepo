import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { HouseholdService } from '../../core/services/household.service';
import { CreateHouseholdRequest } from '../../core/models/household.model';

@Component({
  selector: 'app-household-create-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule
  ],
  template: `
    <h2 mat-dialog-title>CREATE YOUR HOUSEHOLD</h2>
    <mat-dialog-content>
      <p class="subtitle">Set up a shared space for joint budgeting and expenses.</p>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Household Name</mat-label>
        <input matInput [(ngModel)]="name" name="hhName" placeholder="e.g. Mendoza Family" required />
      </mat-form-field>

      <div class="currency-group">
        <label class="group-label">Base Currencies (Select all that apply):</label>
        <div class="checkbox-row">
          <mat-checkbox [(ngModel)]="currencies.PEN" name="pen">PEN (Peruvian Sol)</mat-checkbox>
          <mat-checkbox [(ngModel)]="currencies.USD" name="usd">USD (US Dollar)</mat-checkbox>
          <mat-checkbox [(ngModel)]="currencies.EUR" name="eur">EUR (Euro)</mat-checkbox>
          <mat-checkbox [(ngModel)]="currencies.COP" name="cop">COP (Colombian Peso)</mat-checkbox>
        </div>
      </div>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Invite Partner as Co-Owner (Optional)</mat-label>
        <input matInput type="email" [(ngModel)]="partnerEmail" name="pEmail" placeholder="partner@example.com" />
      </mat-form-field>

      @if (errorMessage()) {
        <div class="error-banner">{{ errorMessage() }}</div>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-flat-button color="primary" [disabled]="isSubmitting() || !name.trim() || selectedCurrencies().length === 0" (click)="submit()">
        {{ isSubmitting() ? 'Creating...' : 'Create Household →' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .subtitle {
      color: #64748b;
      margin-top: 0;
      margin-bottom: 16px;
    }
    .full-width {
      width: 100%;
      margin-bottom: 8px;
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
      color: #b00020;
      font-size: 0.85rem;
      margin-top: 8px;
    }
  `]
})
export class HouseholdCreateDialogComponent {
  private dialogRef = inject(MatDialogRef<HouseholdCreateDialogComponent>);
  private householdService = inject(HouseholdService);

  name = 'Mendoza Family';
  currencies = {
    PEN: true,
    USD: true,
    EUR: false,
    COP: false
  };
  partnerEmail = '';

  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

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
      partnerEmail: this.partnerEmail.trim() || undefined
    };

    this.householdService.createHousehold(request).subscribe({
      next: (hh) => {
        this.isSubmitting.set(false);
        this.dialogRef.close(hh);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to create household.');
      }
    });
  }
}
