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
import { LatestContentItem, LatestFeedRow, buildLatestFeedRows, isVideoItem } from '../../latest-contents-logic';
import { LOBBY_FEED_AD_SLOTS } from '../../lobby-ad-slots';
import { VisibleVideoPlayer } from '../visible-video-player';
import { sendLatestContentEvent } from '../latest-content-events';

/**
 * 로비 세로형 자동재생 "Latest Content" (2-34차 1-2) — 첫 네이티브 광고와 Today's TOP 사이.
 * 어드민 "세로형 자동재생" 편성(section=LATEST)을 세로 카드로 쌓고, 콘텐츠 3·6·7번째 뒤에 네이티브 광고 자리를 둔다.
 *
 * - 광고 자리는 로비의 다른 슬롯과 같은 .lobby-ad-slot + data-slot(3·4·5). 로비가 data-slot 으로 좌표를 재서
 *   네이티브로 보낸다 (컴포넌트 안이라 로비의 @ViewChildren 으로는 안 잡히므로 DOM 으로 찾는다)
 * - 화면에 가장 많이 보이는 영상 카드 하나만 음소거 자동재생 (VisibleVideoPlayer)
 * - 영상 카드 터치 → 로비가 전체 화면 플레이어, 사진 카드 터치 → 로비가 게시물 상세
 * - 개수는 어드민 설정(1~30)만큼 서버가 잘라 보낸다. 무한 스크롤 없음
 */
@Component({
  selector: 'app-latest-feed',
  templateUrl: './latest-feed.component.html',
  styleUrls: ['./latest-feed.component.scss'],
})
export class LatestFeedComponent implements AfterViewInit, OnDestroy {
  readonly defaultAvatar = 'assets/img/defaultImg/avatar.svg';

  @Input() set items(value: LatestContentItem[]) {
    this.itemList = value || [];
    this.rows = buildLatestFeedRows(this.itemList, LOBBY_FEED_AD_SLOTS);
    // 카드 DOM 이 바뀐 뒤 관찰 대상을 다시 잡는다
    setTimeout(() => this.bindCards());
  }
  get items(): LatestContentItem[] {
    return this.itemList;
  }
  /** 로비 슬롯 상태 (전체 6칸). 이 컴포넌트는 3·4·5 칸만 읽는다 */
  @Input() adFilled: boolean[] = [];
  @Input() adHeight: number[] = [];

  @Output() profileSelect = new EventEmitter<string>();
  @Output() contentSelect = new EventEmitter<LatestContentItem>();

  @ViewChildren('card') cardEls: QueryList<ElementRef<HTMLElement>>;

  rows: LatestFeedRow[] = [];
  playingIndex = -1;

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
        const i = indexes.length ? indexes[0] : -1;
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

  /** 로비 진입·앱 복귀 */
  start(): void {
    this.player.start();
  }

  /** 로비 이탈·백그라운드·전체 화면 플레이어 열림 */
  stop(): void {
    this.player.stop();
  }

  trackByRow(_: number, row: LatestFeedRow): string {
    return row.kind === 'ad' ? 'ad' + row.slot : 'lc' + row.item.lcId;
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

  /** 전체 화면 플레이어에서 프로필로 이동한 경우 — 통계만 기록한다 (로비가 호출) */
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
