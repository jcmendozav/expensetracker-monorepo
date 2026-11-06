import { Component, inject, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import type { AppNotification } from '../../core/models/notification.model';
import type { HouseholdInvitation } from '../../core/models/household.model';
import { NotificationService } from '../../core/services/notification.service';
import { HouseholdService } from '../../core/services/household.service';

@Component({
  selector: 'app-activity-drawer',
  imports: [DatePipe, MatButtonModule, MatIconModule],
  template: `
    <div class="drawer-panel" role="region" aria-label="Activity and notifications">
      <div class="drawer-header">
        <h3>Activity & notifications</h3>
        <div class="header-actions">
          <button mat-button class="mark-all-btn" type="button" (click)="markAllRead()">
            Mark all read
          </button>
          <button mat-icon-button type="button" (click)="close.emit()" aria-label="Close activity drawer">
            ✕
          </button>
        </div>
      </div>

      <div class="drawer-content">
        @if (invitations().length > 0) {
          <div class="section-title">Pending invitations</div>
          <div class="invitations-group">
            @for (inv of invitations(); track inv.id) {
              <div class="invitation-card">
                <div class="inv-head">
                  <span class="inv-badge">🏠 Household invitation</span>
                  <span class="inv-date">{{ inv.createdAt | date:'shortTime' }}</span>
                </div>
                <p class="inv-msg">
                  {{ inv.inviterName }} invited you to join <strong>{{ inv.householdName }}</strong> as Co-Owner.
                </p>
                <div class="inv-actions">
                  <button
                    mat-flat-button
                    color="primary"
                    class="action-btn"
                    type="button"
                    [disabled]="processingInviteId() === inv.id"
                    (click)="acceptInvite(inv.id)">
                    Accept
                  </button>
                  <button
                    mat-stroked-button
                    color="warn"
                    class="action-btn"
                    type="button"
                    [disabled]="processingInviteId() === inv.id"
                    (click)="declineInvite(inv.id)">
                    Decline
                  </button>
                </div>
              </div>
            }
          </div>
        }

        <div class="section-title">Recent activity</div>
        @if (notifications().length === 0) {
          <div class="empty-state">
            <span class="empty-icon" aria-hidden="true">🔔</span>
            <p>No recent activity or notifications.</p>
          </div>
        } @else {
          <div class="notification-list">
            @for (item of notifications(); track item.id) {
              <div class="notification-item" [class.unread]="!item.isRead">
                <div class="item-header">
                  <span class="item-type-badge">{{ item.title }}</span>
                  <span class="item-date">{{ item.createdAt | date:'shortTime' }}</span>
                </div>
                <p class="item-msg">{{ item.message }}</p>

                <div class="item-actions">
                  @if (item.budgetId) {
                    <button
                      mat-button
                      class="view-budget-btn"
                      type="button"
                      (click)="viewBudget(item.budgetId)">
                      ↗ View budget
                    </button>
                  }
                  @if (!item.isRead) {
                    <button
                      mat-button
                      class="read-btn"
                      type="button"
                      (click)="markRead(item)">
                      ✓ Mark read
                    </button>
                  }
                </div>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .drawer-panel {
      width: 340px;
      height: 100%;
      background: #ffffff;
      border-left: 1px solid #e2e8f0;
      box-shadow: -4px 0 16px rgba(0, 0, 0, 0.08);
      display: flex;
      flex-direction: column;
    }
    .drawer-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      border-bottom: 1px solid #e2e8f0;
    }
    .drawer-header h3 {
      margin: 0;
      font-size: 1rem;
      font-weight: 600;
    }
    .mark-all-btn {
      font-size: 0.8rem;
    }
    .drawer-content {
      flex: 1;
      overflow-y: auto;
      padding: 12px;
    }
    .section-title {
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      color: #64748b;
      margin: 8px 0 6px;
      letter-spacing: 0.05em;
    }
    .invitations-group {
      margin-bottom: 16px;
    }
    .invitation-card {
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      border-radius: 8px;
      padding: 12px;
      margin-bottom: 8px;
    }
    .inv-head {
      display: flex;
      justify-content: space-between;
      font-size: 0.75rem;
      margin-bottom: 4px;
    }
    .inv-badge {
      font-weight: 600;
      color: #1d4ed8;
    }
    .inv-date {
      color: #64748b;
    }
    .inv-msg {
      font-size: 0.85rem;
      color: #1e293b;
      margin: 0 0 8px;
    }
    .inv-actions {
      display: flex;
      gap: 8px;
    }
    .action-btn {
      font-size: 0.75rem;
      height: 28px;
      line-height: 28px;
      padding: 0 12px;
    }
    .empty-state {
      text-align: center;
      padding: 40px 16px;
      color: #94a3b8;
    }
    .empty-icon {
      font-size: 2rem;
      display: block;
      margin-bottom: 8px;
    }
    .notification-item {
      padding: 12px;
      border-radius: 8px;
      background: #f8fafc;
      margin-bottom: 8px;
      border: 1px solid #e2e8f0;
    }
    .notification-item.unread {
      background: #f0fdf4;
      border-color: #bbf7d0;
    }
    .item-header {
      display: flex;
      justify-content: space-between;
      font-size: 0.75rem;
      color: #64748b;
      margin-bottom: 4px;
    }
    .item-type-badge {
      font-weight: 600;
      color: #1e293b;
    }
    .item-msg {
      font-size: 0.85rem;
      color: #334155;
      margin: 0 0 6px;
    }
    .item-actions {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .view-budget-btn {
      font-size: 0.75rem;
      padding: 0 6px;
      height: 24px;
      line-height: 24px;
      color: #2563eb;
    }
    .read-btn {
      font-size: 0.75rem;
      padding: 0 8px;
      line-height: 24px;
      height: 24px;
    }
  `]
})
export class ActivityDrawerComponent {
  private readonly notificationService = inject(NotificationService);
  private readonly householdService = inject(HouseholdService);
  private readonly router = inject(Router);

  readonly notifications = input<AppNotification[]>([]);
  readonly invitations = input<HouseholdInvitation[]>([]);

  readonly close = output<void>();
  readonly refreshed = output<void>();

  readonly processingInviteId = signal<string | null>(null);

  acceptInvite(invitationId: string): void {
    this.processingInviteId.set(invitationId);
    this.householdService.acceptInvitation(invitationId).subscribe({
      next: () => {
        this.processingInviteId.set(null);
        this.refreshed.emit();
      },
      error: () => {
        this.processingInviteId.set(null);
      }
    });
  }

  declineInvite(invitationId: string): void {
    this.processingInviteId.set(invitationId);
    this.householdService.declineInvitation(invitationId).subscribe({
      next: () => {
        this.processingInviteId.set(null);
        this.refreshed.emit();
      },
      error: () => {
        this.processingInviteId.set(null);
      }
    });
  }

  viewBudget(budgetId: string): void {
    this.close.emit();
    this.router.navigate(['/budgets', budgetId]);
  }

  markRead(item: AppNotification): void {
    this.notificationService.markAsRead(item.id).subscribe({
      next: () => {
        item.isRead = true;
        this.refreshed.emit();
      }
    });
  }

  markAllRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        for (const n of this.notifications()) {
          n.isRead = true;
        }
        this.refreshed.emit();
      }
    });
  }
}
