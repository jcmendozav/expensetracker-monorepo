import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { ActivityDrawerComponent } from './activity-drawer.component';
import { NotificationService } from '../../core/services/notification.service';
import { HouseholdService } from '../../core/services/household.service';
import type { AppNotification } from '../../core/models/notification.model';
import type { Household, HouseholdInvitation } from '../../core/models/household.model';

describe('ActivityDrawerComponent', () => {
  let component: ActivityDrawerComponent;
  let fixture: ComponentFixture<ActivityDrawerComponent>;
  let mockNotificationService: jasmine.SpyObj<NotificationService>;
  let mockHouseholdService: jasmine.SpyObj<HouseholdService>;
  let mockRouter: jasmine.SpyObj<Router>;

  const mockNotifications: AppNotification[] = [
    {
      id: 'notif-1',
      householdId: 'hh-1',
      recipientId: 'user-1',
      actorId: 'user-2',
      type: 'BUDGET_LOW',
      title: 'Low balance alert',
      message: 'Groceries is low',
      budgetId: 'bdg-1',
      isRead: false,
      createdAt: '2026-01-01T12:00:00Z',
      expiresAt: '2026-04-01T12:00:00Z'
    }
  ];

  const mockInvitations: HouseholdInvitation[] = [
    {
      id: 'inv-1',
      householdId: 'hh-1',
      householdName: 'Mendoza Family',
      inviterId: 'user-2',
      inviterName: 'Maria',
      invitedEmail: 'test@example.com',
      role: 'CO_OWNER',
      status: 'PENDING',
      createdAt: '2026-01-01T12:00:00Z',
      updatedAt: '2026-01-01T12:00:00Z'
    }
  ];

  beforeEach(async () => {
    mockNotificationService = jasmine.createSpyObj<NotificationService>('NotificationService', ['markAsRead', 'markAllAsRead']);
    mockHouseholdService = jasmine.createSpyObj<HouseholdService>('HouseholdService', ['acceptInvitation', 'declineInvitation']);
    mockRouter = jasmine.createSpyObj<Router>('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [ActivityDrawerComponent],
      providers: [
        provideNoopAnimations(),
        { provide: NotificationService, useValue: mockNotificationService },
        { provide: HouseholdService, useValue: mockHouseholdService },
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ActivityDrawerComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('notifications', mockNotifications);
    fixture.componentRef.setInput('invitations', mockInvitations);
    fixture.detectChanges();
  });

  it('should create and render notifications and invitations', () => {
    expect(component).toBeTruthy();
    expect(component.notifications().length).toBe(1);
    expect(component.invitations().length).toBe(1);
  });

  it('should accept invitation and emit refreshed', () => {
    const mockHh: Household = {
      id: 'hh-1',
      name: 'Mendoza Family',
      status: 'ACTIVE',
      baseCurrencies: ['PEN'],
      balancesByCurrency: {},
      ownerId: 'user-1',
      members: {},
      memberIds: ['user-1'],
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z'
    };
    mockHouseholdService.acceptInvitation.and.returnValue(of(mockHh));
    spyOn(component.refreshed, 'emit');

    component.acceptInvite('inv-1');

    expect(mockHouseholdService.acceptInvitation).toHaveBeenCalledWith('inv-1');
    expect(component.refreshed.emit).toHaveBeenCalled();
  });

  it('should decline invitation and emit refreshed', () => {
    mockHouseholdService.declineInvitation.and.returnValue(of({ id: 'inv-1', status: 'DECLINED' }));
    spyOn(component.refreshed, 'emit');

    component.declineInvite('inv-1');

    expect(mockHouseholdService.declineInvitation).toHaveBeenCalledWith('inv-1');
    expect(component.refreshed.emit).toHaveBeenCalled();
  });

  it('should mark notification read and emit refreshed', () => {
    mockNotificationService.markAsRead.and.returnValue(of({ id: 'notif-1', isRead: true }));
    spyOn(component.refreshed, 'emit');

    component.markRead(mockNotifications[0]);

    expect(mockNotificationService.markAsRead).toHaveBeenCalledWith('notif-1');
    expect(mockNotifications[0].isRead).toBeTrue();
    expect(component.refreshed.emit).toHaveBeenCalled();
  });

  it('should mark all notifications as read', () => {
    mockNotificationService.markAllAsRead.and.returnValue(of({ updatedCount: 1 }));
    spyOn(component.refreshed, 'emit');

    component.markAllRead();

    expect(mockNotificationService.markAllAsRead).toHaveBeenCalled();
    expect(component.refreshed.emit).toHaveBeenCalled();
  });

  it('should navigate to budget when viewBudget is clicked', () => {
    spyOn(component.close, 'emit');

    component.viewBudget('bdg-1');

    expect(component.close.emit).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/budgets', 'bdg-1']);
  });
});
