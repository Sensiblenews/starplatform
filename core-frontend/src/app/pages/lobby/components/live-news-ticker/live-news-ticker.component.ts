import { AfterViewInit, Component, ElementRef, EventEmitter, Input, NgZone, OnDestroy, Output, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { VsCard } from '../vs-carousel/vs-carousel.component';

// 티커 한 줄을 탭했을 때 로비가 이동시킬 곳
export type TickerTargetKind = 'NONE' | 'STAR' | 'VS' | 'URL';
export interface TickerTarget {
  kind: TickerTargetKind;
  starId?: string;
  vsId?: number;
  url?: string;
}

// 어드민 LIVE NEWS 문구 (GET /api/super/lobby/live-news)
export interface LiveNewsItem {
  newsId: number;
  message: string;
  targetType: string; // NONE | STAR | VS | URL
  targetValue: string | null;
}

// 티커에 흘려보낼 문장 한 건
export interface TickerItem {
  text: string;
  target: TickerTarget;
}

// 속보 한 건. priority 1(역전)은 진행 중인 문장을 끊고 즉시 나간다
export interface TickerEvent extends TickerItem {
  priority: number; // 1 = 역전(NEW KING/LEAD CHANGE), 2 = 추격(GAP 감소), 3 = 점수 변화
}

// VS 카드 한 장의 직전 상태. 폴링 응답끼리 비교해 "무슨 일이 생겼는지"를 찾는다
interface CardSnapshot {
  leftId: string;
  leftScore: number;
  rightId: string;
  rightScore: number;
}

// 화면에 동시에 흐를 수 있는 문장 2개(교차 진입용 슬롯)
interface TickerSlot {
  text: string;
  urgent: boolean;
}

/**
 * 로비 라이브 뉴스 티커 — VS 카드 바로 아래 36px 한 줄.
 *
 * 방송 자막처럼 문장이 오른쪽에서 왼쪽으로 흘러간다(클라이언트 확정 — marquee 방식).
 *
 * 평상시(Normal Loop, 2-29차)는 세 구간을 순서대로 순환한다.
 *   ① 어드민 LIVE NEWS 문구 (ON, 노출 순서대로)
 *   ② 카테고리별 #1 현황 — 로비가 이미 3초 폴링 중인 VS 카드의 1위 측을 한 줄씩 (데이터 없는 카테고리는 건너뜀)
 *   ③ 현재 VS 상황 요약
 * 새 API는 ①뿐이고 ②·③은 기존 VS 카드 데이터를 재사용한다.
 *
 * 속보(Interrupt, 2-28차 그대로): VS 카드 점수 변화를 감지해 역전(priority 1)만 진행 중인 문장을 끊고 즉시 내보낸다.
 *
 * 문장 사이 간격: 앞 문장의 꼬리가 화면 중앙을 지날 때 다음 문장을 출발시킨다(요청서 — 기존 간격의 절반).
 * 이전에는 앞 문장이 완전히 빠져나간 뒤 출발해 빈 구간이 화면 폭만큼 생겼다.
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
  // 다음 문장 출발 시점: 앞 문장 꼬리가 화면 오른쪽 끝에서 이 비율만큼 들어왔을 때 (0.5 = 화면 중앙)
  private static readonly HANDOFF_WINDOW_RATIO = 0.5;

  // 카테고리별 #1 문구 아이콘 (카테고리 7종 + GLOBAL)
  private static readonly CATEGORY_ICONS: { [key: string]: string } = {
    GLOBAL: '🌎',
    STAR: '⭐',
    CELEB: '👤',
    BRAND: '🏢',
    ORG: '🏛️',
    UNIV: '🎓',
    CITY: '🌆',
    MEDIA: '📺',
  };

  @ViewChild('marqueeWindow') marqueeWindowEl: ElementRef<HTMLElement>;
  @ViewChildren('slotEl') slotEls: QueryList<ElementRef<HTMLElement>>;

  // 티커 터치 → 로비가 문장의 타겟(스타/VS 카드/외부 URL)으로 이동한다
  @Output() focusRequest = new EventEmitter<TickerTarget>();

  slots: TickerSlot[] = [{ text: '', urgent: false }, { text: '', urgent: false }];

  private currentCards: VsCard[] = [];
  private currentNews: LiveNewsItem[] = [];
  private prevSnapshots = new Map<string, CardSnapshot>();
  private pendingEvents: TickerEvent[] = [];
  private currentItem: TickerItem | null = null;
  private queueIndex = 0;
  private nextSlot = 0;
  private running = false;
  private anims: (Animation | null)[] = [null, null];
  private handoffTimerId: any = null;
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
      this.playMessage(top, true);
    } else {
      // 일반 이벤트는 최신 상태만 유지한다. 몇 초 지난 사건을 뒤늦게 내보내지 않는다
      this.pendingEvents = events.slice(0, LiveNewsTickerComponent.MAX_PENDING);
    }
  }

  @Input()
  set news(value: LiveNewsItem[] | null) {
    this.currentNews = value || [];
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
    this.cancelAnimations();
    this.clearTimers();
  }

  onBarClick() {
    this.focusRequest.emit(this.currentItem ? this.currentItem.target : { kind: 'NONE' });
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
      const target: TickerTarget = { kind: 'VS', vsId: card.vsId };

      if (gap > 0 && leader.id !== prevLeaderId) {
        // 역전 — Daily King 카드는 왕좌 교체로 표현한다
        events.push({
          priority: 1,
          text: card.type === 'DAILY'
            ? `🚨 NEW KING · ${leader.name} takes #1!`
            : `⚡ LEAD CHANGE · ${leader.name} overtakes ${trailer.name}!`,
          target,
        });
      } else if (gap > 0 && gap < prevGap && gap <= LiveNewsTickerComponent.CHASE_GAP_MAX) {
        events.push({
          priority: 2,
          text: `🔥 ${trailer.name} closing in on ${leader.name} · GAP ${gap}`,
          target,
        });
      } else {
        events.push({
          priority: 3,
          text: `⚔️ ${card.left.name} ${leftScore} : ${rightScore} ${card.right.name}`,
          target,
        });
      }
    }
    return events;
  }

  private static cardKey(card: VsCard): string {
    return `${card.vsId}-${card.type}-${card.category}`;
  }

  // ===== Normal Loop 큐 (순수 로직 — 스펙 테스트 대상) =====

  /**
   * 속보가 없을 때 순환할 문장을 세 구간 순서로 만든다.
   * ① 어드민 문구 → ② 카테고리별 #1 → ③ VS 상황. 어드민 문구가 없으면 ②부터 시작한다.
   */
  static buildQueue(news: LiveNewsItem[], cards: VsCard[]): TickerItem[] {
    return [
      ...LiveNewsTickerComponent.buildNewsItems(news),
      ...LiveNewsTickerComponent.buildCategoryLeaderItems(cards),
      ...LiveNewsTickerComponent.buildVsStatusItems(cards),
    ];
  }

  /** ① 어드민 문구. 타겟은 저장값 그대로 */
  static buildNewsItems(news: LiveNewsItem[]): TickerItem[] {
    const items: TickerItem[] = [];
    for (const n of news || []) {
      const text = (n && n.message || '').trim();
      if (!text) continue;
      items.push({ text, target: LiveNewsTickerComponent.newsTarget(n) });
    }
    return items;
  }

  /**
   * ② 카테고리별 #1. Global Ranking 카드(type GLOBAL)의 1위 측을 카테고리마다 한 줄,
   * Daily King 전체 카드는 DAILY KING #1 한 줄. 커스텀 카드·1위 없는 카드는 건너뛴다.
   */
  static buildCategoryLeaderItems(cards: VsCard[]): TickerItem[] {
    const items: TickerItem[] = [];
    for (const card of cards || []) {
      if (!card || !card.left || !card.left.name) continue;
      const leader = card.left;
      const target: TickerTarget = { kind: 'STAR', starId: leader.id };
      if (card.type === 'GLOBAL') {
        const category = (card.category || 'GLOBAL').toUpperCase();
        const icon = LiveNewsTickerComponent.CATEGORY_ICONS[category] || '⭐';
        items.push({ text: `${icon} ${category} #1 · ${leader.name}`, target });
      } else if (card.type === 'DAILY' && (card.category || 'GLOBAL').toUpperCase() === 'GLOBAL') {
        items.push({ text: `👑 DAILY KING #1 · ${leader.name}`, target });
      }
    }
    return items;
  }

  /** ③ VS 상황 요약. 카드마다 한 줄, 탭하면 그 카드로 이동 */
  static buildVsStatusItems(cards: VsCard[]): TickerItem[] {
    const items: TickerItem[] = [];
    for (const card of cards || []) {
      if (!card || !card.left) continue;
      const target: TickerTarget = { kind: 'VS', vsId: card.vsId };
      if (!card.right) {
        items.push({ text: `👑 ${card.left.name} awaits a challenger`, target });
        continue;
      }
      const leftScore = card.left.score || 0;
      const rightScore = card.right.score || 0;
      const gap = Math.abs(leftScore - rightScore);
      items.push({ text: `⚔️ LIVE VS · ${card.left.name} vs ${card.right.name} · GAP ${gap}`, target });
    }
    return items;
  }

  /** 어드민 저장값(targetType/targetValue) → 탭 타겟. 값이 비었거나 모르는 종류면 NONE */
  static newsTarget(n: LiveNewsItem): TickerTarget {
    const type = String(n && n.targetType || 'NONE').toUpperCase();
    const value = n && n.targetValue != null ? String(n.targetValue).trim() : '';
    if (!value) return { kind: 'NONE' };
    if (type === 'STAR') return { kind: 'STAR', starId: value };
    if (type === 'VS') {
      const vsId = Number(value);
      return isNaN(vsId) ? { kind: 'NONE' } : { kind: 'VS', vsId };
    }
    if (type === 'URL') return { kind: 'URL', url: value };
    return { kind: 'NONE' };
  }

  // ===== 재생 =====

  private playNext() {
    if (!this.running) return;

    const event = this.pendingEvents.shift();
    if (event) {
      this.playMessage(event, event.priority === 1);
      return;
    }

    // 속보가 없으면 Normal Loop를 순환한다 — 로비가 멈춰 보이지 않게(요청서 확정)
    const queue = LiveNewsTickerComponent.buildQueue(this.currentNews, this.currentCards);
    if (queue.length === 0) {
      // 아직 보여줄 데이터가 없다. 폴링이 채워줄 때까지 잠시 뒤 재시도
      this.fallbackTimerId = setTimeout(() => this.playNext(), 1500);
      return;
    }
    if (this.queueIndex >= queue.length) this.queueIndex = 0;
    this.playMessage(queue[this.queueIndex], false);
    this.queueIndex++;
  }

  private playMessage(item: TickerItem, urgent: boolean) {
    this.currentItem = item;
    this.clearTimers();

    if (urgent) {
      // 속보는 흐르고 있던 문장들을 치우고 혼자 나간다
      this.cancelAnimations();
      this.nextSlot = 0;
      this.slots[1] = { text: '', urgent: false };
    }

    const slot = this.nextSlot;
    this.nextSlot = slot === 0 ? 1 : 0;
    this.slots[slot] = { text: item.text, urgent };

    // 렌더가 끝난 뒤 실제 폭을 재서 흐름 시간을 정한다
    setTimeout(() => {
      if (!this.running) return;
      const windowEl = this.marqueeWindowEl && this.marqueeWindowEl.nativeElement;
      const slotRefs = this.slotEls ? this.slotEls.toArray() : [];
      const textEl = slotRefs[slot] && slotRefs[slot].nativeElement;
      if (!windowEl || !textEl) return;

      if (typeof textEl.animate !== 'function') {
        // 구형 웹뷰 폴백: 흐름 없이 고정 표시 후 교체 (슬롯 하나만 쓴다)
        this.nextSlot = slot;
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
      // 꼬리가 화면 오른쪽 끝에서 HANDOFF_WINDOW_RATIO만큼 들어온 시점 = 다음 문장 출발
      const handoffDistance = textWidth + windowWidth * LiveNewsTickerComponent.HANDOFF_WINDOW_RATIO;
      const handoffMs = distance > 0 ? duration * (handoffDistance / distance) : duration;

      if (this.anims[slot]) this.anims[slot].cancel();
      // 애니메이션 프레임마다 변경 감지를 돌릴 이유가 없다
      this.ngZone.runOutsideAngular(() => {
        const anim = textEl.animate(
          [
            { transform: `translateX(${windowWidth}px)` },
            { transform: `translateX(-${textWidth}px)` },
          ],
          { duration, easing: 'linear' }
        );
        this.anims[slot] = anim;
        anim.onfinish = () => {
          if (this.anims[slot] === anim) this.anims[slot] = null;
        };
      });

      this.handoffTimerId = setTimeout(() => {
        this.handoffTimerId = null;
        this.playNext();
      }, handoffMs);
    });
  }

  private cancelAnimations() {
    for (let i = 0; i < this.anims.length; i++) {
      if (this.anims[i]) {
        this.anims[i].cancel();
        this.anims[i] = null;
      }
    }
  }

  private clearTimers() {
    if (this.handoffTimerId) {
      clearTimeout(this.handoffTimerId);
      this.handoffTimerId = null;
    }
    if (this.fallbackTimerId) {
      clearTimeout(this.fallbackTimerId);
      this.fallbackTimerId = null;
    }
  }
}
