import { Component, OnInit, inject, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TransactionService } from '../../core/services/transaction.service';
import { TransactionListComponent } from '../transaction-list/transaction-list.component';
import { TransactionFormComponent } from '../transaction-form/transaction-form.component';
import { Transaction } from '../../core/models/transaction.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, TransactionListComponent, TransactionFormComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  private transactionService = inject(TransactionService);
  @ViewChild(TransactionListComponent) transactionList!: TransactionListComponent;
  @ViewChild(TransactionFormComponent) transactionForm!: TransactionFormComponent;

  totalSpent: number = 0;
  isLoading: boolean = true;

  ngOnInit() {
    this.transactionService.getDashboardSummary().subscribe({
      next: (data) => {
        this.totalSpent = data.totalSpent;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load dashboard stats', err);
        this.isLoading = false;
      }
    });
  }

  // Add this method inside your class
  refreshSummary() {
    this.isLoading = true; // Optional: show spinner briefly
    this.transactionService.getDashboardSummary().subscribe(data => {
      this.totalSpent = data.totalSpent;
      this.isLoading = false;
    });
  }

  onTransactionAdded() {
    // Refresh the list
    this.transactionList.loadTransactions();
    // Refresh the stats
    this.refreshSummary();
  }

  onEditTransaction(transaction: Transaction) {
    // Scroll to top so user sees the form
    window.scrollTo({ top: 0, behavior: 'smooth' });

    // Call the method we created in Step 2
    this.transactionForm.setFormData(transaction);
  }
}