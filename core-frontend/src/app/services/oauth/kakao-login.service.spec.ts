import { TestBed } from '@angular/core/testing';

import { KakaoLoginService } from '../oauth/kakao-login.service';

describe('KakaoLoginService', () => {
  let service: KakaoLoginService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(KakaoLoginService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
