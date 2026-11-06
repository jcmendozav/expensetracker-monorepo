import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { HouseholdService } from '../../core/services/household.service';
import { BudgetService } from '../../core/services/budget.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuthService } from '../../core/services/auth.service';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  const mockHousehold = {
    id: 'hh_123',
    name: 'Mendoza Family',
    status: 'ACTIVE',
    baseCurrencies: ['PEN', 'USD'],
    balancesByCurrency: { PEN: 1420000, USD: 85000 },
    ownerId: 'usr_carlos_123',
    members: {},
    memberIds: ['usr_carlos_123'],
    createdAt: '',
    updatedAt: ''
  };

  const mockHouseholdService = {
    getCurrentHousehold: () => of(mockHousehold),
    getPendingInvitations: () => of([]),
    createHousehold: () => of(mockHousehold),
    acceptInvitation: () => of(mockHousehold),
    declineInvitation: () => of({ id: 'inv_1', status: 'DECLINED' })
  };

  const mockBudgetService = {
    listBudgets: () => of({ items: [], nextCursor: undefined }),
    getSummary: () => of({ balancesByCurrency: { PEN: 1420000, USD: 85000 }, activeCount: 0 }),
    listTransactions: () => of([]),
    logTransaction: () => of({}),
    transferFunds: () => of({}),
    voidTransaction: () => of({})
  };

  const mockNotificationService = {
    getFeed: () => of({ items: [], nextCursor: undefined }),
    markAsRead: () => of({ id: 'n1', isRead: true }),
    markAllAsRead: () => of({ updatedCount: 0 })
  };

  const mockAuthService = {
    user: () => ({ uid: 'usr_carlos_123', email: 'carlos@example.com', displayName: 'Carlos' }),
    getToken: () => Promise.resolve('mock-token'),
    logout: () => of(undefined)
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: HouseholdService, useValue: mockHouseholdService },
        { provide: BudgetService, useValue: mockBudgetService },
        { provide: NotificationService, useValue: mockNotificationService },
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load household liquidity', async () => {
    await component.loadAll();
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(component.household()?.name).toBe('Mendoza Family');
  });
});
