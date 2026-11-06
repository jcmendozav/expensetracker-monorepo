import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { BudgetSettingsComponent } from './budget-settings.component';
import { BudgetService } from '../../core/services/budget.service';
import type { Budget } from '../../core/models/budget.model';

describe('BudgetSettingsComponent', () => {
  let component: BudgetSettingsComponent;
  let fixture: ComponentFixture<BudgetSettingsComponent>;
  let mockBudgetService: jasmine.SpyObj<BudgetService>;
  let router: Router;

  const mockBudget: Budget = {
    id: 'bdg-1',
    householdId: 'hh-1',
    ownerId: 'user-1',
    title: 'Groceries',
    currency: 'PEN',
    initialAmountCents: 100000,
    currentBalanceCents: 80000,
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(async () => {
    mockBudgetService = jasmine.createSpyObj<BudgetService>('BudgetService', [
      'getBudget',
      'updateBudget',
      'archiveBudget'
    ]);
    mockBudgetService.getBudget.and.returnValue(of(mockBudget));

    await TestBed.configureTestingModule({
      imports: [BudgetSettingsComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: BudgetService, useValue: mockBudgetService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? 'bdg-1' : null)
              }
            }
          }
        }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(BudgetSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load budget and initialize fields', () => {
    expect(component).toBeTruthy();
    expect(component.title).toBe('Groceries');
    expect(component.amount).toBe(1000);
  });

  it('should update budget with integer cents and navigate', () => {
    mockBudgetService.updateBudget.and.returnValue(of({
      ...mockBudget,
      title: 'Updated Groceries',
      initialAmountCents: 120000
    }));

    component.title = 'Updated Groceries';
    component.amount = 1200;

    component.saveSettings();

    expect(mockBudgetService.updateBudget).toHaveBeenCalledWith('bdg-1', {
      title: 'Updated Groceries',
      initialAmountCents: 120000
    });
    expect(router.navigate).toHaveBeenCalledWith(['/budgets', 'bdg-1']);
  });

  it('should archive budget and navigate to dashboard', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    mockBudgetService.archiveBudget.and.returnValue(of({
      ...mockBudget,
      status: 'ARCHIVED'
    }));

    component.archiveBudget();

    expect(mockBudgetService.archiveBudget).toHaveBeenCalledWith('bdg-1');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
