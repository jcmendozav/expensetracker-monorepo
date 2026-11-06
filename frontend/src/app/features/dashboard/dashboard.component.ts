import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatBottomSheet, MatBottomSheetModule } from '@angular/material/bottom-sheet';
import { MatBadgeModule } from '@angular/material/badge';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { HouseholdService } from '../../core/services/household.service';
import { BudgetService } from '../../core/services/budget.service';
import { NotificationService } from '../../core/services/notification.service';
import { Household, HouseholdInvitation } from '../../core/models/household.model';
import { Budget, LiquiditySummary } from '../../core/models/budget.model';
import { LedgerTransaction } from '../../core/models/ledger.model';
import { AppNotification } from '../../core/models/notification.model';
import { ExpenseEntrySheetComponent } from '../expense-entry-sheet/expense-entry-sheet.component';
import { TransferModalComponent } from '../transfer-modal/transfer-modal.component';
import { BudgetCreateDialogComponent } from '../budget-dialog/budget-create-dialog.component';
import { HouseholdCreateDialogComponent } from '../household-dialog/household-create-dialog.component';
import { ActivityDrawerComponent } from '../activity-drawer/activity-drawer.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatProgressBarModule,
    MatChipsModule,
    MatDialogModule,
    MatBottomSheetModule,
    MatBadgeModule,
    ActivityDrawerComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  authService = inject(AuthService);
  private householdService = inject(HouseholdService);
  private budgetService = inject(BudgetService);
  private notificationService = inject(NotificationService);
  private dialog = inject(MatDialog);
  private bottomSheet = inject(MatBottomSheet);

  household = signal<Household | null>(null);
  budgets = signal<Budget[]>([]);
  summary = signal<LiquiditySummary | null>(null);
  pendingInvitations = signal<HouseholdInvitation[]>([]);
  notifications = signal<AppNotification[]>([]);
  selectedBudgetId = signal<string | null>(null);
  selectedBudgetLedger = signal<LedgerTransaction[]>([]);

  isLoading = signal(true);
  isActivityDrawerOpen = signal(false);

  ngOnInit(): void {
    this.loadAll();
  }

  async loadAll(): Promise<void> {
    this.isLoading.set(true);
    const user = this.authService.user();
    const userEmail = user?.email || undefined;

    try {
      // 1. Check for pending invitations
      if (userEmail) {
        const invites = await firstValueFrom(this.householdService.getPendingInvitations(userEmail));
        this.pendingInvitations.set(invites);
      }

      // 2. Fetch current active household
      let hh: Household | null = null;
      try {
        hh = await firstValueFrom(this.householdService.getCurrentHousehold());
        this.household.set(hh);
      } catch (err: any) {
        if (err?.status === 404) {
          this.household.set(null);
        } else {
          console.error('Error fetching household', err);
        }
      }

      // 3. If household exists, fetch budgets & liquidity
      if (hh) {
        const [budgetsRes, summaryRes, notifsRes] = await Promise.all([
          firstValueFrom(this.budgetService.listBudgets(hh.id)),
          firstValueFrom(this.budgetService.getSummary(hh.id)),
          firstValueFrom(this.notificationService.getFeed(20))
        ]);

        this.budgets.set(budgetsRes.items);
        this.summary.set(summaryRes);
        this.notifications.set(notifsRes.items);

        // If a budget was selected, refresh its ledger
        if (this.selectedBudgetId()) {
          this.loadLedger(this.selectedBudgetId()!);
        } else if (budgetsRes.items.length > 0) {
          this.selectBudget(budgetsRes.items[0].id);
        }
      }
    } catch (e) {
      console.error('Failed to load dashboard data', e);
    } finally {
      this.isLoading.set(false);
    }
  }

  unreadCount(): number {
    return this.notifications().filter(n => !n.isRead).length;
  }

  toggleActivityDrawer(): void {
    this.isActivityDrawerOpen.update(v => !v);
  }

  openHouseholdOnboarding(): void {
    const ref = this.dialog.open(HouseholdCreateDialogComponent, { width: '450px' });
    ref.afterClosed().subscribe((res) => {
      if (res) this.loadAll();
    });
  }

  openCreateBudget(): void {
    const hh = this.household();
    if (!hh) return;
    const ref = this.dialog.open(BudgetCreateDialogComponent, {
      width: '450px',
      data: {
        householdId: hh.id,
        availableCurrencies: hh.baseCurrencies || ['PEN', 'USD']
      }
    });
    ref.afterClosed().subscribe((res) => {
      if (res) this.loadAll();
    });
  }

  openExpenseEntry(): void {
    const sheetRef = this.bottomSheet.open(ExpenseEntrySheetComponent, {
      data: {
        budgets: this.budgets(),
        defaultBudgetId: this.selectedBudgetId() || undefined
      }
    });
    sheetRef.afterDismissed().subscribe((res) => {
      if (res) this.loadAll();
    });
  }

  openTransfer(): void {
    const dialogRef = this.dialog.open(TransferModalComponent, {
      width: '500px',
      data: {
        budgets: this.budgets(),
        defaultSourceBudgetId: this.selectedBudgetId() || undefined
      }
    });
    dialogRef.afterClosed().subscribe((res) => {
      if (res) this.loadAll();
    });
  }

  selectBudget(budgetId: string): void {
    this.selectedBudgetId.set(budgetId);
    this.loadLedger(budgetId);
  }

  async loadLedger(budgetId: string): Promise<void> {
    try {
      const items = await firstValueFrom(this.budgetService.listTransactions(budgetId));
      this.selectedBudgetLedger.set(items);
    } catch (e) {
      console.error('Failed to load ledger', e);
    }
  }

  async voidTransaction(budgetId: string, txId: string): Promise<void> {
    try {
      await firstValueFrom(this.budgetService.voidTransaction(budgetId, txId));
      this.loadAll();
    } catch (e) {
      console.error('Failed to void transaction', e);
    }
  }

  async acceptInvitation(invId: string): Promise<void> {
    const user = this.authService.user();
    try {
      await firstValueFrom(this.householdService.acceptInvitation(invId, user?.email || undefined, user?.displayName || undefined));
      this.loadAll();
    } catch (e) {
      console.error('Failed to accept invite', e);
    }
  }

  async declineInvitation(invId: string): Promise<void> {
    try {
      await firstValueFrom(this.householdService.declineInvitation(invId));
      this.loadAll();
    } catch (e) {
      console.error('Failed to decline invite', e);
    }
  }

  getBudgetProgress(budget: Budget): number {
    if (!budget.initialAmountCents || budget.initialAmountCents <= 0) return 0;
    const pct = (budget.currentBalanceCents / budget.initialAmountCents) * 100;
    return Math.max(0, Math.min(100, Math.round(pct)));
  }

  formatCurrency(cents: number, curr: string): string {
    const major = (cents / 100).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return `${curr} ${major}`;
  }

  getCurrenciesList(): string[] {
    const s = this.summary();
    return s ? Object.keys(s.balancesByCurrency) : [];
  }

  selectedBudgetTitle(): string {
    const b = this.budgets().find(item => item.id === this.selectedBudgetId());
    return b ? b.title : '';
  }
}
