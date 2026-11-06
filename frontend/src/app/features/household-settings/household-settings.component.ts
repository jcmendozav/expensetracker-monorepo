import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { HouseholdService } from '../../core/services/household.service';
import { AuthService } from '../../core/services/auth.service';
import type { Household, HouseholdInvitation, UpdateHouseholdRequest } from '../../core/models/household.model';

@Component({
  selector: 'app-household-settings',
  imports: [
    FormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule,
    MatChipsModule
  ],
  template: `
    <div class="page-container">
      @if (isLoading()) {
        <div class="loading-state">Loading household settings...</div>
      } @else if (!household()) {
        <mat-card appearance="outlined" class="settings-card">
          <mat-card-content>
            <p>No active household found.</p>
            <a mat-flat-button color="primary" routerLink="/households/new">Create household</a>
          </mat-card-content>
        </mat-card>
      } @else {
        <mat-card appearance="outlined" class="settings-card">
          <mat-card-header>
            <mat-card-title>Household settings</mat-card-title>
            <mat-card-subtitle>Manage members, currencies, and preferences.</mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <form (ngSubmit)="saveSettings()">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Household name</mat-label>
                <input matInput [(ngModel)]="name" name="hhName" required />
              </mat-form-field>

              <div class="currency-group">
                <label class="group-label">Base currencies:</label>
                <div class="checkbox-row">
                  <mat-checkbox [(ngModel)]="currencies.PEN" name="pen">PEN (Peruvian Sol)</mat-checkbox>
                  <mat-checkbox [(ngModel)]="currencies.USD" name="usd">USD (US Dollar)</mat-checkbox>
                  <mat-checkbox [(ngModel)]="currencies.EUR" name="eur">EUR (Euro)</mat-checkbox>
                  <mat-checkbox [(ngModel)]="currencies.COP" name="cop">COP (Colombian Peso)</mat-checkbox>
                </div>
              </div>

              <div class="members-section">
                <h3>Household co-owners</h3>
                <div class="member-list">
                  @for (member of memberList(); track member.userId) {
                    <div class="member-row">
                      <div class="member-info">
                        <strong>{{ member.displayName || member.email || 'Member' }}</strong>
                        <span class="member-email">{{ member.email }}</span>
                      </div>
                      <mat-chip-set>
                        <mat-chip [class.primary-owner]="member.role === 'PRIMARY_OWNER'">
                          {{ member.role === 'PRIMARY_OWNER' ? 'Primary Owner' : 'Co-Owner' }}
                        </mat-chip>
                      </mat-chip-set>
                    </div>
                  }

                  @for (inv of pendingInvites(); track inv.id) {
                    <div class="member-row pending-row">
                      <div class="member-info">
                        <strong>{{ inv.invitedEmail }}</strong>
                        <span class="member-email">Invited by {{ inv.inviterName }}</span>
                      </div>
                      <div class="invite-actions">
                        <mat-chip class="pending-chip">Pending</mat-chip>
                        @if (isPrimaryOwner()) {
                          <button
                            mat-button
                            color="warn"
                            type="button"
                            (click)="cancelInvite(inv.id)">
                            Cancel
                          </button>
                        }
                      </div>
                    </div>
                  }
                </div>

                <div class="invite-coowner-box">
                  <mat-form-field appearance="outline" class="invite-input">
                    <mat-label>Invite co-owner by email</mat-label>
                    <input
                      matInput
                      type="email"
                      [(ngModel)]="newPartnerEmail"
                      name="partnerEmailInput"
                      placeholder="partner@example.com" />
                  </mat-form-field>
                  <button
                    mat-flat-button
                    color="primary"
                    type="button"
                    [disabled]="isInviting() || !newPartnerEmail.trim()"
                    (click)="invitePartner()">
                    {{ isInviting() ? 'Inviting...' : 'Send invite' }}
                  </button>
                </div>
              </div>

              @if (errorMessage()) {
                <div class="error-banner" role="alert">{{ errorMessage() }}</div>
              }
              @if (successMessage()) {
                <div class="success-banner" role="status">{{ successMessage() }}</div>
              }

              <div class="danger-zone">
                <h4>Danger zone</h4>
                <p>Archiving the household hides all budgets and freezes ledger transactions.</p>
                <button
                  mat-stroked-button
                  color="warn"
                  type="button"
                  [disabled]="isArchiving()"
                  (click)="archiveHousehold()">
                  {{ isArchiving() ? 'Archiving...' : 'Archive household' }}
                </button>
              </div>

              <div class="card-actions">
                <a mat-button routerLink="/dashboard">Back to dashboard</a>
                <button
                  mat-flat-button
                  color="primary"
                  type="submit"
                  [disabled]="isSaving() || !name.trim() || selectedCurrencies().length === 0">
                  {{ isSaving() ? 'Saving...' : 'Save changes' }}
                </button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .page-container {
      display: flex;
      justify-content: center;
      padding: 32px 16px;
    }
    .settings-card {
      max-width: 640px;
      width: 100%;
      padding: 16px;
    }
    .full-width {
      width: 100%;
      margin-bottom: 12px;
    }
    .currency-group {
      margin-bottom: 24px;
    }
    .group-label {
      display: block;
      font-size: 0.85rem;
      font-weight: 500;
      color: #334155;
      margin-bottom: 8px;
    }
    .checkbox-row {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
    }
    .members-section {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 24px;
    }
    .members-section h3 {
      margin: 0 0 12px;
      font-size: 1rem;
      font-weight: 600;
    }
    .member-list {
      margin-bottom: 16px;
    }
    .member-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 0;
      border-bottom: 1px solid #f1f5f9;
    }
    .member-info {
      display: flex;
      flex-direction: column;
    }
    .member-email {
      font-size: 0.8rem;
      color: #64748b;
    }
    .primary-owner {
      background: #dbeafe !important;
      color: #1e40af !important;
      font-weight: 600;
    }
    .pending-row {
      background: #fffbeb;
      padding: 8px;
      border-radius: 6px;
      margin-top: 4px;
    }
    .invite-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .pending-chip {
      background: #fef3c7 !important;
      color: #92400e !important;
    }
    .invite-coowner-box {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-top: 12px;
    }
    .invite-input {
      flex: 1;
      margin-bottom: 0;
    }
    .danger-zone {
      border: 1px solid #fecaca;
      background: #fff5f5;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 24px;
    }
    .danger-zone h4 {
      margin: 0 0 6px;
      color: #b91c1c;
      font-size: 0.95rem;
    }
    .danger-zone p {
      font-size: 0.85rem;
      color: #7f1d1d;
      margin: 0 0 12px;
    }
    .error-banner {
      background: #fee2e2;
      color: #b91c1c;
      padding: 8px 12px;
      border-radius: 6px;
      margin-bottom: 16px;
      font-size: 0.85rem;
    }
    .success-banner {
      background: #dcfce7;
      color: #15803d;
      padding: 8px 12px;
      border-radius: 6px;
      margin-bottom: 16px;
      font-size: 0.85rem;
    }
    .card-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
    .loading-state {
      padding: 40px;
      text-align: center;
      color: #64748b;
    }
  `]
})
export class HouseholdSettingsComponent implements OnInit {
  private readonly householdService = inject(HouseholdService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly household = signal<Household | null>(null);
  readonly pendingInvites = signal<HouseholdInvitation[]>([]);
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly isInviting = signal(false);
  readonly isArchiving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  name = '';
  currencies = {
    PEN: false,
    USD: false,
    EUR: false,
    COP: false
  };
  newPartnerEmail = '';

  ngOnInit(): void {
    this.loadHousehold();
  }

  loadHousehold(): void {
    this.isLoading.set(true);
    this.householdService.getCurrentHousehold().subscribe({
      next: (hh) => {
        this.household.set(hh);
        this.name = hh.name;
        this.currencies.PEN = hh.baseCurrencies.includes('PEN');
        this.currencies.USD = hh.baseCurrencies.includes('USD');
        this.currencies.EUR = hh.baseCurrencies.includes('EUR');
        this.currencies.COP = hh.baseCurrencies.includes('COP');
        this.isLoading.set(false);
        this.loadPendingInvites(hh.id);
      },
      error: () => {
        this.household.set(null);
        this.isLoading.set(false);
      }
    });
  }

  private loadPendingInvites(householdId: string): void {
    this.householdService.getHouseholdInvitations(householdId).subscribe({
      next: (invites) => {
        this.pendingInvites.set(invites.filter(i => i.status === 'PENDING'));
      },
      error: () => {}
    });
  }

  memberList() {
    const hh = this.household();
    if (!hh || !hh.members) return [];
    return Object.values(hh.members);
  }

  isPrimaryOwner(): boolean {
    const user = this.authService.user();
    const hh = this.household();
    return !!(user && hh && hh.ownerId === user.uid);
  }

  selectedCurrencies(): string[] {
    const keys: (keyof typeof this.currencies)[] = ['PEN', 'USD', 'EUR', 'COP'];
    return keys.filter(k => this.currencies[k]);
  }

  saveSettings(): void {
    const hh = this.household();
    if (!hh || !this.name.trim()) return;

    this.isSaving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const updateReq: UpdateHouseholdRequest = {
      name: this.name.trim(),
      baseCurrencies: this.selectedCurrencies()
    };

    this.householdService.updateHousehold(hh.id, updateReq).subscribe({
      next: (updated) => {
        this.household.set(updated);
        this.isSaving.set(false);
        this.successMessage.set('Settings saved successfully.');
      },
      error: (err: unknown) => {
        this.isSaving.set(false);
        if (err instanceof HttpErrorResponse) {
          this.errorMessage.set(err.error?.message ?? err.message ?? 'Failed to update household.');
        } else {
          this.errorMessage.set('Failed to update household.');
        }
      }
    });
  }

  invitePartner(): void {
    const hh = this.household();
    if (!hh || !this.newPartnerEmail.trim()) return;

    this.isInviting.set(true);
    this.errorMessage.set(null);
    this.householdService.inviteCoOwner(hh.id, { invitedEmail: this.newPartnerEmail.trim() }).subscribe({
      next: () => {
        this.newPartnerEmail = '';
        this.isInviting.set(false);
        this.successMessage.set('Invitation sent successfully.');
        this.loadPendingInvites(hh.id);
      },
      error: (err: unknown) => {
        this.isInviting.set(false);
        if (err instanceof HttpErrorResponse) {
          this.errorMessage.set(err.error?.message ?? err.message ?? 'Failed to send invitation.');
        } else {
          this.errorMessage.set('Failed to send invitation.');
        }
      }
    });
  }

  cancelInvite(invitationId: string): void {
    const hh = this.household();
    if (!hh) return;
    this.householdService.cancelInvitation(hh.id, invitationId).subscribe({
      next: () => {
        this.loadPendingInvites(hh.id);
      }
    });
  }

  archiveHousehold(): void {
    const hh = this.household();
    if (!hh) return;
    const confirmed = window.confirm('Are you sure you want to archive this household?');
    if (!confirmed) return;

    this.isArchiving.set(true);
    this.householdService.archiveHousehold(hh.id).subscribe({
      next: () => {
        this.isArchiving.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.isArchiving.set(false);
        this.errorMessage.set('Failed to archive household.');
      }
    });
  }
}
