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
  ViewChildren,
} from '@angular/core';
import { HttpService } from 'src/app/services/http.service';
import { DeviceIdService } from 'src/app/services/device-id.service';
import { MediaAudioService } from 'src/app/services/media-audio.service';
import { LatestContentItem, isVideoItem } from '../../latest-contents-logic';
import { VisibleVideoPlayer } from '../visible-video-player';
import { sendLatestContentEvent } from '../latest-content-events';

/**
 * 로비 가로형 미니 숏폼 "Hot Content" (2-34차 1-2) — 세로형 Latest Content 바로 아래.
 * 어드민 "가로형 미니숏폼" 편성(section=HOT)을 한 화면에 1.5~2장 보이는 가로 스크롤로 보여 준다.
 * 가장 많이 보이는 영상 카드 하나만 음소거 자동재생. 터치 동작은 세로형과 같다(영상 → 플레이어, 사진 → 상세).
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
  get items(): LatestContentItem[] {
    return this.itemList;
  }

  @Output() profileSelect = new EventEmitter<string>();
  @Output() contentSelect = new EventEmitter<LatestContentItem>();

  @ViewChildren('card') cardEls: QueryList<ElementRef<HTMLElement>>;

  playingIndex = -1;

  private itemList: LatestContentItem[] = [];
  private readonly sentEvents = new Set<string>();
  private readonly player: VisibleVideoPlayer;

  constructor(
    private http: HttpService,
    private deviceIdService: DeviceIdService,
    private ngZone: NgZone,
    public audio: MediaAudioService,
  ) {
    this.player = new VisibleVideoPlayer(
      this.audio,
      i => isVideoItem(this.itemList[i]),
      i => this.ngZone.run(() => {
        this.playingIndex = i;
        const item = this.itemList[i];
        if (item) this.track(item, 'IMPRESSION');
      }),
    );
  }

  ngAfterViewInit(): void {
    this.bindCards();
  }

  ngOnDestroy(): void {
    this.player.destroy();
  }

  start(): void {
    this.player.start();
  }

  stop(): void {
    this.player.stop();
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

  /** 카드의 소리 아이콘 — 앱 전체 소리 켬/끔 (카드 터치로 번지지 않게 막는다) */
  toggleSound(event: Event): void {
    event.stopPropagation();
    this.audio.toggleSound();
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
