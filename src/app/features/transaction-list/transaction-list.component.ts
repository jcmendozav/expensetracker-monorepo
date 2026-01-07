import { CommonModule } from '@angular/common';
import { TransactionService } from '../../core/services/transaction.service';
import { Transaction } from '../../core/models/transaction.model';
import { Component, OnInit, inject, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transaction-list.component.html',
  styleUrls: ['./transaction-list.component.scss']
})
export class TransactionListComponent implements OnInit {
  private transactionService = inject(TransactionService);
  transactions: Transaction[] = [];
  @Output() edit = new EventEmitter<Transaction>();
  isLoading = true;

  ngOnInit() {
    this.loadTransactions();
  }
  loadTransactions() {
    // Reset to true if you reload data manually later
    this.isLoading = true;

    this.transactionService.getTransactions().subscribe({
      next: (data) => {
        this.transactions = data;
        // 2. Turn off loading when data arrives
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading transactions', err);
        // 3. Turn off loading even if there is an error (so it doesn't spin forever)
        this.isLoading = false;
      }
    });
  }

  onDelete(id: string | undefined) {
    if (!id) return;
    if (confirm('Are you sure you want to delete this transaction?')) {
      this.transactionService.deleteTransaction(id).subscribe(() => {
        // Refresh the list after deleting
        this.transactions = this.transactions.filter(t => t.id !== id);
      });
    }
  }
  onEdit(transaction: Transaction) {
    this.edit.emit(transaction);
  }
}