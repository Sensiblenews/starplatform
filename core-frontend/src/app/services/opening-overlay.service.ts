import { Injectable } from '@angular/core';

/**
 * 로비 오프닝 오버레이("폭죽") 1회 재생 판정 (2-29차).
 *
 * 클라이언트 확정: 앱을 다시 켤 때만 1회. 뒤로 가기로 로비에 돌아올 때·백그라운드 복귀 때는 재생하지 않는다.
 * 루트 싱글턴에 플래그를 두면 로비 페이지가 라우터 캐시에 살아 있든 재생성되든 프로세스 단위로 1회가 보장되고,
 * 프로세스가 죽고 다시 켜지면 플래그도 사라져 자연스럽게 다시 재생된다. 영구 저장(localStorage)은 쓰지 않는다.
 */
@Injectable({ providedIn: 'root' })
export class OpeningOverlayService {
  private played = false;

  /** 순수 판정 — 아직 재생 안 했고 앱이 포그라운드일 때만 (spec 대상) */
  static shouldPlay(played: boolean, foreground: boolean): boolean {
    return !played && foreground;
  }

  /** 재생 가능하면 true를 돌려주고 즉시 소진한다. 두 번째 호출부터는 false */
  consumeFirstPlay(foreground: boolean): boolean {
    const ok = OpeningOverlayService.shouldPlay(this.played, foreground);
    if (ok) this.played = true;
    return ok;
  }

  /** 테스트 전용 — 운영 코드에서는 부르지 않는다 */
  reset(): void {
    this.played = false;
  }
}
