import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HouseholdService } from './household.service';
import type { CreateHouseholdRequest, Household, HouseholdInvitation } from '../models/household.model';
import { environment } from '../../../environments/environments';

describe('HouseholdService', () => {
  let service: HouseholdService;
  let httpTesting: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/v1`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        HouseholdService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(HouseholdService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('getCurrentHousehold should send GET to /households/current', () => {
    const mockHh: Partial<Household> = { id: 'hh_1', name: 'Mendoza Family' };

    service.getCurrentHousehold().subscribe(hh => {
      expect(hh.id).toBe('hh_1');
      expect(hh.name).toBe('Mendoza Family');
    });

    const req = httpTesting.expectOne(`${baseUrl}/households/current`);
    expect(req.request.method).toBe('GET');
    req.flush(mockHh);
  });

  it('createHousehold should send POST to /households with request', () => {
    const request: CreateHouseholdRequest = {
      name: 'Mendoza Family',
      baseCurrencies: ['PEN', 'USD']
    };

    service.createHousehold(request).subscribe(hh => {
      expect(hh.id).toBe('hh_new');
    });

    const req = httpTesting.expectOne(`${baseUrl}/households`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 'hh_new', ...request });
  });

  it('getPendingInvitations should send GET to /invitations/pending', () => {
    service.getPendingInvitations().subscribe(invs => {
      expect(invs.length).toBe(1);
      expect(invs[0].id).toBe('inv_1');
    });

    const req = httpTesting.expectOne(`${baseUrl}/invitations/pending`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'inv_1', status: 'PENDING' }]);
  });

  it('acceptInvitation should send POST to /invitations/{id}/accept', () => {
    service.acceptInvitation('inv_1').subscribe(hh => {
      expect(hh.id).toBe('hh_1');
    });

    const req = httpTesting.expectOne(`${baseUrl}/invitations/inv_1/accept`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 'hh_1' });
  });

  it('declineInvitation should send POST to /invitations/{id}/decline', () => {
    service.declineInvitation('inv_1').subscribe(res => {
      expect(res.status).toBe('DECLINED');
    });

    const req = httpTesting.expectOne(`${baseUrl}/invitations/inv_1/decline`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 'inv_1', status: 'DECLINED' });
  });
});
