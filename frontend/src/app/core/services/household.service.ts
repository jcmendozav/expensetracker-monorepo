import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import type { Observable } from 'rxjs';
import type {
  CreateHouseholdRequest,
  Household,
  HouseholdInvitation,
  InviteCoOwnerRequest,
  UpdateHouseholdRequest
} from '../models/household.model';
import { environment } from '../../../environments/environments';

@Injectable({
  providedIn: 'root'
})
export class HouseholdService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1`;

  getCurrentHousehold(): Observable<Household> {
    return this.http.get<Household>(`${this.baseUrl}/households/current`);
  }

  createHousehold(request: CreateHouseholdRequest): Observable<Household> {
    return this.http.post<Household>(`${this.baseUrl}/households`, request);
  }

  getHouseholdById(id: string): Observable<Household> {
    return this.http.get<Household>(`${this.baseUrl}/households/${id}`);
  }

  updateHousehold(id: string, request: UpdateHouseholdRequest): Observable<Household> {
    return this.http.patch<Household>(`${this.baseUrl}/households/${id}`, request);
  }

  archiveHousehold(id: string): Observable<Household> {
    return this.updateHousehold(id, { status: 'ARCHIVED' });
  }

  inviteCoOwner(householdId: string, request: InviteCoOwnerRequest): Observable<HouseholdInvitation> {
    return this.http.post<HouseholdInvitation>(`${this.baseUrl}/households/${householdId}/invitations`, request);
  }

  getHouseholdInvitations(householdId: string): Observable<HouseholdInvitation[]> {
    return this.http.get<HouseholdInvitation[]>(`${this.baseUrl}/households/${householdId}/invitations`);
  }

  cancelInvitation(householdId: string, invitationId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/households/${householdId}/invitations/${invitationId}`);
  }

  getPendingInvitations(): Observable<HouseholdInvitation[]> {
    return this.http.get<HouseholdInvitation[]>(`${this.baseUrl}/invitations/pending`);
  }

  acceptInvitation(invitationId: string): Observable<Household> {
    return this.http.post<Household>(`${this.baseUrl}/invitations/${invitationId}/accept`, null);
  }

  declineInvitation(invitationId: string): Observable<{ id: string; status: string }> {
    return this.http.post<{ id: string; status: string }>(`${this.baseUrl}/invitations/${invitationId}/decline`, null);
  }
}
