import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { MAT_BOTTOM_SHEET_DATA, MatBottomSheetRef } from '@angular/material/bottom-sheet';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { firstValueFrom } from 'rxjs';
import { BudgetService } from '../../core/services/budget.service';
import type { LedgerTransaction } from '../../core/models/ledger.model';

export interface TransactionEditSheetData {
  transaction: LedgerTransaction;
  budgetId: string;
  currency: string;
}

@Component({
  selector: 'app-transaction-edit-sheet',
  imports: [
    FormsModule,
    DecimalPipe,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <div class="sheet-container">
      <div class="drag-handle" aria-hidden="true"></div>

      <div class="sheet-header">
        <h2>Edit Transaction</h2>
        <span class="status-badge" [class.voided]="data.transaction.status === 'VOIDED'">
          {{ data.transaction.status }}
        </span>
      </div>

      <div class="transaction-summary">
        <div class="amount-display">
          <span class="type-indicator" [class.spending]="data.transaction.type === 'SPENDING'">
            {{ data.transaction.type === 'SPENDING' ? '-' : '+' }}
          </span>
          <span class="currency">{{ data.currency }}</span>
          <span class="amount">{{ (data.transaction.amountCents / 100) | number:'1.2-2' }}</span>
        </div>
        <div class="meta-info">
          <span>Logged by {{ data.transaction.createdByName }}</span>
        </div>
      </div>

      <form (ngSubmit)="saveMetadata()">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Category</mat-label>
          <input
            matInput
            [(ngModel)]="category"
            name="category"
            required
            [disabled]="isSaving() || data.transaction.status === 'VOIDED'"
            placeholder="e.g. Groceries, Utilities" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Note</mat-label>
          <input
            matInput
            [(ngModel)]="note"
            name="note"
            [disabled]="isSaving() || data.transaction.status === 'VOIDED'"
            placeholder="Add details" />
        </mat-form-field>

        @if (errorMessage()) {
          <div class="error-banner" role="alert">{{ errorMessage() }}</div>
        }

        <div class="actions">
          @if (data.transaction.status !== 'VOIDED') {
            <button
              mat-stroked-button
              color="warn"
              type="button"
              [disabled]="isSaving() || isVoiding()"
              (click)="voidTransaction()">
              {{ isVoiding() ? 'Voiding...' : 'Void & Refund' }}
            </button>
          }

          <div class="spacer"></div>

          <button mat-button type="button" (click)="dismiss()">Close</button>

          @if (data.transaction.status !== 'VOIDED') {
            <button
              mat-flat-button
              color="primary"
              type="submit"
              [disabled]="isSaving() || isVoiding() || !category.trim()">
              {{ isSaving() ? 'Saving...' : 'Save Changes' }}
            </button>
          }
        </div>
      </form>
    </div>
  `,
  styles: [`
    .sheet-container {
      padding: 16px 20px 24px;
      max-width: 520px;
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
      margin-bottom: 16px;
    }
    .sheet-header h2 {
      margin: 0;
      font-size: 1.25rem;
      font-weight: 600;
    }
    .status-badge {
      font-size: 0.75rem;
      font-weight: 700;
      padding: 4px 8px;
      border-radius: 4px;
      background: #dcfce7;
      color: #15803d;
      text-transform: uppercase;
    }
    .status-badge.voided {
      background: #fee2e2;
      color: #b91c1c;
    }
    .transaction-summary {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 12px 16px;
      margin-bottom: 20px;
    }
    .amount-display {
      font-size: 1.5rem;
      font-weight: 700;
      color: #0f172a;
    }
    .amount-display .spending {
      color: #dc2626;
    }
    .meta-info {
      font-size: 0.85rem;
      color: #64748b;
      margin-top: 4px;
    }
    .full-width {
      width: 100%;
      margin-bottom: 12px;
    }
    .error-banner {
      background: #fee2e2;
      color: #b91c1c;
      padding: 8px 12px;
      border-radius: 6px;
      font-size: 0.85rem;
      margin-bottom: 16px;
    }
    .actions {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 12px;
    }
    .spacer {
      flex: 1;
    }
  `]
})
export class TransactionEditSheetComponent {
  readonly data = inject<TransactionEditSheetData>(MAT_BOTTOM_SHEET_DATA);
  private readonly bottomSheetRef = inject(MatBottomSheetRef<TransactionEditSheetComponent>);
  private readonly budgetService = inject(BudgetService);

  category = this.data.transaction.category ?? '';
  note = this.data.transaction.note ?? '';

  readonly isSaving = signal(false);
  readonly isVoiding = signal(false);
  readonly errorMessage = signal<string | null>(null);

  async saveMetadata(): Promise<void> {
    if (!this.category.trim()) {
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set(null);

    try {
      await firstValueFrom(
        this.budgetService.updateTransactionMetadata(this.data.budgetId, this.data.transaction.id, {
          category: this.category.trim(),
          note: this.note.trim() ? this.note.trim() : undefined
        })
      );
      this.bottomSheetRef.dismiss({ updated: true });
    } catch (err: unknown) {
      this.errorMessage.set('Failed to update transaction metadata. Please try again.');
    } finally {
      this.isSaving.set(false);
    }
  }

  async voidTransaction(): Promise<void> {
    const confirmed = window.confirm('Are you sure you want to void this transaction? The amount will be refunded to the budget.');
    if (!confirmed) {
      return;
    }

    this.isVoiding.set(true);
    this.errorMessage.set(null);

    try {
      await firstValueFrom(
        this.budgetService.voidTransaction(this.data.budgetId, this.data.transaction.id)
      );
      this.bottomSheetRef.dismiss({ voided: true });
    } catch (err: unknown) {
      this.errorMessage.set('Failed to void transaction. Please try again.');
    } finally {
      this.isVoiding.set(false);
    }
  }

  dismiss(): void {
    this.bottomSheetRef.dismiss();
  }
}
