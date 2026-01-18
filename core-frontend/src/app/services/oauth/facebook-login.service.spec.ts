import { TestBed } from '@angular/core/testing';

import { EmailLoginService } from './email-login.service';

describe('FacebookLoginService', () => {
  let service: EmailLoginService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EmailLoginService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
