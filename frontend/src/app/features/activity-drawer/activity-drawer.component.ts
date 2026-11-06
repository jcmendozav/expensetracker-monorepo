import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AppNotification } from '../../core/models/notification.model';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-activity-drawer',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="drawer-panel">
      <div class="drawer-header">
        <h3>Activity & Notifications</h3>
        <div class="header-actions">
          <button mat-button class="mark-all-btn" (click)="markAllRead()">Mark All Read</button>
          <button mat-icon-button (click)="close.emit()" aria-label="Close activity drawer">✕</button>
        </div>
      </div>

      <div class="drawer-content">
        @if (notifications.length === 0) {
          <div class="empty-state">
            <span class="empty-icon">🔔</span>
            <p>No recent activity or notifications.</p>
          </div>
        } @else {
          <div class="notification-list">
            @for (item of notifications; track item.id) {
              <div class="notification-item" [class.unread]="!item.isRead">
                <div class="item-header">
                  <span class="item-type-badge">{{ item.title }}</span>
                  <span class="item-date">{{ item.createdAt | date:'shortTime' }}</span>
                </div>
                <p class="item-msg">{{ item.message }}</p>
                @if (!item.isRead) {
                  <button mat-button class="read-btn" (click)="markRead(item)">✓ Mark read</button>
                }
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .drawer-panel {
      width: 320px;
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
      background: #eff6ff;
      border-color: #bfdbfe;
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
    .read-btn {
      font-size: 0.75rem;
      padding: 0 8px;
      line-height: 24px;
      height: 24px;
    }
  `]
})
export class ActivityDrawerComponent {
  private notificationService = inject(NotificationService);

  @Input() notifications: AppNotification[] = [];
  @Output() close = new EventEmitter<void>();
  @Output() refreshed = new EventEmitter<void>();

  markRead(item: AppNotification): void {
    this.notificationService.markAsRead(item.id).subscribe(() => {
      item.isRead = true;
      this.refreshed.emit();
    });
  }

  markAllRead(): void {
    this.notificationService.markAllAsRead().subscribe(() => {
      this.notifications.forEach(n => n.isRead = true);
      this.refreshed.emit();
    });
  }
}

