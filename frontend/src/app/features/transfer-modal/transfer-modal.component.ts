import { Component, Inject, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { Budget } from '../../core/models/budget.model';
import { BudgetService } from '../../core/services/budget.service';
import { TransferRequest } from '../../core/models/ledger.model';

export interface TransferModalData {
  budgets: Budget[];
  defaultSourceBudgetId?: string;
}

@Component({
  selector: 'app-transfer-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>Transfer / Split Budget Funds</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Source Budget</mat-label>
        <mat-select [(ngModel)]="sourceBudgetId" name="sourceBudgetSelect" (selectionChange)="onBudgetChange()">
          @for (b of data.budgets; track b.id) {
            <mat-option [value]="b.id">
              {{ b.title }} ({{ b.currency }} {{ b.currentBalanceCents / 100 | number:'1.2-2' }} available)
            </mat-option>
          }
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Destination Budget</mat-label>
        <mat-select [(ngModel)]="targetBudgetId" name="targetBudgetSelect" (selectionChange)="onBudgetChange()">
          @for (b of destinationCandidates(); track b.id) {
            <mat-option [value]="b.id">
              {{ b.title }} ({{ b.currency }} {{ b.currentBalanceCents / 100 | number:'1.2-2' }})
            </mat-option>
          }
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Transfer Amount</mat-label>
        <input
          matInput
          type="number"
          step="0.01"
          min="0.01"
          inputmode="decimal"
          [(ngModel)]="sourceAmount"
          name="amountInput"
          placeholder="0.00"
          required />
        <span matTextPrefix>{{ sourceCurrency() }}&nbsp;</span>
      </mat-form-field>

      @if (isCrossCurrency()) {
        <div class="fx-section">
          <mat-form-field appearance="outline" class="fx-rate-field">
            <mat-label>Exchange Rate (1 {{ sourceCurrency() }} = ? {{ targetCurrency() }})</mat-label>
            <input
              matInput
              type="number"
              step="0.0001"
              min="0.0001"
              inputmode="decimal"
              [(ngModel)]="exchangeRate"
              name="fxRateInput"
              placeholder="e.g. 3.75"
              required />
          </mat-form-field>

          <div class="calculated-preview">
            Target Receives:
            <strong>{{ targetCurrency() }} {{ estimatedTargetAmount() | number:'1.2-2' }}</strong>
          </div>
        </div>
      }

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Note (Optional)</mat-label>
        <input matInput [(ngModel)]="note" name="noteInput" placeholder="Reason for transfer" />
      </mat-form-field>

      @if (errorMessage()) {
        <div class="error-banner">{{ errorMessage() }}</div>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="cancel()">Cancel</button>
      <button
        mat-flat-button
        color="primary"
        type="button"
        [disabled]="isSubmitting() || !isValid()"
        (click)="submit()">
        {{ isSubmitting() ? 'Transferring...' : 'Confirm Transfer' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width {
      width: 100%;
      margin-bottom: 8px;
    }
    .fx-section {
      background: #f7f9fc;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 12px;
      margin-bottom: 12px;
    }
    .fx-rate-field {
      width: 100%;
      margin-bottom: 4px;
    }
    .calculated-preview {
      font-size: 0.95rem;
      color: #1e293b;
      margin-top: 4px;
    }
    .error-banner {
      color: #b00020;
      font-size: 0.85rem;
      margin-top: 8px;
    }
  `]
})
export class TransferModalComponent {
  private dialogRef = inject(MatDialogRef<TransferModalComponent>);
  private budgetService = inject(BudgetService);

  sourceBudgetId = '';
  targetBudgetId = '';
  sourceAmount: number | null = null;
  exchangeRate: number | null = null;
  note = '';

  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  constructor(@Inject(MAT_DIALOG_DATA) public data: TransferModalData) {
    if (data.defaultSourceBudgetId) {
      this.sourceBudgetId = data.defaultSourceBudgetId;
    } else if (data.budgets.length > 0) {
      this.sourceBudgetId = data.budgets[0].id;
    }

    const destinations = this.destinationCandidates();
    if (destinations.length > 0) {
      this.targetBudgetId = destinations[0].id;
    }
    this.updateDefaultFx();
  }

  destinationCandidates(): Budget[] {
    return this.data.budgets.filter(b => b.id !== this.sourceBudgetId);
  }

  onBudgetChange(): void {
    if (this.sourceBudgetId === this.targetBudgetId) {
      const candidates = this.destinationCandidates();
      this.targetBudgetId = candidates.length > 0 ? candidates[0].id : '';
    }
    this.updateDefaultFx();
  }

  sourceCurrency(): string {
    const b = this.data.budgets.find(b => b.id === this.sourceBudgetId);
    return b ? b.currency : '';
  }

  targetCurrency(): string {
    const b = this.data.budgets.find(b => b.id === this.targetBudgetId);
    return b ? b.currency : '';
  }

  isCrossCurrency(): boolean {
    const s = this.sourceCurrency();
    const t = this.targetCurrency();
    return s !== '' && t !== '' && s !== t;
  }

  private updateDefaultFx(): void {
    if (this.isCrossCurrency()) {
      if (!this.exchangeRate || this.exchangeRate <= 0) {
        // reasonable default e.g. 1.0 or leave null
        this.exchangeRate = 1.0;
      }
    } else {
      this.exchangeRate = null;
    }
  }

  estimatedTargetAmount(): number {
    if (!this.sourceAmount || this.sourceAmount <= 0) return 0;
    if (!this.isCrossCurrency()) return this.sourceAmount;
    const rate = this.exchangeRate || 1.0;
    return Math.round(this.sourceAmount * rate * 100) / 100;
  }

  isValid(): boolean {
    if (!this.sourceBudgetId || !this.targetBudgetId) return false;
    if (this.sourceBudgetId === this.targetBudgetId) return false;
    if (!this.sourceAmount || this.sourceAmount <= 0) return false;
    if (this.isCrossCurrency() && (!this.exchangeRate || this.exchangeRate <= 0)) return false;
    return true;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  submit(): void {
    if (!this.isValid()) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const request: TransferRequest = {
      sourceBudgetId: this.sourceBudgetId,
      targetBudgetId: this.targetBudgetId,
      sourceAmountCents: Math.round(this.sourceAmount! * 100),
      exchangeRate: this.isCrossCurrency() ? this.exchangeRate! : undefined,
      note: this.note.trim() || undefined
    };

    this.budgetService.transferFunds(request).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.dialogRef.close(res);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.message || 'Transfer failed. Please check funds.');
      }
    });
  }
}

