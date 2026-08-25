import { Injectable } from '@angular/core';

interface PerfMark {
  label: string;
  at: number;
}

/**
 * 화면 진입 구간 계측기 (2-26차).
 *
 * 내장 브라우저에서는 Ionic 라이프사이클이 돌지 않아 로딩 구간을 재현할 수 없다.
 * 그래서 실기기에서 직접 수치를 볼 수 있도록 런타임 스위치로 켠다.
 *
 *   localStorage.setItem('perfTrace', '1')  → 켜기 (앱 재시작 필요)
 *   localStorage.removeItem('perfTrace')    → 끄기
 *
 * 기본값은 꺼짐이며, 꺼져 있으면 모든 메서드가 즉시 반환하므로 배포 빌드에 부담을 주지 않는다.
 * 환경 파일(environment.production)로 가르지 않는 이유: 두 환경 파일 모두 production=true라
 * 개발자가 주석을 토글해야만 갈리기 때문에 배포 빌드 실측에 쓸 수 없다.
 */
@Injectable({ providedIn: 'root' })
export class PerfTraceService {

  private readonly enabled: boolean;

  private traceName: string | null = null;
  private marks: PerfMark[] = [];

  constructor() {
    let on = false;
    try {
      on = localStorage.getItem('perfTrace') === '1';
    } catch (e) {
      // 프라이빗 모드 등 localStorage 접근이 막힌 환경에서는 그냥 끈다
      on = false;
    }
    this.enabled = on;
  }

  get isEnabled(): boolean {
    return this.enabled;
  }

  /** 구간 측정 시작. 이전 측정이 끝나지 않았으면 버리고 새로 시작한다 */
  start(traceName: string): void {
    if (!this.enabled) return;
    this.traceName = traceName;
    this.marks = [{ label: 'start', at: this.now() }];
  }

  /** 중간 지점 기록. start() 없이 부르면 무시한다 */
  mark(label: string): void {
    if (!this.enabled || this.traceName === null) return;
    this.marks.push({ label, at: this.now() });
  }

  /** 마지막 지점을 찍고 표로 출력한 뒤 측정을 닫는다 */
  end(label: string = 'end'): void {
    if (!this.enabled || this.traceName === null) return;
    this.marks.push({ label, at: this.now() });

    const first = this.marks[0].at;
    let prev = first;
    const rows = this.marks.map(m => {
      const row = {
        구간: m.label,
        '직전대비(ms)': Math.round(m.at - prev),
        '누적(ms)': Math.round(m.at - first)
      };
      prev = m.at;
      return row;
    });

    console.log(`[perf] ${this.traceName} — 총 ${Math.round(prev - first)}ms`);
    console.table(rows);

    this.traceName = null;
    this.marks = [];
  }

  private now(): number {
    if (typeof performance !== 'undefined' && performance && typeof performance.now === 'function') {
      return performance.now();
    }
    return Date.now();
  }
}
