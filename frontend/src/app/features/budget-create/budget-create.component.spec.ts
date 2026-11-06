import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { BudgetCreateComponent } from './budget-create.component';
import { BudgetService } from '../../core/services/budget.service';
import { HouseholdService } from '../../core/services/household.service';
import type { Household } from '../../core/models/household.model';
import type { Budget } from '../../core/models/budget.model';

describe('BudgetCreateComponent', () => {
  let component: BudgetCreateComponent;
  let fixture: ComponentFixture<BudgetCreateComponent>;
  let mockBudgetService: jasmine.SpyObj<BudgetService>;
  let mockHouseholdService: jasmine.SpyObj<HouseholdService>;
  let router: Router;

  const mockHh: Household = {
    id: 'hh-1',
    name: 'Mendoza Family',
    status: 'ACTIVE',
    baseCurrencies: ['PEN', 'USD'],
    balancesByCurrency: {},
    ownerId: 'user-1',
    members: {},
    memberIds: ['user-1'],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(async () => {
    mockBudgetService = jasmine.createSpyObj<BudgetService>('BudgetService', ['createBudget']);
    mockHouseholdService = jasmine.createSpyObj<HouseholdService>('HouseholdService', ['getCurrentHousehold']);

    mockHouseholdService.getCurrentHousehold.and.returnValue(of(mockHh));

    await TestBed.configureTestingModule({
      imports: [BudgetCreateComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: BudgetService, useValue: mockBudgetService },
        { provide: HouseholdService, useValue: mockHouseholdService }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(BudgetCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load household and set available currencies', () => {
    expect(component).toBeTruthy();
    expect(component.householdId()).toBe('hh-1');
    expect(component.availableCurrencies()).toEqual(['PEN', 'USD']);
    expect(component.currency).toBe('PEN');
  });

  it('should create budget with integer minor units and navigate to dashboard', () => {
    const mockBudget: Budget = {
      id: 'bdg-1',
      householdId: 'hh-1',
      ownerId: 'user-1',
      title: 'Groceries',
      currency: 'PEN',
      initialAmountCents: 50000,
      currentBalanceCents: 50000,
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z'
    };
    mockBudgetService.createBudget.and.returnValue(of(mockBudget));

    component.title = 'Groceries';
    component.amount = 500;
    component.currency = 'PEN';

    component.submit();

    expect(mockBudgetService.createBudget).toHaveBeenCalledWith({
      householdId: 'hh-1',
      title: 'Groceries',
      initialAmountCents: 50000,
      currency: 'PEN'
    });
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
