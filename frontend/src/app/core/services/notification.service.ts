import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NotificationFeedResponse } from '../models/notification.model';
import { environment } from '../../../environments/environments';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/v1/notifications`;

  getFeed(limit = 20, cursor?: string): Observable<NotificationFeedResponse> {
    let params = new HttpParams().set('limit', limit.toString());
    if (cursor) {
      params = params.set('cursor', cursor);
    }
    return this.http.get<NotificationFeedResponse>(this.baseUrl, { params });
  }

  markAsRead(id: string): Observable<{ id: string; isRead: boolean }> {
    return this.http.patch<{ id: string; isRead: boolean }>(`${this.baseUrl}/${id}/read`, null);
  }

  markAllAsRead(): Observable<{ updatedCount: number }> {
    return this.http.post<{ updatedCount: number }>(`${this.baseUrl}/mark-all-read`, null);
  }
}

