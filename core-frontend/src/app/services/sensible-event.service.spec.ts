import { TestBed } from '@angular/core/testing';

import { SensibleEventService } from './sensible-event.service';

describe('SensibleEventService', () => {
  let service: SensibleEventService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SensibleEventService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
