import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
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
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/v1`;

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

  inviteCoOwner(householdId: string, request: InviteCoOwnerRequest): Observable<HouseholdInvitation> {
    return this.http.post<HouseholdInvitation>(`${this.baseUrl}/households/${householdId}/invitations`, request);
  }

  getHouseholdInvitations(householdId: string): Observable<HouseholdInvitation[]> {
    return this.http.get<HouseholdInvitation[]>(`${this.baseUrl}/households/${householdId}/invitations`);
  }

  cancelInvitation(householdId: string, invitationId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/households/${householdId}/invitations/${invitationId}`);
  }

  getPendingInvitations(email?: string): Observable<HouseholdInvitation[]> {
    let params = new HttpParams();
    if (email) {
      params = params.set('email', email);
    }
    return this.http.get<HouseholdInvitation[]>(`${this.baseUrl}/invitations/pending`, { params });
  }

  acceptInvitation(invitationId: string, email?: string, displayName?: string): Observable<Household> {
    let params = new HttpParams();
    if (email) params = params.set('email', email);
    if (displayName) params = params.set('displayName', displayName);
    return this.http.post<Household>(`${this.baseUrl}/invitations/${invitationId}/accept`, null, { params });
  }

  declineInvitation(invitationId: string): Observable<{ id: string; status: string }> {
    return this.http.post<{ id: string; status: string }>(`${this.baseUrl}/invitations/${invitationId}/decline`, null);
  }
}

