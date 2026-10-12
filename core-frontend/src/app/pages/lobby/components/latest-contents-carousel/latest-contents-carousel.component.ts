import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnDestroy,
  Output,
  ViewChild,
} from '@angular/core';
import { HttpService } from 'src/app/services/http.service';
import { DeviceIdService } from 'src/app/services/device-id.service';
import {
  LatestContentEventType,
  LatestContentItem,
  canAutoAdvance,
  eventKey,
  nextIndex,
  normalizeIntervalMs,
  prevIndex,
  swipeDirection,
} from '../../latest-contents-logic';

/**
 * 로비 Latest Contents 숏폼 (2-33차) — 헤더와 VS 카드 사이. 클라이언트 시안 그대로:
 * 어두운 카드 안에 왼쪽 큰 세로 영상(1:2) + 오른쪽 "Next" 썸네일, 아래에 게시자 프로필.
 *
 * - 영상은 한 번에 하나만 그린다(활성 항목). 다음 항목은 썸네일만 보여 주고 누르면 넘어간다
 * - 자동 이동(어드민 간격, 기본 4초) · 마지막→첫 항목 순환 · 손으로 넘기면 6초 보류
 * - 활성 영상은 음소거 자동재생. 재생이 시작되기 전까지는 썸네일을 덮어 둔다(가로 영상도 잘림 없이 cover)
 * - 자동재생이 거부되면(iOS 저전력 등) 썸네일 위에 재생 버튼
 * - 타이머는 로비가 start()/stop() 으로 켜고 끈다 (화면 이탈·백그라운드)
 */
@Component({
  selector: 'app-latest-contents-carousel',
  templateUrl: './latest-contents-carousel.component.html',
  styleUrls: ['./latest-contents-carousel.component.scss'],
})
export class LatestContentsCarouselComponent implements AfterViewInit, OnDestroy {
  readonly defaultAvatar = 'assets/img/defaultImg/avatar.svg';

  @Input() set items(value: LatestContentItem[]) {
    this.itemList = value || [];
    this.activeIndex = 0;
    this.isPlaying = false;
    this.showPlayButton = false;
    setTimeout(() => this.syncPlayback());
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
  /** 영상 터치 → 항목 전체 (로비가 전체 화면 플레이어를 연다) */
  @Output() contentSelect = new EventEmitter<LatestContentItem>();

  @ViewChild('video') video: ElementRef<HTMLVideoElement>;

  activeIndex = 0;
  /** 활성 영상이 실제로 재생 중인지. false 면 썸네일을 덮어 둔다 */
  isPlaying = false;
  showPlayButton = false;

  private itemList: LatestContentItem[] = [];
  private intervalMs = normalizeIntervalMs(undefined);
  private timerId: any = null;
  private running = false;
  private lastManualAt: number | null = null;
  private touchStartX = 0;
  private readonly sentEvents = new Set<string>();

  constructor(
    private http: HttpService,
    private deviceIdService: DeviceIdService,
    private ngZone: NgZone,
  ) { }

  get active(): LatestContentItem | undefined {
    return this.itemList[this.activeIndex];
  }

  /** 오른쪽 미리보기. 항목이 하나면 없다 */
  get next(): LatestContentItem | undefined {
    if (this.itemList.length < 2) return undefined;
    return this.itemList[nextIndex(this.activeIndex, this.itemList.length)];
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.syncPlayback());
  }

  ngOnDestroy(): void {
    this.stop();
  }

  // ===== 로비가 호출하는 생명주기 =====

  /** 자동 이동 시작 + 활성 영상 재생 (로비 진입·앱 복귀) */
  start(): void {
    this.running = true;
    this.startTimer();
    this.syncPlayback();
  }

  /** 자동 이동 정지 + 영상 정지 (화면 이탈·백그라운드) */
  stop(): void {
    this.running = false;
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
    const video = this.videoEl();
    if (video && !video.paused) video.pause();
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
    this.ngZone.run(() => this.goTo(nextIndex(this.activeIndex, this.itemList.length)));
  }

  private goTo(index: number): void {
    if (index === this.activeIndex) return;
    this.activeIndex = index;
    this.isPlaying = false;
    this.showPlayButton = false;
    // 바인딩(src·썸네일)이 바뀐 뒤 재생해야 하므로 변경 감지 한 틱 뒤
    setTimeout(() => this.syncPlayback());
  }

  // ===== 수동 조작 =====

  onTouchStart(event: TouchEvent): void {
    this.touchStartX = event.touches[0].clientX;
  }

  onTouchEnd(event: TouchEvent): void {
    const dir = swipeDirection(event.changedTouches[0].clientX - this.touchStartX);
    if (!dir) return;
    this.lastManualAt = Date.now();
    this.goTo(dir === 'next'
      ? nextIndex(this.activeIndex, this.itemList.length)
      : prevIndex(this.activeIndex, this.itemList.length));
  }

  /** 오른쪽 "Next" 썸네일 터치 → 다음 항목 */
  onNextTap(event: Event): void {
    event.stopPropagation();
    this.lastManualAt = Date.now();
    this.goTo(nextIndex(this.activeIndex, this.itemList.length));
  }

  // ===== 재생 제어 =====

  private videoEl(): HTMLVideoElement | null {
    return this.video ? this.video.nativeElement : null;
  }

  /** 활성 영상 재생 (로비가 활성일 때만). 노출 이벤트도 여기서 기록한다 */
  private syncPlayback(): void {
    const video = this.videoEl();
    const item = this.active;
    if (!video || !item) return;
    if (!this.running) {
      if (!video.paused) video.pause();
      return;
    }
    this.trackEvent(item, 'IMPRESSION');
    this.playVideo(video);
  }

  private playVideo(video: HTMLVideoElement): void {
    if (!video.paused && !video.ended) return;
    // iOS 는 속성(attribute)만으로는 부족하고 프로퍼티가 muted 여야 자동재생을 허용한다
    video.muted = true;
    video.defaultMuted = true;
    const promise = video.play();
    if (promise && typeof promise.then === 'function') {
      promise.catch((err: any) => {
        // 자동재생 거부(NotAllowedError: 저전력 모드 등)만 재생 버튼을 띄운다.
        // 다음 항목으로 넘어가며 끊긴 AbortError 는 정상 흐름이라 무시한다
        if (err && err.name === 'NotAllowedError') {
          this.ngZone.run(() => { this.showPlayButton = true; });
        }
      });
    }
  }

  /** 실제 프레임이 나오기 시작 — 썸네일을 걷고 재생 이벤트 기록 */
  onPlaying(): void {
    this.isPlaying = true;
    this.showPlayButton = false;
    if (this.active) this.trackEvent(this.active, 'PLAY');
  }

  onPaused(): void {
    // 백그라운드 등으로 멈추면 다시 썸네일을 덮어 마지막 프레임이 멈춘 채 보이지 않게 한다
    if (!this.running) this.isPlaying = false;
  }

  onVideoError(): void {
    // 영상을 못 불러오면 썸네일만 남긴다 (재생 버튼은 눌러도 소용없으니 숨김)
    this.isPlaying = false;
    this.showPlayButton = false;
  }

  /** 재생 버튼 오버레이 — 사용자 제스처 안에서 play() 를 불러야 한다 */
  onPlayButton(event: Event): void {
    event.stopPropagation();
    const video = this.videoEl();
    if (video) this.playVideo(video);
  }

  // ===== 클릭 =====

  onVideoTap(): void {
    const item = this.active;
    if (!item) return;
    this.trackEvent(item, 'CLICK');
    this.contentSelect.emit(item);
  }

  onProfileTap(event: Event): void {
    event.stopPropagation();
    const item = this.active;
    if (!item) return;
    this.trackEvent(item, 'PROFILE_CLICK');
    this.profileSelect.emit(item.prsId);
  }

  /** 전체 화면 플레이어에서 프로필로 이동한 경우 — 통계만 기록한다 (로비가 호출) */
  trackProfileClick(item: LatestContentItem): void {
    this.trackEvent(item, 'PROFILE_CLICK');
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

  handleImageError(event: any): void {
    if (event && event.target) event.target.src = this.defaultAvatar;
  }
}
