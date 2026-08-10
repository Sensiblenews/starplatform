import { TestBed } from '@angular/core/testing';
import { DomSanitizer } from '@angular/platform-browser';
import { SafeUrlPipe } from './safe-url.pipe';

describe('SafeUrlPipe', () => {
  it('create an instance', () => {
    // 파이프 생성자가 DomSanitizer를 요구하므로 TestBed에서 주입받아 전달한다
    const sanitizer = TestBed.inject(DomSanitizer);
    const pipe = new SafeUrlPipe(sanitizer);
    expect(pipe).toBeTruthy();
  });
});
