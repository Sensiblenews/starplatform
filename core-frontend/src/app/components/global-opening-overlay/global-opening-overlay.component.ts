import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnDestroy, Output } from '@angular/core';
import { HapticService } from '../../services/haptic.service';

/**
 * 로비 오프닝 오버레이 — 글로벌 랭킹 접속 연출 (2-29차, 클라이언트 10단계안).
 *
 * 기존 로비 UI 위에 투명하게 얹혀 1.5초 뒤 스스로 사라진다. 조작을 막지 않는다(pointer-events: none).
 *   0.0초 지구본 등장 → 0.2초 별빛 → 0.4초 원형 빛 파동 + 약한 햅틱 1회 → 0.6초 문구 → 1.2초 소거 → 1.5초 제거
 * 소리는 쓰지 않는다. 모션 감소 설정이면 정적으로 보이고 햅틱도 생략한다.
 * 언제 재생할지는 OpeningOverlayService(프로세스당 1회)와 로비 페이지가 정한다.
 */
@Component({
  standalone: true,
  imports: [CommonModule],
  selector: 'app-global-opening-overlay',
  templateUrl: './global-opening-overlay.component.html',
  styleUrls: ['./global-opening-overlay.component.scss'],
})
export class GlobalOpeningOverlayComponent implements OnDestroy {
  // 타임라인(ms). 순서: HAPTIC_AT < FADE_AT < TOTAL
  static readonly HAPTIC_AT_MS = 400;
  static readonly FADE_AT_MS = 1200;
  static readonly TOTAL_MS = 1500;

  // 별빛 5개 — 위치·색·시작 시차. 파티클 시스템 대신 고정 배치(클라이언트 안)
  readonly stars = [
    { x: -58, y: -46, size: 12, color: 'gold', delay: 0.20 },
    { x: 54, y: -52, size: 9, color: 'white', delay: 0.25 },
    { x: -66, y: 34, size: 8, color: 'blue', delay: 0.32 },
    { x: 60, y: 40, size: 11, color: 'gold', delay: 0.40 },
    { x: 6, y: -78, size: 7, color: 'white', delay: 0.50 },
  ];

  /** 정상 종료(1.5초) 시에만 발생한다. stop(false)로 끊었을 때는 발생하지 않는다 */
  @Output() finished = new EventEmitter<void>();

  visible = false;
  fading = false;
  // 모션 감소 설정: 확대·파동 없이 정적 표시, 햅틱 생략
  staticMode = false;

  private timers: any[] = [];

  constructor(private haptic: HapticService) { }

  /** 시스템 모션 감소 설정 — matchMedia가 없는 환경(구형 WebView·테스트)에서는 false */
  static prefersReducedMotion(
    win: { matchMedia?: (query: string) => { matches: boolean } } | null | undefined = typeof window !== 'undefined' ? window : null
  ): boolean {
    if (!win || typeof win.matchMedia !== 'function') return false;
    try {
      return !!win.matchMedia('(prefers-reduced-motion: reduce)').matches;
    } catch (e) {
      return false;
    }
  }

  /** 재생 시작. 이미 재생 중이면 무시한다. API 응답과 무관하게 자기 타이머로만 끝난다 */
  play(): void {
    if (this.visible) return;

    this.staticMode = GlobalOpeningOverlayComponent.prefersReducedMotion();
    this.visible = true;
    this.fading = false;

    if (!this.staticMode) {
      // 메인 빛 파동이 시작되는 순간에만 약한 햅틱 1회 (별빛마다·전체 시간 동안 울리지 않는다)
      this.timers.push(setTimeout(() => { this.haptic.tap(); }, GlobalOpeningOverlayComponent.HAPTIC_AT_MS));
    }
    this.timers.push(setTimeout(() => { this.fading = true; }, GlobalOpeningOverlayComponent.FADE_AT_MS));
    this.timers.push(setTimeout(() => this.stop(true), GlobalOpeningOverlayComponent.TOTAL_MS));
  }

  /**
   * 즉시 종료. 백그라운드 전환·화면 이탈 시에는 emit=false로 끊는다.
   * 남은 타이머(햅틱 포함)를 모두 정리하므로 끊은 뒤에 진동이 울리지 않는다.
   */
  stop(emit: boolean = true): void {
    this.clearTimers();
    if (!this.visible) return;
    this.visible = false;
    this.fading = false;
    if (emit) this.finished.emit();
  }

  ngOnDestroy(): void {
    this.stop(false);
  }

  private clearTimers(): void {
    this.timers.forEach(t => clearTimeout(t));
    this.timers = [];
  }
}
