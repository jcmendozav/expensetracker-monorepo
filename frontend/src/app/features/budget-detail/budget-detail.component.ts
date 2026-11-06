import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DecimalPipe, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatBottomSheet, MatBottomSheetModule } from '@angular/material/bottom-sheet';
import { BudgetService } from '../../core/services/budget.service';
import type { Budget } from '../../core/models/budget.model';
import type { LedgerTransaction } from '../../core/models/ledger.model';
import { ExpenseEntrySheetComponent } from '../expense-entry-sheet/expense-entry-sheet.component';
import { TransactionEditSheetComponent } from '../transaction-edit-sheet/transaction-edit-sheet.component';

@Component({
  selector: 'app-budget-detail',
  imports: [
    DecimalPipe,
    DatePipe,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatProgressBarModule,
    MatBottomSheetModule
  ],
  template: `
    <div class="page-container">
      @if (isLoading()) {
        <div class="loading-state">Loading budget details...</div>
      } @else if (!budget()) {
        <mat-card appearance="outlined" class="detail-card">
          <mat-card-content>
            <p>Budget not found.</p>
            <a mat-flat-button color="primary" routerLink="/dashboard">Back to dashboard</a>
          </mat-card-content>
        </mat-card>
      } @else {
        <div class="detail-card">
          <!-- ── Header Bar ── -->
          <div class="top-nav">
            <a mat-button routerLink="/dashboard">← Back to dashboard</a>
            <a mat-stroked-button [routerLink]="['/budgets', budget()!.id, 'settings']">
              ⚙️ Settings
            </a>
          </div>

          <!-- ── Budget Header & Liquidity ── -->
          <mat-card appearance="outlined" class="overview-card">
            <mat-card-header>
              <div class="title-row">
                <h1>{{ budget()!.title }}</h1>
                <mat-chip-set>
                  @if (budget()!.status === 'LOW_BALANCE') {
                    <mat-chip class="chip-warn">⚠️ Low balance</mat-chip>
                  } @else if (budget()!.status === 'DEPLETED') {
                    <mat-chip class="chip-danger">Depleted</mat-chip>
                  } @else if (budget()!.status === 'ARCHIVED') {
                    <mat-chip>Archived</mat-chip>
                  } @else {
                    <mat-chip class="chip-healthy">Healthy</mat-chip>
                  }
                </mat-chip-set>
              </div>
            </mat-card-header>

            <mat-card-content>
              <div class="balance-display">
                <div class="balance-item">
                  <span class="label">Remaining balance</span>
                  <span class="value">{{ budget()!.currency }} {{ (budget()!.currentBalanceCents / 100) | number:'1.2-2' }}</span>
                </div>
                <div class="balance-item secondary">
                  <span class="label">Initial allocation</span>
                  <span class="value">{{ budget()!.currency }} {{ (budget()!.initialAmountCents / 100) | number:'1.2-2' }}</span>
                </div>
              </div>

              <mat-progress-bar
                mode="determinate"
                [value]="progressPercentage()"
                [color]="budget()!.status === 'LOW_BALANCE' ? 'warn' : 'primary'">
              </mat-progress-bar>
              <div class="progress-label">
                <span>{{ progressPercentage() }}% remaining</span>
              </div>

              <div class="action-bar">
                <button mat-flat-button color="primary" (click)="openExpenseEntry()">
                  + Log transaction
                </button>
              </div>
            </mat-card-content>
          </mat-card>

          <!-- ── Immutable Ledger History ── -->
          <section class="ledger-section">
            <div class="ledger-header">
              <h2>Immutable ledger history</h2>
            </div>

            @if (transactions().length === 0) {
              <p class="empty-ledger">No transactions recorded for this budget yet.</p>
            } @else {
              <div class="ledger-table-container">
                <table class="ledger-table">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Type</th>
                      <th>Category / Note</th>
                      <th>Logged by</th>
                      <th>Amount</th>
                      <th>Balance after</th>
                      <th>Status</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (tx of transactions(); track tx.id) {
                      <tr [class.voided-row]="tx.status === 'VOIDED'" (click)="openEditSheet(tx)">
                        <td>{{ tx.transactionDate | date:'shortDate' }} {{ tx.transactionDate | date:'shortTime' }}</td>
                        <td>
                          <span class="tx-type-tag" [class.type-spending]="tx.type === 'SPENDING'" [class.type-income]="tx.type === 'INCOME'">
                            {{ tx.type }}
                          </span>
                        </td>
                        <td>
                          <strong>{{ tx.category }}</strong>
                          @if (tx.note) {
                            <div class="tx-note">{{ tx.note }}</div>
                          }
                        </td>
                        <td>{{ tx.createdByName }}</td>
                        <td class="amount-cell" [class.neg]="tx.type === 'SPENDING' || tx.type === 'TRANSFER_OUT'" [class.pos]="tx.type === 'INCOME' || tx.type === 'TRANSFER_IN'">
                          {{ tx.type === 'SPENDING' || tx.type === 'TRANSFER_OUT' ? '-' : '+' }}{{ (tx.amountCents / 100) | number:'1.2-2' }}
                        </td>
                        <td>{{ (tx.balanceAfterCents / 100) | number:'1.2-2' }}</td>
                        <td>
                          <span class="status-badge" [class.status-void]="tx.status === 'VOIDED'">
                            {{ tx.status }}
                          </span>
                        </td>
                        <td>
                          <button
                            mat-button
                            class="edit-btn"
                            type="button"
                            (click)="$event.stopPropagation(); openEditSheet(tx)">
                            Edit
                          </button>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </section>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-container {
      display: flex;
      justify-content: center;
      padding: 24px 16px;
    }
    .detail-card {
      max-width: 900px;
      width: 100%;
    }
    .top-nav {
      display: flex;
      justify-content: space-between;
      margin-bottom: 16px;
    }
    .overview-card {
      padding: 16px;
      margin-bottom: 24px;
    }
    .title-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      width: 100%;
    }
    .title-row h1 {
      margin: 0;
      font-size: 1.4rem;
      font-weight: 700;
    }
    .balance-display {
      display: flex;
      gap: 32px;
      margin: 16px 0;
    }
    .balance-item {
      display: flex;
      flex-direction: column;
    }
    .balance-item .label {
      font-size: 0.8rem;
      color: #64748b;
      text-transform: uppercase;
      font-weight: 600;
    }
    .balance-item .value {
      font-size: 1.5rem;
      font-weight: 700;
      color: #0f172a;
    }
    .balance-item.secondary .value {
      font-size: 1.2rem;
      color: #475569;
    }
    .progress-label {
      display: flex;
      justify-content: flex-end;
      font-size: 0.8rem;
      color: #64748b;
      margin-top: 4px;
    }
    .action-bar {
      margin-top: 16px;
      display: flex;
      gap: 12px;
    }
    .ledger-section h2 {
      font-size: 1.1rem;
      font-weight: 700;
      text-transform: uppercase;
      margin-bottom: 12px;
    }
    .ledger-table-container {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      overflow-x: auto;
    }
    .ledger-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
    }
    .ledger-table th, .ledger-table td {
      padding: 12px 16px;
      border-bottom: 1px solid #f1f5f9;
      font-size: 0.85rem;
    }
    .ledger-table th {
      background: #f8fafc;
      font-weight: 600;
      color: #475569;
      text-transform: uppercase;
      font-size: 0.75rem;
    }
    .ledger-table tr {
      cursor: pointer;
    }
    .ledger-table tr:hover {
      background: #f8fafc;
    }
    .voided-row {
      opacity: 0.55;
      text-decoration: line-through;
    }
    .tx-note {
      font-size: 0.75rem;
      color: #64748b;
    }
    .amount-cell {
      font-weight: 600;
    }
    .amount-cell.neg {
      color: #dc2626;
    }
    .amount-cell.pos {
      color: #16a34a;
    }
    .tx-type-tag {
      font-size: 0.7rem;
      font-weight: 600;
      padding: 2px 6px;
      border-radius: 4px;
      background: #e2e8f0;
    }
    .status-badge {
      font-size: 0.7rem;
      padding: 2px 6px;
      border-radius: 4px;
      background: #dcfce7;
      color: #15803d;
    }
    .status-badge.status-void {
      background: #fee2e2;
      color: #b91c1c;
    }
    .chip-warn {
      background: #fef3c7 !important;
      color: #92400e !important;
    }
    .chip-danger {
      background: #fee2e2 !important;
      color: #991b1b !important;
    }
    .chip-healthy {
      background: #dcfce7 !important;
      color: #166534 !important;
    }
    .empty-ledger {
      color: #64748b;
      padding: 24px 0;
    }
    .loading-state {
      padding: 40px;
      text-align: center;
      color: #64748b;
    }
  `]
})
export class BudgetDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly budgetService = inject(BudgetService);
  private readonly bottomSheet = inject(MatBottomSheet);

  readonly budget = signal<Budget | null>(null);
  readonly transactions = signal<LedgerTransaction[]>([]);
  readonly isLoading = signal(true);

  budgetId = '';

  ngOnInit(): void {
    this.budgetId = this.route.snapshot.paramMap.get('id') ?? '';
    if (this.budgetId) {
      this.loadData();
    } else {
      this.isLoading.set(false);
    }
  }

  loadData(): void {
    this.isLoading.set(true);
    this.budgetService.getBudget(this.budgetId).subscribe({
      next: (b) => {
        this.budget.set(b);
        this.loadTransactions();
      },
      error: () => {
        this.budget.set(null);
        this.isLoading.set(false);
      }
    });
  }

  loadTransactions(): void {
    this.budgetService.listTransactions(this.budgetId).subscribe({
      next: (txs) => {
        this.transactions.set(txs);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  progressPercentage(): number {
    const b = this.budget();
    if (!b || !b.initialAmountCents || b.initialAmountCents <= 0) return 0;
    const pct = (b.currentBalanceCents / b.initialAmountCents) * 100;
    return Math.max(0, Math.min(100, Math.round(pct)));
  }

  openExpenseEntry(): void {
    const b = this.budget();
    if (!b) return;

    const ref = this.bottomSheet.open(ExpenseEntrySheetComponent, {
      data: {
        budgets: [b],
        defaultBudgetId: b.id
      }
    });
    ref.afterDismissed().subscribe((res) => {
      if (res) this.loadData();
    });
  }

  openEditSheet(tx: LedgerTransaction): void {
    const b = this.budget();
    if (!b) return;

    const ref = this.bottomSheet.open(TransactionEditSheetComponent, {
      data: {
        transaction: tx,
        budgetId: b.id,
        currency: b.currency
      }
    });
    ref.afterDismissed().subscribe((res) => {
      if (res) this.loadData();
    });
  }
}
