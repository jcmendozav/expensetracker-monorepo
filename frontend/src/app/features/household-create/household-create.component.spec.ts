import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { HouseholdCreateComponent } from './household-create.component';
import { HouseholdService } from '../../core/services/household.service';
import type { Household } from '../../core/models/household.model';

describe('HouseholdCreateComponent', () => {
  let component: HouseholdCreateComponent;
  let fixture: ComponentFixture<HouseholdCreateComponent>;
  let mockHouseholdService: jasmine.SpyObj<HouseholdService>;
  let router: Router;

  beforeEach(async () => {
    mockHouseholdService = jasmine.createSpyObj<HouseholdService>('HouseholdService', ['createHousehold']);

    await TestBed.configureTestingModule({
      imports: [HouseholdCreateComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: HouseholdService, useValue: mockHouseholdService }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(HouseholdCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create with default values', () => {
    expect(component).toBeTruthy();
    expect(component.name).toBe('Mendoza Family');
    expect(component.selectedCurrencies()).toContain('PEN');
    expect(component.selectedCurrencies()).toContain('USD');
  });

  it('should create household and navigate to dashboard on submit', () => {
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
    mockHouseholdService.createHousehold.and.returnValue(of(mockHh));

    component.submit();

    expect(mockHouseholdService.createHousehold).toHaveBeenCalledWith({
      name: 'Mendoza Family',
      baseCurrencies: ['PEN', 'USD'],
      partnerEmail: undefined
    });
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
