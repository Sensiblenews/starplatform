import { AfterViewInit, Component, ElementRef, EventEmitter, Input, NgZone, OnDestroy, Output, ViewChild } from '@angular/core';
import { VsCard } from '../vs-carousel/vs-carousel.component';

// 티커에 흘려보낼 속보 한 건. priority 1(역전)은 진행 중인 문장을 끊고 즉시 나간다
export interface TickerEvent {
  priority: number; // 1 = 역전(NEW KING/LEAD CHANGE), 2 = 추격(GAP 감소), 3 = 점수 변화
  text: string;
}

// VS 카드 한 장의 직전 상태. 폴링 응답끼리 비교해 "무슨 일이 생겼는지"를 찾는다
interface CardSnapshot {
  leftId: string;
  leftScore: number;
  rightId: string;
  rightScore: number;
}

/**
 * 로비 라이브 뉴스 티커 (2-27차) — VS 카드 바로 아래 36px 한 줄.
 *
 * 방송 자막처럼 문장이 오른쪽에서 왼쪽으로 흘러간다(클라이언트 확정 — marquee 방식).
 * 새 API·DB 없이 로비가 이미 3초 폴링 중인 VS 카드 데이터의 변화만 감지한다.
 * 문장 하나가 끝까지 흐른 뒤에만 다음 문장으로 넘어가므로 최소 갱신 간격(3초+)이
 * 자연히 지켜지고, 역전(priority 1)만 예외로 진행 중인 문장을 끊고 즉시 나간다.
 */
@Component({
  selector: 'app-live-news-ticker',
  templateUrl: './live-news-ticker.component.html',
  styleUrls: ['./live-news-ticker.component.scss'],
})
export class LiveNewsTickerComponent implements AfterViewInit, OnDestroy {

  // 흐름 속도(px/초). 너무 빠르면 못 읽고, 너무 느리면 속보가 밀린다
  private static readonly SPEED_PX_PER_SEC = 70;
  // 짧은 문장도 최소 이 시간은 화면에 머물게 한다
  private static readonly MIN_PASS_MS = 4000;
  // 추격 속보로 인정할 최대 점수 차
  private static readonly CHASE_GAP_MAX = 5;
  // 밀린 과거 속보를 쌓아두지 않는다 — 최신 중요 이벤트 우선(요청서 확정)
  private static readonly MAX_PENDING = 2;

  @ViewChild('marqueeWindow') marqueeWindowEl: ElementRef<HTMLElement>;
  @ViewChild('marqueeText') marqueeTextEl: ElementRef<HTMLElement>;

  // 티커 터치 → 로비가 현재 VS 카드로 포커스를 옮긴다
  @Output() focusRequest = new EventEmitter<void>();

  currentText = '';
  currentUrgent = false;

  private currentCards: VsCard[] = [];
  private prevSnapshots = new Map<string, CardSnapshot>();
  private pendingEvents: TickerEvent[] = [];
  private defaultIndex = 0;
  private running = false;
  private anim: Animation | null = null;
  private fallbackTimerId: any = null;

  @Input()
  set cards(value: VsCard[] | null) {
    this.currentCards = value || [];

    const events = LiveNewsTickerComponent.detectEvents(this.prevSnapshots, this.currentCards);
    this.prevSnapshots = LiveNewsTickerComponent.buildSnapshots(this.currentCards);
    if (events.length === 0) return;

    events.sort((a, b) => a.priority - b.priority);
    const top = events[0];
    if (top.priority === 1 && this.running) {
      // 역전은 즉시 — 진행 중인 문장을 끊는다
      this.pendingEvents = events.slice(1, 1 + LiveNewsTickerComponent.MAX_PENDING);
      this.playMessage(top.text, true);
    } else {
      // 일반 이벤트는 최신 상태만 유지한다. 몇 초 지난 사건을 뒤늦게 내보내지 않는다
      this.pendingEvents = events.slice(0, LiveNewsTickerComponent.MAX_PENDING);
    }
  }

  constructor(private ngZone: NgZone) { }

  ngAfterViewInit() {
    this.start();
  }

  ngOnDestroy() {
    this.stop();
  }

  // 로비 페이지가 ionViewDidEnter/ionViewWillLeave에서 호출해 백그라운드 낭비를 막는다
  start() {
    if (this.running) return;
    this.running = true;
    this.playNext();
  }

  stop() {
    this.running = false;
    if (this.anim) {
      this.anim.cancel();
      this.anim = null;
    }
    if (this.fallbackTimerId) {
      clearTimeout(this.fallbackTimerId);
      this.fallbackTimerId = null;
    }
  }

  onBarClick() {
    this.focusRequest.emit();
  }

  // ===== 이벤트 감지 (순수 로직 — 스펙 테스트 대상) =====

  /** 카드 목록을 직전 비교용 스냅샷으로 변환한다 */
  static buildSnapshots(cards: VsCard[]): Map<string, CardSnapshot> {
    const map = new Map<string, CardSnapshot>();
    for (const card of cards || []) {
      if (!card || !card.left || !card.right) continue;
      map.set(LiveNewsTickerComponent.cardKey(card), {
        leftId: card.left.id,
        leftScore: card.left.score || 0,
        rightId: card.right.id,
        rightScore: card.right.score || 0,
      });
    }
    return map;
  }

  /**
   * 직전 스냅샷과 현재 카드를 비교해 속보를 만든다.
   * 같은 대진(좌우 동일)에서 실제 변화가 있을 때만 이벤트가 발생한다.
   */
  static detectEvents(prev: Map<string, CardSnapshot>, cards: VsCard[]): TickerEvent[] {
    const events: TickerEvent[] = [];
    for (const card of cards || []) {
      if (!card || !card.left || !card.right) continue;
      const snap = prev.get(LiveNewsTickerComponent.cardKey(card));
      if (!snap) continue;
      // 대진이 바뀌었으면(스타 교체) 비교 자체가 무의미하다
      if (snap.leftId !== card.left.id || snap.rightId !== card.right.id) continue;

      const leftScore = card.left.score || 0;
      const rightScore = card.right.score || 0;
      if (leftScore === snap.leftScore && rightScore === snap.rightScore) continue;

      const leader = leftScore >= rightScore ? card.left : card.right;
      const trailer = leftScore >= rightScore ? card.right : card.left;
      const gap = Math.abs(leftScore - rightScore);
      const prevGap = Math.abs(snap.leftScore - snap.rightScore);
      const prevLeaderId = snap.leftScore >= snap.rightScore ? snap.leftId : snap.rightId;

      if (gap > 0 && leader.id !== prevLeaderId) {
        // 역전 — Daily King 카드는 왕좌 교체로 표현한다
        events.push({
          priority: 1,
          text: card.type === 'DAILY'
            ? `🚨 NEW KING · ${leader.name} takes #1!`
            : `⚡ LEAD CHANGE · ${leader.name} overtakes ${trailer.name}!`,
        });
      } else if (gap > 0 && gap < prevGap && gap <= LiveNewsTickerComponent.CHASE_GAP_MAX) {
        events.push({
          priority: 2,
          text: `🔥 ${trailer.name} closing in on ${leader.name} · GAP ${gap}`,
        });
      } else {
        events.push({
          priority: 3,
          text: `⚔️ ${card.left.name} ${leftScore} : ${rightScore} ${card.right.name}`,
        });
      }
    }
    return events;
  }

  private static cardKey(card: VsCard): string {
    return `${card.vsId}-${card.type}-${card.category}`;
  }

  // ===== 재생 =====

  private playNext() {
    if (!this.running) return;

    const event = this.pendingEvents.shift();
    if (event) {
      this.playMessage(event.text, event.priority === 1);
      return;
    }

    // 속보가 없으면 현재 상황 문구를 순환한다 — 로비가 멈춰 보이지 않게(요청서 확정)
    const defaults = this.buildDefaults();
    if (defaults.length === 0) {
      // 아직 보여줄 카드가 없다. 폴링이 채워줄 때까지 잠시 뒤 재시도
      this.fallbackTimerId = setTimeout(() => this.playNext(), 1500);
      return;
    }
    this.playMessage(defaults[this.defaultIndex % defaults.length], false);
    this.defaultIndex++;
  }

  /** 이벤트가 없을 때 순환할 현재 상태 문구를 만든다 */
  private buildDefaults(): string[] {
    const messages: string[] = [];
    for (const card of this.currentCards) {
      if (!card || !card.left) continue;
      if (!card.right) {
        messages.push(`👑 ${card.left.name} awaits a challenger`);
        continue;
      }
      const leftScore = card.left.score || 0;
      const rightScore = card.right.score || 0;
      const gap = Math.abs(leftScore - rightScore);
      messages.push(`⚔️ LIVE VS · ${card.left.name} vs ${card.right.name} · GAP ${gap}`);
      if (card.type === 'DAILY') {
        const leader = leftScore >= rightScore ? card.left : card.right;
        messages.push(`👑 DAILY KING · ${leader.name} holding #1`);
      }
    }
    return messages;
  }

  private playMessage(text: string, urgent: boolean) {
    this.currentText = text;
    this.currentUrgent = urgent;

    if (this.fallbackTimerId) {
      clearTimeout(this.fallbackTimerId);
      this.fallbackTimerId = null;
    }

    // 렌더가 끝난 뒤 실제 폭을 재서 흐름 시간을 정한다
    setTimeout(() => {
      if (!this.running) return;
      const windowEl = this.marqueeWindowEl && this.marqueeWindowEl.nativeElement;
      const textEl = this.marqueeTextEl && this.marqueeTextEl.nativeElement;
      if (!windowEl || !textEl) return;

      if (typeof textEl.animate !== 'function') {
        // 구형 웹뷰 폴백: 흐름 없이 고정 표시 후 교체
        textEl.style.transform = 'translateX(0)';
        this.fallbackTimerId = setTimeout(() => this.playNext(), LiveNewsTickerComponent.MIN_PASS_MS);
        return;
      }

      const windowWidth = windowEl.offsetWidth || 0;
      const textWidth = textEl.offsetWidth || 0;
      const distance = windowWidth + textWidth;
      const duration = Math.max(
        LiveNewsTickerComponent.MIN_PASS_MS,
        (distance / LiveNewsTickerComponent.SPEED_PX_PER_SEC) * 1000
      );

      if (this.anim) this.anim.cancel();
      // 애니메이션 프레임마다 변경 감지를 돌릴 이유가 없다
      this.ngZone.runOutsideAngular(() => {
        this.anim = textEl.animate(
          [
            { transform: `translateX(${windowWidth}px)` },
            { transform: `translateX(-${textWidth}px)` },
          ],
          { duration, easing: 'linear' }
        );
        this.anim.onfinish = () => this.ngZone.run(() => {
          this.anim = null;
          this.playNext();
        });
      });
    });
  }
}
