import { TestBed } from '@angular/core/testing';

import { AppleLoginService } from './apple-login.service';

describe('AppleLoginService', () => {
  let service: AppleLoginService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AppleLoginService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
