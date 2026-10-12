import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnDestroy,
  Output,
  QueryList,
  ViewChild,
  ViewChildren,
} from '@angular/core';
import { HttpService } from 'src/app/services/http.service';
import { DeviceIdService } from 'src/app/services/device-id.service';
import {
  LatestContentEventType,
  LatestContentItem,
  canAutoAdvance,
  eventKey,
  halfContentHeight,
  indexFromScroll,
  nextIndex,
  normalizeIntervalMs,
  shouldBindSrc,
} from '../../latest-contents-logic';

/**
 * 로비 Latest Contents 숏폼 캐러셀 (2-33차) — 헤더와 VS 카드 사이.
 *
 * - 가로 스크롤 + scroll-snap. 메인 카드 70% 폭, 오른쪽에 다음 카드 일부가 보인다
 * - 자동 이동(어드민 간격, 기본 4초) · 마지막→첫 카드 순환 · 손으로 넘기면 6초 보류
 * - 활성 카드 1개만 음소거 자동재생. 영상 src 는 활성 ±1장에만 붙인다(WebView 메모리)
 * - 자동재생이 거부되면(iOS 저전력 등) 포스터 위에 재생 버튼을 띄운다
 * - 타이머는 로비가 start()/stop() 으로 켜고 끈다 (화면 이탈·백그라운드)
 */
@Component({
  selector: 'app-latest-contents-carousel',
  templateUrl: './latest-contents-carousel.component.html',
  styleUrls: ['./latest-contents-carousel.component.scss'],
})
export class LatestContentsCarouselComponent implements AfterViewInit, OnDestroy {
  /** 스크롤이 멈췄다고 보는 시간. 구형 iOS WebView 에는 scrollend 가 없다 */
  private static readonly SCROLL_SETTLE_MS = 120;
  /** 프로그램 스크롤(자동 이동) 중 발생하는 scroll 이벤트를 수동 조작으로 오인하지 않는 시간 */
  private static readonly PROGRAMMATIC_WINDOW_MS = 800;

  readonly defaultAvatar = 'assets/img/defaultImg/avatar.svg';

  @Input() set items(value: LatestContentItem[]) {
    this.itemList = value || [];
    this.activeIndex = 0;
    this.showPlayButton = {};
    if (this.track) {
      this.track.nativeElement.scrollLeft = 0;
    }
    // 새 목록이 그려진 뒤 활성 카드를 다시 맞춘다 (videos.changes 에서 syncPlayback)
  }
  get items(): LatestContentItem[] {
    return this.itemList;
  }

  @Input() set intervalSec(value: unknown) {
    this.intervalMs = normalizeIntervalMs(value);
    if (this.timerId) this.startTimer(); // 간격이 바뀌면 타이머 재설정
  }

  /** 프로필(아바타+이름) 터치 → 스타 ID */
  @Output() profileSelect = new EventEmitter<string>();
  /** 영상 터치 → 게시물 ID */
  @Output() contentSelect = new EventEmitter<string>();

  @ViewChild('track') track: ElementRef<HTMLElement>;
  @ViewChildren('card') cards: QueryList<ElementRef<HTMLElement>>;
  @ViewChildren('video') videos: QueryList<ElementRef<HTMLVideoElement>>;

  activeIndex = 0;
  showPlayButton: { [index: number]: boolean } = {};

  private itemList: LatestContentItem[] = [];
  private intervalMs = normalizeIntervalMs(undefined);
  private timerId: any = null;
  private running = false;
  private lastManualAt: number | null = null;
  private programmaticUntil = 0;
  private scrollSettleId: any = null;
  private videosChangesSub: { unsubscribe: () => void } | null = null;
  private readonly sentEvents = new Set<string>();
  private readonly onResize = () => this.applyHeight();

  constructor(
    private http: HttpService,
    private deviceIdService: DeviceIdService,
    private ngZone: NgZone,
  ) { }

  ngAfterViewInit(): void {
    // Ionic 이 --offset-top 을 채운 뒤 재야 하므로 한 틱 뒤에 잰다
    setTimeout(() => this.applyHeight());
    window.addEventListener('resize', this.onResize);
    this.videosChangesSub = this.videos.changes.subscribe(() => setTimeout(() => this.syncPlayback()));
    setTimeout(() => this.syncPlayback());
  }

  ngOnDestroy(): void {
    this.stop();
    window.removeEventListener('resize', this.onResize);
    if (this.videosChangesSub) this.videosChangesSub.unsubscribe();
    if (this.scrollSettleId) clearTimeout(this.scrollSettleId);
  }

  // ===== 로비가 호출하는 생명주기 =====

  /** 자동 이동 시작 + 활성 카드 재생 (로비 진입·앱 복귀) */
  start(): void {
    this.running = true;
    this.startTimer();
    this.syncPlayback();
  }

  /** 자동 이동 정지 + 모든 영상 정지 (화면 이탈·백그라운드) */
  stop(): void {
    this.running = false;
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
    this.pauseAll();
  }

  // ===== 자동 이동 =====

  private startTimer(): void {
    if (this.timerId) clearInterval(this.timerId);
    this.timerId = null;
    if (!this.running) return;
    // 타이머 틱은 Angular 존 밖에서 돌리고, 실제 상태 변경 때만 존으로 들어온다
    this.ngZone.runOutsideAngular(() => {
      this.timerId = setInterval(() => this.tick(), this.intervalMs);
    });
  }

  private tick(): void {
    if (!this.running || this.itemList.length < 2) return;
    if (!canAutoAdvance(Date.now(), this.lastManualAt)) return;
    const next = nextIndex(this.activeIndex, this.itemList.length);
    // 마지막→첫 카드는 모든 카드를 가로지르지 않도록 즉시 점프
    this.ngZone.run(() => this.goTo(next, next !== 0));
  }

  private goTo(index: number, smooth: boolean): void {
    const track = this.track ? this.track.nativeElement : null;
    const card = this.cardAt(index);
    if (track && card) {
      this.programmaticUntil = Date.now() + LatestContentsCarouselComponent.PROGRAMMATIC_WINDOW_MS;
      const left = card.offsetLeft - this.firstCardOffset();
      try {
        track.scrollTo({ left, behavior: smooth ? 'smooth' : 'auto' });
      } catch (e) {
        track.scrollLeft = left;
      }
    }
    this.setActive(index);
  }

  // ===== 수동 스와이프 =====

  onTouchStart(): void {
    this.lastManualAt = Date.now();
  }

  onScroll(): void {
    if (this.scrollSettleId) clearTimeout(this.scrollSettleId);
    this.scrollSettleId = setTimeout(() => {
      this.scrollSettleId = null;
      const track = this.track ? this.track.nativeElement : null;
      if (!track) return;
      const step = this.stepWidth();
      const index = indexFromScroll(track.scrollLeft, step, this.itemList.length);
      const manual = Date.now() > this.programmaticUntil;
      if (manual && index !== this.activeIndex) {
        this.lastManualAt = Date.now();
      }
      if (index !== this.activeIndex) {
        this.setActive(index);
      }
    }, LatestContentsCarouselComponent.SCROLL_SETTLE_MS);
  }

  // ===== 재생 제어 =====

  private setActive(index: number): void {
    this.activeIndex = index;
    const item = this.itemList[index];
    if (item) this.trackEvent(item, 'IMPRESSION');
    // src 바인딩이 바뀐 뒤 재생해야 하므로 변경 감지 한 틱 뒤
    setTimeout(() => this.syncPlayback());
  }

  /** 활성 카드만 재생, 나머지는 정지. 창 밖 카드는 src 를 떼어 디코더를 돌려준다 */
  private syncPlayback(): void {
    if (!this.videos) return;
    const length = this.itemList.length;
    // 첫 렌더는 setActive 를 거치지 않으므로 여기서도 노출을 기록한다 (세션 중복은 Set 이 거른다)
    if (this.running && this.itemList[this.activeIndex]) {
      this.trackEvent(this.itemList[this.activeIndex], 'IMPRESSION');
    }
    this.videos.forEach((ref, i) => {
      const video = ref.nativeElement;
      if (!shouldBindSrc(i, this.activeIndex, length)) {
        if (video.getAttribute('src') || video.currentSrc) {
          video.pause();
          video.removeAttribute('src');
          try { video.load(); } catch (e) { /* 이미 비어 있음 */ }
        }
        return;
      }
      if (i !== this.activeIndex || !this.running) {
        if (!video.paused) video.pause();
        return;
      }
      this.playVideo(video, i, this.itemList[i]);
    });
  }

  private playVideo(video: HTMLVideoElement, index: number, item: LatestContentItem | undefined): void {
    // 이미 재생 중이면 다시 부르지 않는다 (첫 렌더 직후 start()와 QueryList 변경이 겹쳐 두 번 들어온다)
    if (!video.paused && !video.ended) return;
    // iOS 는 속성(attribute)만으로는 부족하고 프로퍼티가 muted 여야 자동재생을 허용한다
    video.muted = true;
    video.defaultMuted = true;
    const promise = video.play();
    if (promise && typeof promise.then === 'function') {
      promise.then(() => {
        this.ngZone.run(() => {
          this.showPlayButton[index] = false;
          if (item) this.trackEvent(item, 'PLAY');
        });
      }).catch((err: any) => {
        // 자동재생 거부(NotAllowedError: 저전력 모드 등)만 재생 버튼을 띄운다.
        // 다음 카드로 넘어가며 pause() 로 끊긴 AbortError 는 정상 흐름이라 무시한다
        const name = err && err.name;
        if (name === 'NotAllowedError') {
          this.ngZone.run(() => { this.showPlayButton[index] = true; });
        }
      });
    }
  }

  private pauseAll(): void {
    if (!this.videos) return;
    this.videos.forEach(ref => {
      const video = ref.nativeElement;
      if (!video.paused) video.pause();
    });
  }

  /** 재생 버튼 오버레이 — 사용자 제스처 안에서 play() 를 불러야 한다 */
  onPlayButton(index: number, event: Event): void {
    event.stopPropagation();
    const ref = this.videos ? this.videos.toArray()[index] : null;
    if (!ref) return;
    this.playVideo(ref.nativeElement, index, this.itemList[index]);
  }

  onVideoError(index: number): void {
    // 영상을 못 불러오면 포스터만 남기고 재생 버튼은 숨긴다 (눌러도 되지 않으므로)
    this.showPlayButton[index] = false;
  }

  // ===== 클릭 =====

  onVideoTap(item: LatestContentItem): void {
    this.trackEvent(item, 'CLICK');
    this.contentSelect.emit(String(item.conId));
  }

  onProfileTap(item: LatestContentItem, event: Event): void {
    event.stopPropagation();
    this.trackEvent(item, 'PROFILE_CLICK');
    this.profileSelect.emit(item.prsId);
  }

  // ===== 통계 =====

  /** 세션 안에서 편성·종류당 1회만 보낸다. 실패는 무시 — 통계가 로비를 방해하면 안 된다 */
  private trackEvent(item: LatestContentItem, type: LatestContentEventType): void {
    const key = eventKey(item.lcId, type);
    if (this.sentEvents.has(key)) return;
    this.sentEvents.add(key);

    const send = (deviceId: string) => {
      if (!deviceId) return;
      this.http.post('/api/super/lobby/latest-contents/event', { lcId: item.lcId, eventType: type, deviceId })
        .subscribe({ next: () => { }, error: () => { } });
    };
    const cached = this.deviceIdService.get();
    if (cached) {
      send(cached);
    } else {
      this.deviceIdService.resolve().then(send).catch(() => { });
    }
  }

  // ===== 레이아웃 =====

  /** 캐러셀 높이 = 헤더를 뺀 콘텐츠 높이의 절반 (요청서 최종안) */
  private applyHeight(): void {
    const track = this.track ? this.track.nativeElement : null;
    if (!track) return;
    const content = track.closest('ion-content') as HTMLElement | null;
    const contentHeight = content ? content.clientHeight : window.innerHeight;
    let offsetTop = 0;
    if (content) {
      const raw = getComputedStyle(content).getPropertyValue('--offset-top');
      offsetTop = parseFloat(raw) || 0;
    }
    track.style.height = `${halfContentHeight(contentHeight, offsetTop)}px`;
  }

  private cardAt(index: number): HTMLElement | null {
    const list = this.cards ? this.cards.toArray() : [];
    return list[index] ? list[index].nativeElement : null;
  }

  private firstCardOffset(): number {
    const first = this.cardAt(0);
    return first ? first.offsetLeft : 0;
  }

  /** 카드 폭 + 간격. 두 번째 카드가 없으면 첫 카드 폭 */
  private stepWidth(): number {
    const first = this.cardAt(0);
    const second = this.cardAt(1);
    if (first && second) return second.offsetLeft - first.offsetLeft;
    return first ? first.offsetWidth : 0;
  }

  isSrcBound(index: number): boolean {
    return shouldBindSrc(index, this.activeIndex, this.itemList.length);
  }

  trackByLcId(_: number, item: LatestContentItem): number {
    return item.lcId;
  }

  handleImageError(event: any): void {
    if (event && event.target) event.target.src = this.defaultAvatar;
  }
}
