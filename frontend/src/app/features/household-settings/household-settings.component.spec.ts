import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { HouseholdSettingsComponent } from './household-settings.component';
import { HouseholdService } from '../../core/services/household.service';
import { AuthService } from '../../core/services/auth.service';
import type { Household } from '../../core/models/household.model';

describe('HouseholdSettingsComponent', () => {
  let component: HouseholdSettingsComponent;
  let fixture: ComponentFixture<HouseholdSettingsComponent>;
  let mockHouseholdService: jasmine.SpyObj<HouseholdService>;
  let mockAuthService: { user: () => any };

  const mockHousehold: Household = {
    id: 'hh-1',
    name: 'Mendoza Family',
    status: 'ACTIVE',
    baseCurrencies: ['PEN', 'USD'],
    balancesByCurrency: {},
    ownerId: 'user-1',
    members: {
      'user-1': {
        userId: 'user-1',
        role: 'PRIMARY_OWNER',
        displayName: 'Carlos',
        email: 'carlos@example.com',
        status: 'ACTIVE'
      }
    },
    memberIds: ['user-1'],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(async () => {
    mockHouseholdService = jasmine.createSpyObj<HouseholdService>('HouseholdService', [
      'getCurrentHousehold',
      'getHouseholdInvitations',
      'updateHousehold',
      'inviteCoOwner',
      'cancelInvitation',
      'archiveHousehold'
    ]);
    mockAuthService = {
      user: signal({ uid: 'user-1', email: 'carlos@example.com' })
    };

    mockHouseholdService.getCurrentHousehold.and.returnValue(of(mockHousehold));
    mockHouseholdService.getHouseholdInvitations.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [HouseholdSettingsComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: HouseholdService, useValue: mockHouseholdService },
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HouseholdSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load household and set form controls', () => {
    expect(component).toBeTruthy();
    expect(component.name).toBe('Mendoza Family');
    expect(component.isPrimaryOwner()).toBeTrue();
    expect(component.memberList().length).toBe(1);
  });

  it('should update household on saveSettings', () => {
    const updatedHh = { ...mockHousehold, name: 'New Family Name' };
    mockHouseholdService.updateHousehold.and.returnValue(of(updatedHh));

    component.name = 'New Family Name';
    component.saveSettings();

    expect(mockHouseholdService.updateHousehold).toHaveBeenCalledWith('hh-1', {
      name: 'New Family Name',
      baseCurrencies: ['PEN', 'USD']
    });
    expect(component.successMessage()).toBe('Settings saved successfully.');
  });
});
