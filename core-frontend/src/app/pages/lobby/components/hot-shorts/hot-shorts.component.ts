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
import { MediaAudioService } from 'src/app/services/media-audio.service';
import {
  LatestContentItem,
  canAutoAdvance,
  isVideoItem,
  nextScrollLeft,
  normalizeIntervalMs,
} from '../../latest-contents-logic';
import { VisibleVideoPlayer } from '../visible-video-player';
import { sendLatestContentEvent } from '../latest-content-events';

/**
 * 로비 가로형 미니 숏폼 "Hot Content" (2-34차 1-2) — 세로형 Latest Content 바로 아래.
 * 어드민 "가로형 미니숏폼" 편성(section=HOT)을 한 화면에 1.5~2장 보이는 가로 스크롤로 보여 준다.
 * 터치 동작은 세로형과 같다(영상 → 플레이어, 사진 → 상세).
 *
 * 2-34 후속(클라이언트 피드백 "왼쪽 1개와 오른쪽 1개 등 2개 모두 자동재생, 가로 스와이프 자동 처리"):
 * - 절반 이상 보이는 영상 카드 2장을 함께 재생한다. 소리는 왼쪽 장만(MediaAudioService.play), 오른쪽은 음소거(playMuted)
 * - 어드민 설정 간격(기본 4초)마다 한 칸씩 자동으로 넘기고 끝에서 처음으로 돌아간다. 손으로 넘기면 6초 쉰다
 * - 자동 넘김은 섹션이 화면에 절반 이상 보일 때만 돈다. 화면 밖에서는 멈춰 있다가, 보이기 시작하면
 *   간격만큼 기다린 뒤 넘기기 시작한다 (보자마자 휙 넘어가지 않게)
 */
@Component({
  selector: 'app-hot-shorts',
  templateUrl: './hot-shorts.component.html',
  styleUrls: ['./hot-shorts.component.scss'],
})
export class HotShortsComponent implements AfterViewInit, OnDestroy {
  readonly defaultAvatar = 'assets/img/defaultImg/avatar.svg';

  @Input() set items(value: LatestContentItem[]) {
    this.itemList = value || [];
    setTimeout(() => this.bindCards());
  }

  /** 자동 넘김 간격(초) — 어드민 "가로형 미니숏폼" 설정. 범위 밖이면 4초 */
  @Input() set intervalSec(value: unknown) {
    this.intervalMs = normalizeIntervalMs(value);
    this.startTimer();
  }
  get items(): LatestContentItem[] {
    return this.itemList;
  }

  @Output() profileSelect = new EventEmitter<string>();
  @Output() contentSelect = new EventEmitter<LatestContentItem>();

  @ViewChildren('card') cardEls: QueryList<ElementRef<HTMLElement>>;
  @ViewChild('track') trackEl: ElementRef<HTMLElement>;

  /** 지금 재생 중인 카드들 (최대 2장) */
  playingIndexes: number[] = [];

  private intervalMs = normalizeIntervalMs(undefined);
  private timerId: any = null;
  private running = false;
  private lastManualAt: number | null = null;
  /** 섹션(가로 트랙)이 화면에 절반 이상 보이는지 */
  private visible = false;
  private visibilityObserver: IntersectionObserver | null = null;

  private itemList: LatestContentItem[] = [];
  private readonly sentEvents = new Set<string>();
  private readonly player: VisibleVideoPlayer;

  constructor(
    private http: HttpService,
    private deviceIdService: DeviceIdService,
    private ngZone: NgZone,
    private audio: MediaAudioService,
  ) {
    this.player = new VisibleVideoPlayer(
      this.audio,
      i => isVideoItem(this.itemList[i]),
      indexes => this.ngZone.run(() => {
        this.playingIndexes = indexes;
        indexes.forEach(i => {
          const item = this.itemList[i];
          if (item) this.track(item, 'IMPRESSION');
        });
      }),
      2,
    );
  }

  ngAfterViewInit(): void {
    this.bindCards();
    const el = this.trackEl ? this.trackEl.nativeElement : null;
    if (!el) return;
    if (typeof IntersectionObserver === 'undefined') {
      this.visible = true; // 판정할 수 없는 환경 — 예전처럼 항상 넘긴다
      this.startTimer();
      return;
    }
    this.visibilityObserver = new IntersectionObserver(entries => {
      const entry = entries[entries.length - 1];
      const nowVisible = !!entry && entry.isIntersecting && entry.intersectionRatio >= 0.5;
      if (nowVisible === this.visible) return;
      this.visible = nowVisible;
      this.startTimer(); // 보이면 처음부터 간격을 다시 센다, 안 보이면 멈춘다
    }, { threshold: [0, 0.5, 1] });
    this.visibilityObserver.observe(el);
  }

  ngOnDestroy(): void {
    this.stop();
    this.player.destroy();
    if (this.visibilityObserver) this.visibilityObserver.disconnect();
  }

  start(): void {
    this.running = true;
    this.player.start();
    this.startTimer();
  }

  stop(): void {
    this.running = false;
    this.player.stop();
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  isPlayingAt(index: number): boolean {
    return this.playingIndexes.indexOf(index) >= 0;
  }

  /** 손으로 넘기기 시작하면 자동 넘김을 잠시 쉰다 */
  onManualScroll(): void {
    this.lastManualAt = Date.now();
  }

  // ===== 자동 넘김 =====

  private startTimer(): void {
    if (this.timerId) clearInterval(this.timerId);
    this.timerId = null;
    if (!this.running || !this.visible) return;
    // 타이머 틱은 Angular 존 밖에서 — 스크롤이 일어나면 IntersectionObserver 가 재생 카드를 바꾼다
    this.ngZone.runOutsideAngular(() => {
      this.timerId = setInterval(() => this.autoAdvance(), this.intervalMs);
    });
  }

  private autoAdvance(): void {
    const el = this.trackEl ? this.trackEl.nativeElement : null;
    const first = this.cardEls && this.cardEls.first ? this.cardEls.first.nativeElement : null;
    if (!el || !first || this.itemList.length < 2) return;
    if (!canAutoAdvance(Date.now(), this.lastManualAt)) return;
    const gap = parseFloat(getComputedStyle(el).columnGap || '0') || 0;
    const step = first.offsetWidth + gap;
    const target = nextScrollLeft(el.scrollLeft, step, el.scrollWidth - el.clientWidth);
    el.scrollTo({ left: target, behavior: 'smooth' });
  }

  trackByItem(_: number, item: LatestContentItem): number {
    return item.lcId;
  }

  isVideo(item: LatestContentItem): boolean {
    return isVideoItem(item);
  }

  onPlaying(item: LatestContentItem): void {
    this.track(item, 'PLAY');
  }

  onCardTap(item: LatestContentItem): void {
    this.track(item, 'CLICK');
    this.contentSelect.emit(item);
  }

  onProfileTap(item: LatestContentItem, event: Event): void {
    event.stopPropagation();
    this.track(item, 'PROFILE_CLICK');
    this.profileSelect.emit(item.prsId);
  }

  trackProfileClick(item: LatestContentItem): void {
    this.track(item, 'PROFILE_CLICK');
  }

  handleImageError(event: any): void {
    if (event && event.target) event.target.src = this.defaultAvatar;
  }

  private bindCards(): void {
    if (!this.cardEls) return;
    this.player.setCards(this.cardEls.toArray().map(ref => ref.nativeElement));
  }

  private track(item: LatestContentItem, type: 'IMPRESSION' | 'PLAY' | 'CLICK' | 'PROFILE_CLICK'): void {
    sendLatestContentEvent(this.http, this.deviceIdService, this.sentEvents, item, type);
  }
}
