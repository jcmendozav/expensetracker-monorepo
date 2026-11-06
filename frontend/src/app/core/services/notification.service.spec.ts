import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NotificationService } from './notification.service';
import type { NotificationFeedResponse } from '../models/notification.model';
import { environment } from '../../../environments/environments';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpTesting: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/v1/notifications`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        NotificationService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(NotificationService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('getFeed should send GET to /notifications with limit and cursor params', () => {
    const mockFeed: NotificationFeedResponse = {
      items: [
        {
          id: 'n1',
          householdId: 'hh1',
          recipientId: 'u1',
          actorId: 'u2',
          type: 'EXPENSE_LOGGED',
          title: 'Expense',
          message: 'Logged expense',
          isRead: false,
          createdAt: '2026-11-06T00:00:00Z'
        }
      ],
      nextCursor: 'n1'
    };

    service.getFeed(10, 'cur_prev').subscribe(feed => {
      expect(feed.items.length).toBe(1);
      expect(feed.nextCursor).toBe('n1');
    });

    const req = httpTesting.expectOne(`${baseUrl}?limit=10&cursor=cur_prev`);
    expect(req.request.method).toBe('GET');
    req.flush(mockFeed);
  });

  it('markAsRead should send PATCH to /notifications/{id}/read', () => {
    service.markAsRead('n1').subscribe(res => {
      expect(res.isRead).toBe(true);
    });

    const req = httpTesting.expectOne(`${baseUrl}/n1/read`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: 'n1', isRead: true });
  });

  it('markAllAsRead should send POST to /notifications/mark-all-read', () => {
    service.markAllAsRead().subscribe(res => {
      expect(res.updatedCount).toBe(4);
    });

    const req = httpTesting.expectOne(`${baseUrl}/mark-all-read`);
    expect(req.request.method).toBe('POST');
    req.flush({ updatedCount: 4 });
  });
});
