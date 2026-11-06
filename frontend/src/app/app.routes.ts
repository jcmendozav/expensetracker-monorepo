import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { LoginComponent } from './features/login/login.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { HouseholdCreateComponent } from './features/household-create/household-create.component';
import { HouseholdSettingsComponent } from './features/household-settings/household-settings.component';
import { BudgetCreateComponent } from './features/budget-create/budget-create.component';
import { BudgetDetailComponent } from './features/budget-detail/budget-detail.component';
import { BudgetSettingsComponent } from './features/budget-settings/budget-settings.component';

export const authGuard = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.user();
  if (user) return true;

  router.navigate(['/login']);
  return false;
};

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard]
  },
  {
    path: 'households/new',
    component: HouseholdCreateComponent,
    canActivate: [authGuard]
  },
  {
    path: 'settings/household',
    component: HouseholdSettingsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'budgets/new',
    component: BudgetCreateComponent,
    canActivate: [authGuard]
  },
  {
    path: 'budgets/:id',
    component: BudgetDetailComponent,
    canActivate: [authGuard]
  },
  {
    path: 'budgets/:id/settings',
    component: BudgetSettingsComponent,
    canActivate: [authGuard]
  },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];
