import { TestBed } from '@angular/core/testing';

import { IsAuthedGuard } from './is-authed.guard';

describe('IsAuthedGuard', () => {
  let guard: IsAuthedGuard;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    guard = TestBed.inject(IsAuthedGuard);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });
});
