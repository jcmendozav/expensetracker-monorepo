import { CommonModule } from '@angular/common';
import { TransactionService } from '../../core/services/transaction.service';
import { Transaction } from '../../core/models/transaction.model';
import { Component, inject, output, signal, ChangeDetectionStrategy, effect, input } from '@angular/core';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transaction-list.component.html',
  styleUrls: ['./transaction-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionListComponent {
  private transactionService = inject(TransactionService);
  transactions = signal<Transaction[]>([]);
  isLoading = signal(true);
  edit = output<Transaction>();
  refresh = input(0);

  constructor() {
    effect(async () => {
      this.refresh(); // re-run when refresh changes
      this.isLoading.set(true);
      try {
        const data = await firstValueFrom(this.transactionService.getTransactions());
        this.transactions.set(data);
      } catch (err) {
        console.error('Error loading transactions', err);
      } finally {
        this.isLoading.set(false);
      }
    });
  }

  async onDelete(id: string | undefined) {
    if (!id) return;
    if (confirm('Are you sure you want to delete this transaction?')) {
      try {
        await firstValueFrom(this.transactionService.deleteTransaction(id));
        this.transactions.update(transactions => transactions.filter(t => t.id !== id));
      } catch (err) {
        console.error('Delete failed', err);
      }
    }
  }

  onEdit(transaction: Transaction) {
    this.edit.emit(transaction);
  }
}