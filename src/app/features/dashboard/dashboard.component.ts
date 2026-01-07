import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TransactionService } from '../../core/services/transaction.service';
import { TransactionListComponent } from '../transaction-list/transaction-list.component';
import { TransactionFormComponent } from '../transaction-form/transaction-form.component';
import { Transaction } from '../../core/models/transaction.model';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, TransactionListComponent, TransactionFormComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  private transactionService = inject(TransactionService);

  totalSpent = signal(0);
  isLoading = signal(true);
  refresh = signal(0);
  selectedTransaction = signal<Transaction | null>(null);

  constructor() {
    effect(async () => {
      this.refresh(); // re-run when refresh changes
      this.isLoading.set(true);
      try {
        const data = await firstValueFrom(this.transactionService.getDashboardSummary());
        this.totalSpent.set(data.totalSpent);
      } catch (err) {
        console.error('Failed to load dashboard stats', err);
      } finally {
        this.isLoading.set(false);
      }
    });
  }

  refreshSummary() {
    this.refresh.update(v => v + 1);
  }

  onTransactionAdded() {
    // Refresh the stats, which will also refresh the list
    this.refreshSummary();
    // Reset the selected transaction
    this.selectedTransaction.set(null);
  }

  onEditTransaction(transaction: Transaction) {
    // Scroll to top so user sees the form
    window.scrollTo({ top: 0, behavior: 'smooth' });
    // Set the selected transaction
    this.selectedTransaction.set(transaction);
  }
}