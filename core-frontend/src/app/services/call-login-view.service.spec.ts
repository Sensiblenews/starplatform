import { TestBed } from '@angular/core/testing';

import { CallLoginViewService } from './call-login-view.service';

describe('CallLoginViewService', () => {
  let service: CallLoginViewService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CallLoginViewService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
