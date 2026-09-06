import { AfterViewInit, Component, ElementRef, NgZone, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpService } from '../../services/http.service';
import { Platform, PopoverController, AlertController, ModalController, NavController, IonInfiniteScroll } from '@ionic/angular';
import NativeBridge from 'src/app/plugins/native-bridge';
import { AdMobService } from 'src/app/services/ad-mob.service';
import { AdProtectionService } from 'src/app/services/ad-protection.service';
import { StarMenuComponent } from './star-menu.component';
import { Share } from '@capacitor/share';
import { Haptics, ImpactStyle } from '@capacitor/haptics';
import { CommentModalComponent } from './modals/comment-modal.component';
import { MyInsightModalComponent } from './modals/my-insight-modal.component';
import { DeepLinkService } from 'src/app/services/deep-link.service';
import { finalize } from 'rxjs/operators';
import { WriteModalService } from 'src/app/services/write-modal.service';
import { DeviceIdService } from 'src/app/services/device-id.service';
import { environment } from 'src/environments/environment';
import { PerfTraceService } from 'src/app/services/perf-trace.service';
import { DmService } from 'src/app/services/dm.service';
import { openDmChat } from 'src/app/modals/dm-chat/dm-chat.component';


@Component({
  selector: 'app-star-page',
  templateUrl: './star-page.page.html',
  styleUrls: ['./star-page.page.scss'],
})
export class StarPagePage implements OnInit, AfterViewInit, OnDestroy {
  public defaultAvatar = 'assets/img/defaultImg/avatar.svg';
  starId: string;
  starInfo: any = {};
  gallery: any[] = [];
  feedList: any[] = [];

  isFavorite: boolean = false;
  followerCount: number = 0;

  isAdLocked: boolean = false;
  private clickListener: any;
  private lastScrollTop: number = 0;

  private isGoingToDetail: boolean = false;

  @ViewChildren('feedVideo') videoElements: QueryList<ElementRef>;
  @ViewChild(IonInfiniteScroll) infiniteScroll: IonInfiniteScroll;
  private observer: IntersectionObserver;

  // 피드는 한 번에 다 내려받지 않는다. 스타의 전체 피드를 통째로 받으면
  // 응답 크기와 초기 DOM이 같이 커져 Android에서 첫 화면이 늦어진다(2-26차).
  private static readonly FEED_PAGE_SIZE = 10;
  // 백엔드 SuperAppService.MAX_FEED_LIMIT과 같은 값. 복귀 갱신 시 한 번에 다시 받을 상한
  private static readonly FEED_REFRESH_MAX = 100;
  private feedOffset = 0;
  hasMoreFeeds = true;
  private isLoadingMoreFeeds = false;

  viewCount = 0;
  displayViewCount = 0;
  showPlus = false;

  // 🌟 [신규] 랭킹 및 전체 스타 수 변수 추가
  globalRank: number = 0;
  totalStars: number = 0;

  // 🌟 [신규] 프로필 뷰어 모달 상태
  isProfileViewerOpen: boolean = false;

  starAction = {
    hasLiked: false,
    likeCount: 124,
    commentCount: 9,
    shareCount: 3
  };

  deviceId: string = '';

  private pollingIntervalId: any;
  private pollingStartTimeoutId: any = null;
  private lastCheckTime: string = '';

  // 두 번째 이후 진입(= 상세페이지에서 복귀)인지 구분한다.
  // 복귀 시에는 스켈레톤을 다시 띄우지 않고 화면을 유지한 채 뒤에서만 갱신한다.
  private hasEnteredBefore = false;

  // 진입 구간 계측을 이미 닫았는지 (첫 미디어 1건에서만 닫는다)
  private perfTraceClosed = false;

  isAdmin: boolean = false;
  currentAdminId: string = '';

  // 🌟 [신규] 온보딩 및 추천 페이지 관련 변수
  isClaimed: boolean = true; // 기본값은 주인이 있는 것으로 설정 (API에서 받아옴)
  recommendedPages: any[] = []; // 하단 무한 스크롤(Next Page)용 추천 리스트

  isStar: boolean = false; // 🌟 추가: 현재 사용자가 이 페이지의 주인인지 여부
  private paramSub: any;

  // 첫 진입 시에만 스켈레톤을 노출한다 (새로고침·재조회에는 관여하지 않음)
  isLoadingStar = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpService,
    private popoverCtrl: PopoverController,
    private platform: Platform,
    private adMobService: AdMobService,
    private adProtection: AdProtectionService,
    private ngZone: NgZone,
    private alertCtrl: AlertController,
    private modalCtrl: ModalController,
    private writeModalService: WriteModalService,
    private navCtrl: NavController,
    private deepLink: DeepLinkService,
    private deviceIdService: DeviceIdService,
    private perf: PerfTraceService,
    private dm: DmService,
  ) { }

  // 로그인한 스타가 다른 스타 페이지를 볼 때만 채팅 아이콘 (2-29차 메신저, 스타 소유자끼리 1:1)
  get canMessage(): boolean {
    return this.dm.canMessage(this.starId);
  }

  async openDm(event?: Event) {
    if (event) event.stopPropagation();
    if (!this.canMessage) return;
    try { Haptics.impact({ style: ImpactStyle.Light }); } catch (e) { }
    await openDmChat(this.modalCtrl, this.dm, {
      peerId: this.starId,
      peerName: this.starInfo?.name || '',
      peerImage: this.starInfo?.image || null,
    });
  }

  async ngOnInit() {
    this.perf.mark('star:ngOnInit');
    this.isAdmin = localStorage.getItem('isAdmin') === 'true';
    this.currentAdminId = localStorage.getItem('adminId') || '';

    // 기기 ID는 앱 기동 시 한 번 조회해 캐시해 둔다(DeviceIdService).
    // 캐시가 차 있으면 네이티브 브리지를 기다리지 않고 바로 첫 요청을 보낸다.
    // 비어 있을 때만 예전처럼 기다린다 — IS_LIKED 판정에 기기 ID가 필요하기 때문이다.
    this.deviceId = this.deviceIdService.get();
    if (!this.deviceId) {
      this.deviceId = await this.deviceIdService.resolve();
    }

    this.paramSub = this.route.paramMap.subscribe(params => {
      this.starId = params.get('starId');
      this.isStar = localStorage.getItem('isStar') === 'true' && localStorage.getItem('starId') === this.starId;

      this.resetFeedPaging();
      this.loadStarDetail();
      this.checkFavoriteState();
    });
  }

  checkFavoriteState() {
    const favList = JSON.parse(localStorage.getItem('favorite_stars') || '[]');
    this.isFavorite = favList.includes(this.starId);
  }

  getEffectiveDeviceId(): string {
    const isStarLoggedIn = localStorage.getItem('isStar') === 'true';
    const loggedInStarId = localStorage.getItem('starId');
    if (isStarLoggedIn && loggedInStarId) {
      return loggedInStarId;
    }
    return this.deviceId;
  }

  toggleFavorite() {
    let favList = JSON.parse(localStorage.getItem('favorite_stars') || '[]');

    if (this.isFavorite) {
      favList = favList.filter((id: string) => id !== this.starId);
      this.followerCount = Math.max(0, this.followerCount - 1);
    } else {
      favList.push(this.starId);
      this.followerCount++;
    }

    localStorage.setItem('favorite_stars', JSON.stringify(favList));
    this.isFavorite = !this.isFavorite;

    this.http.post('/api/super/star/toggleFollow', { starId: this.starId, isAdd: this.isFavorite, deviceId: this.getEffectiveDeviceId() }).subscribe();
  }

  // 🌟 [신규] 포인트 계산 (조회수 / 10 의 내림)
  getPoints(views: number): number {
    return Math.floor((views || 0) / 10);
  }

  ngAfterViewInit(): void {
    this.perf.mark('star:first-render');
    this.videoElements.changes.subscribe(() => {
      this.initIntersectionObserver();
    });
  }

  ngOnDestroy(): void {
    this.removeClickListener();
    this.stopPolling();
    if (this.paramSub) {
      this.paramSub.unsubscribe();
    }
  }

  async ionViewDidEnter() {
    // 상세페이지에서 돌아온 경우에만 조용히 최신화한다.
    // 화면은 그대로 두고 데이터만 갈아끼우므로 스켈레톤이 다시 뜨지 않는다(2-26차).
    if (this.hasEnteredBefore) {
      this.refreshInBackground();
    }
    this.hasEnteredBefore = true;

    // 🎬 돌아왔을 때 동영상 재개
    if (!this.isGoingToDetail) {
      this.resumeAllVideos();
    }

    // 🌟 [봇 필터 1] 검색 엔진 봇이면 여기서 함수 실행 종료 (광고 요청 안 함)
    const isBot = /bot|crawler|spider|google|bing|yandex|duckduck/i.test(navigator.userAgent || '');
    if (isBot) {
      console.log('🤖 봇 진입 감지 - 배너 광고를 노출하지 않습니다.');
      return;
    }

    // 🌟 방문자 수 실시간 폴링 시작 (화면에 보일 때만 가동, ionViewWillLeave에서 정지).
    // 첫 틱을 진입 직후에 쏘면 초기 로딩과 경합해 Android에서 첫 화면이 늦어진다.
    // 핵심 화면이 그려질 여유를 준 뒤 시작한다(2-26차).
    this.stopPolling();
    this.pollingStartTimeoutId = setTimeout(() => {
      this.pollingStartTimeoutId = null;
      this.startPolling();
    }, 2000);

    if (this.platform.is('capacitor')) {
      let canShow = await this.adProtection.shouldShowAd(this.starId);
      console.log("Ad Protection Check for Star ID:", this.starId, "Can Show Ad:", canShow);
      // canShow = true // 개발용

      if (canShow) {
        NativeBridge.setShow({ show: true, page: 'star' });
        this.isAdLocked = false;

        // 🌟 봇이 아닐 때만 1초 뒤에 조회수(노출) 로그 전송
        setTimeout(() => {
          this.sendAdLog('NATIVE', 'IMPRESSION');
        }, 1000);

        this.setupClickListener();
      } else {
        NativeBridge.setShow({ show: false, page: 'star' });
        this.isAdLocked = true;
      }
    }
  }

  async ionViewWillLeave() {
    // 🌟 [봇 필터 2] 봇인지 확인
    const isBot = /bot|crawler|spider|google|bing|yandex|duckduck/i.test(navigator.userAgent || '');

    if (this.platform.is('capacitor')) {
      NativeBridge.setShow({ show: false, page: 'star' });
      this.removeClickListener();

      // 🌟 봇이 아니고 상세페이지로 가는게 아닐 때만 전면광고 띄움
      if (!this.isGoingToDetail && !isBot) {
        console.log("Leaving to Lobby/Home -> Check Interstitial Conditions");

        if (!this.isAdLocked) {
          const isShown = await this.adMobService.showInterstitial();

          if (isShown) {
            this.sendAdLog('INTERSTITIAL', 'IMPRESSION');
          }
        }
      } else {
        this.isGoingToDetail = false;
      }
    }

    this.stopPolling();
  }

  setupClickListener() {
    this.removeClickListener();

    this.clickListener = () => {
      console.log('⚡️ Web ad click detected! (Event Received)');

      this.ngZone.run(() => {
        this.handleAdClick();
      });
    };

    window.addEventListener('ad_click_detected', this.clickListener);
  }

  removeClickListener() {
    if (this.clickListener) {
      window.removeEventListener('ad_click_detected', this.clickListener);
      this.clickListener = null;
    }
  }

  async handleAdClick() {
    console.log('🔒 Ad Clicked -> Activating 24h Lock');

    this.sendAdLog('NATIVE', 'CLICK');

    await this.adProtection.lockAd(this.starId);
    NativeBridge.setShow({ show: false, page: 'star' });
    this.isAdLocked = true;
    this.removeClickListener();
  }

  onScroll(event: any) {
    if (this.platform.is('capacitor')) {
      const scrollTop = event.detail.scrollTop;

      let direction = 'down';
      if (scrollTop < this.lastScrollTop) {
        direction = 'up';
      }

      NativeBridge.updateAdPosition({
        value: scrollTop,
        direction: direction
      });

      this.lastScrollTop = scrollTop;
    }
  }

  /**
   * 스타 상세 + 피드 첫 페이지를 불러온다.
   *
   * 호출부는 전부 사용자가 명시적으로 일으킨 동작이다(첫 진입, 당겨서 새로고침,
   * 글 작성·삭제, 프로필 수정). 그래서 목록을 병합하지 않고 새로 만든다 —
   * 사용자가 최신 상태를 요구한 것이므로 이전 항목을 살려둘 이유가 없다.
   *
   * 병합은 복귀 시 조용한 갱신(refreshInBackground)에서만 쓴다. 그쪽은 화면이
   * 그대로 떠 있는 상태라 이미지가 다시 로딩되면 깜빡임으로 보이기 때문이다.
   */
  loadStarDetail() {
    this.perf.mark('star:api-request');
    this.resetFeedPaging();

    this.http.post(`/api/super/star/${this.starId}`, {
      deviceId: this.deviceId,
      // 본인 페이지면 검수 대기 글도 받는다. 서버는 이미지 주소를 주지 않고
      // 상태값만 내려주므로, 앱이 "검토 중" 자리를 대신 그린다(2-26차)
      viewerStarId: this.isStar ? this.starId : '',
      // 서버가 starToken까지 검증해야 소유자로 인정한다. 이 값이 검수 대기 이미지 접근으로 이어진다(2-26차)
      starToken: this.isStar ? (localStorage.getItem('starToken') || '') : '',
      limit: StarPagePage.FEED_PAGE_SIZE,
      offset: 0
    }).pipe(
      // 성공/실패와 무관하게 스켈레톤을 해제한다
      finalize(() => this.isLoadingStar = false)
    ).subscribe((res: any) => {
      this.perf.mark('star:api-response');
      if (res.result !== 'OK') return;

      this.applyStarInfo(res.starInfo);

      const photos = res.starInfo.photos || [];
      this.feedOffset = photos.length;
      this.hasMoreFeeds = photos.length >= StarPagePage.FEED_PAGE_SIZE;
      this.resetInfiniteScroll();

      this.feedList = this.mergeFeed(photos, true, StarPagePage.FEED_PAGE_SIZE);
      this.insertAdSlots();

      // 미디어가 하나도 없으면 onMediaLoaded가 오지 않으므로 여기서 구간을 닫는다
      if (!this.perfTraceClosed && !this.feedList.some((item: any) => !item.isAd && !item.isLoaded)) {
        this.perfTraceClosed = true;
        this.perf.end('star:no-media');
      }

      // 🌟 [신규] OWNER_EMAIL 또는 PRS_PWD가 존재하면 주인이 있는 페이지! (백엔드 IS_CLAIMED 사용)
      this.isClaimed = this.starInfo.IS_CLAIMED === 'Y';
      // 🌟 [신규] 페이지 디테일 로딩 시 하단 추천 페이지도 같이 불러옵니다.
      this.loadRecommendedPages();
    });
  }

  /**
   * 상세페이지에서 복귀했을 때의 조용한 최신화.
   * 스켈레톤을 다시 띄우지 않고, 이미 로드한 페이지 수만큼만 다시 받아 병합한다.
   */
  private refreshInBackground() {
    if (this.isLoadingStar || !this.starId) return;

    // 무한 스크롤로 더 내려받았다면 그만큼 다시 받아야 앞부분만 남고 잘리지 않는다.
    // 상한을 넘는 뒤쪽은 mergeFeed가 그대로 보존한다.
    const limit = Math.min(
      Math.max(this.feedOffset, StarPagePage.FEED_PAGE_SIZE),
      StarPagePage.FEED_REFRESH_MAX
    );

    this.http.post(`/api/super/star/${this.starId}`, {
      deviceId: this.deviceId,
      viewerStarId: this.isStar ? this.starId : '',
      // 서버가 starToken까지 검증해야 소유자로 인정한다. 이 값이 검수 대기 이미지 접근으로 이어진다(2-26차)
      starToken: this.isStar ? (localStorage.getItem('starToken') || '') : '',
      limit: limit,
      offset: 0
    }).subscribe((res: any) => {
      if (res.result !== 'OK') return;

      this.applyStarInfo(res.starInfo);

      const photos = res.starInfo.photos || [];
      this.feedList = this.mergeFeed(photos, false, limit);
      this.insertAdSlots();

      // 보존한 뒤쪽까지 포함한 실제 길이로 다음 페이지 위치를 다시 잡는다
      this.feedOffset = this.feedList.filter((item: any) => !item.isAd).length;
      this.hasMoreFeeds = this.hasMoreFeeds || photos.length >= limit;
      this.resetInfiniteScroll();

      this.isClaimed = this.starInfo.IS_CLAIMED === 'Y';
    });
  }

  /** 무한 스크롤 — 다음 페이지를 이어 붙인다 (기존 항목은 건드리지 않는다) */
  loadMoreFeeds(event?: any) {
    if (this.isLoadingMoreFeeds || !this.hasMoreFeeds) {
      if (event) event.target.complete();
      return;
    }
    this.isLoadingMoreFeeds = true;

    this.http.post(`/api/super/star/${this.starId}`, {
      deviceId: this.deviceId,
      viewerStarId: this.isStar ? this.starId : '',
      // 서버가 starToken까지 검증해야 소유자로 인정한다. 이 값이 검수 대기 이미지 접근으로 이어진다(2-26차)
      starToken: this.isStar ? (localStorage.getItem('starToken') || '') : '',
      limit: StarPagePage.FEED_PAGE_SIZE,
      offset: this.feedOffset
    }).pipe(
      finalize(() => {
        this.isLoadingMoreFeeds = false;
        if (event) event.target.complete();
      })
    ).subscribe((res: any) => {
      if (res.result !== 'OK') return;

      const photos = res.starInfo && res.starInfo.photos ? res.starInfo.photos : [];
      this.hasMoreFeeds = photos.length >= StarPagePage.FEED_PAGE_SIZE;
      this.feedOffset += photos.length;

      const known = this.collectFeedKeys();
      const added = photos
        .filter((item: any) => !known.has(String(item.CON_ID)))
        .map((item: any) => this.prepareFeedItem(item));

      // 광고 슬롯을 걷어낸 뒤 이어 붙이고 다시 삽입한다
      this.feedList = this.feedList.filter((item: any) => !item.isAd).concat(added);
      this.insertAdSlots();
    });
  }

  private resetFeedPaging() {
    this.feedOffset = 0;
    this.hasMoreFeeds = true;
    this.isLoadingMoreFeeds = false;
  }

  private resetInfiniteScroll() {
    if (this.infiniteScroll) {
      this.infiniteScroll.disabled = !this.hasMoreFeeds;
    }
  }

  private applyStarInfo(starInfo: any) {
    this.starInfo = starInfo;
    this.followerCount = starInfo.FOLLOWER_CNT || 0;

    // 🌟 [신규] 백엔드에서 받은 랭킹/전체 수 세팅
    this.globalRank = starInfo.GLOBAL_RANK || 0;
    this.totalStars = starInfo.TOTAL_STARS || 0;

    this.viewCount = starInfo.viewCount || 0;
    this.displayViewCount = this.viewCount;

    this.starAction = {
      hasLiked: starInfo.IS_LIKED == 1 || starInfo.IS_LIKED === true,
      likeCount: starInfo.LIKE_CNT || 0,
      commentCount: starInfo.COMMENT_CNT || 0,
      shareCount: starInfo.SHARE_CNT || 0
    };
  }

  private prepareFeedItem(item: any): any {
    if (item.MEDIA_TYPE === 'VIDEO') item.isMuted = true;

    // 검수 대기 글은 서버가 이미지 주소를 주지 않는다.
    // 단 작성자 본인에게는 단기 접근 토큰이 함께 내려오므로 그것으로 원본을 불러온다(2-26차)
    const isPending = item.MDR_STATUS === 'PENDING';
    item.pendingImageUrl = (isPending && item.pendingImageToken)
      ? `${environment.apiBaseURL}/api/super/media/pending?t=${encodeURIComponent(item.pendingImageToken)}`
      : null;

    // 볼 권한이 없는 대기 글에만 "검토 중" 자리를 그린다
    item.isUnderReview = isPending && !item.pendingImageUrl;

    // 미디어가 전혀 없는 텍스트 피드인 경우 로딩 완료(isLoaded = true) 처리
    const hasMedia = item.MEDIA_TYPE === 'VIDEO' || item.image || item.pendingImageUrl
      || item.youtubeUrl || item.YOUTUBE_URL;
    item.isLoaded = (hasMedia && !item.isUnderReview) ? false : true;

    item.hasLiked = item.IS_LIKED == 1 || item.IS_LIKED === true;
    item.likeCount = item.LIKE_CNT || 0;
    item.commentCount = item.COMMENT_CNT || 0;
    return item;
  }

  private collectFeedKeys(): Set<string> {
    const keys = new Set<string>();
    this.feedList.forEach((item: any) => {
      if (!item.isAd && item.CON_ID !== undefined && item.CON_ID !== null) {
        keys.add(String(item.CON_ID));
      }
    });
    return keys;
  }

  /**
   * 새 응답을 기존 목록에 겹쳐 넣는다.
   *
   * 같은 CON_ID가 이미 있으면 기존 객체를 그대로 재사용하고 값만 덮어쓴다.
   * 배열을 통째로 갈아끼우면 trackBy가 같은 키를 봐도 항목 객체가 달라져
   * isLoaded가 false로 돌아가고, 이미지가 다시 페이드인하면서 깜빡임으로 보인다(2-26차).
   */
  private mergeFeed(incoming: any[], rebuild: boolean, requestedLimit: number): any[] {
    // 광고 슬롯은 병합 대상이 아니다. insertAdSlots()가 뒤에서 다시 꽂는다
    const previous = this.feedList.filter((item: any) => !item.isAd);

    const existing = new Map<string, any>();
    previous.forEach((item: any) => {
      if (item.CON_ID !== undefined && item.CON_ID !== null) {
        existing.set(String(item.CON_ID), item);
      }
    });

    // rebuild면 서버가 준 그대로 새로 만든다. 단 isLoaded만은 넘겨받는다 —
    // 그건 서버 상태가 아니라 "이 주소의 이미지가 이미 화면에 그려져 있다"는 DOM 상태다.
    // trackBy가 같은 키를 보고 DOM을 유지하는데 src까지 같으면 브라우저가 다시
    // 불러오지 않아 load 이벤트가 오지 않는다. 그대로 두면 스피너가 영원히 돈다.
    if (rebuild || this.feedList.length === 0) {
      return incoming.map((raw: any) => {
        const prepared = this.prepareFeedItem(raw);
        const old = existing.get(String(raw.CON_ID));
        if (old && old.isLoaded && this.mediaKeyOf(old) === this.mediaKeyOf(prepared)) {
          prepared.isLoaded = true;
        }
        return prepared;
      });
    }

    const merged = incoming.map((raw: any) => {
      const prepared = this.prepareFeedItem(raw);
      const old = existing.get(String(raw.CON_ID));
      if (!old) return prepared;

      // 화면 상태(로딩 완료 여부, 음소거)는 유지하고 서버 값만 갱신한다
      const fresh: any = Object.assign({}, prepared);
      delete fresh.isLoaded;
      delete fresh.isMuted;
      Object.assign(old, fresh);
      return old;
    });

    // 요청한 만큼 꽉 채워 왔을 때만 "그 뒤로 더 있다"고 볼 수 있다.
    // 덜 왔다면 서버가 가진 전부라는 뜻이므로, 목록에 없는 항목은 삭제된 것이다.
    //
    // 이 구분이 없으면 글을 지웠을 때 incoming이 한 건 짧아지는 것을
    // "갱신 범위 밖"으로 오인해 방금 지운 글을 도로 살려낸다.
    if (incoming.length < requestedLimit) {
      return merged;
    }

    // 무한 스크롤로 더 내려받은 뒤쪽은 이번 갱신 대상이 아니므로 그대로 남긴다
    const incomingKeys = new Set(incoming.map((raw: any) => String(raw.CON_ID)));
    const tail = previous
      .slice(incoming.length)
      .filter((item: any) => !incomingKeys.has(String(item.CON_ID)));

    return merged.concat(tail);
  }

  /**
   * 지금 그려질 미디어 주소를 한 문자열로 묶는다.
   * 이 값이 그대로면 img/video의 src도 그대로라 브라우저가 다시 불러오지 않는다.
   */
  private mediaKeyOf(item: any): string {
    return [item && item.image, item && item.pendingImageUrl, item && item.MEDIA_URL]
      .map(v => v || '')
      .join('|');
  }

  /** 피드 항목 추적 키. 광고 슬롯과 콘텐츠를 접두어로 구분한다 */
  trackByFeed(index: number, item: any): string {
    return item && item.isAd ? `ad-${item.adId}` : `con-${item && item.CON_ID}`;
  }

  toggleLike(target: any, type: 'star' | 'feed', event?: Event) {
    if (event) event.stopPropagation();

    const targetId = type === 'star' ? this.starId : target.CON_ID;
    const payload = {
      targetType: type.toUpperCase(),
      targetId: targetId,
      deviceId: this.getEffectiveDeviceId()
    };

    this.http.post('/api/super/like/toggle', payload).subscribe((res: any) => {
      if (res.result === 'OK') {
        if (type === 'star') {
          this.starAction.hasLiked = res.isLiked;
          this.starAction.likeCount += (res.isLiked ? 1 : -1);
        } else {
          target.hasLiked = res.isLiked;
          target.likeCount += (res.isLiked ? 1 : -1);
        }
      }
    });
  }

  async shareContent(id: string | number, type: 'star' | 'feed', event?: Event) {
    if (event) event.stopPropagation();

    try {
      await Haptics.impact({ style: ImpactStyle.Light });
    } catch (e) { }

    const baseUrl = 'https://witch-hunting.com';
    const link = type === 'star' ? `${baseUrl}/star/${id}` : `${baseUrl}/post/${id}`;

    try {
      await Share.share({ title: 'Star Platform', url: link });

      this.http.post('/api/super/share/add', {
        targetType: type.toUpperCase(),
        targetId: id
      }).subscribe((res: any) => {
        if (res.result === 'OK' && type === 'star') {
          this.starAction.shareCount++;
        }
      });
    } catch (e) { console.log('Share canceled'); }
  }

  async openCommentModal(target: any, type: 'star' | 'feed', event?: Event) {
    if (event) event.stopPropagation();
    const targetId = type === 'star' ? this.starId : target.CON_ID;

    const modal = await this.modalCtrl.create({
      component: CommentModalComponent,
      componentProps: { type: type, targetId: targetId, deviceId: this.deviceId },
      initialBreakpoint: 1,
      breakpoints: [0, 1]
    });

    await modal.present();

    const { data } = await modal.onDidDismiss();
    if (data && data.count !== undefined) {
      if (type === 'star') this.starAction.commentCount = data.count;
      else target.commentCount = data.count;
    }
  }

  onMediaLoaded(item: any) {
    item.isLoaded = true;

    // 첫 미디어가 그려지는 시점까지를 진입 구간의 끝으로 본다 (계측 켠 경우에만 동작)
    if (!this.perfTraceClosed) {
      this.perfTraceClosed = true;
      this.perf.end('star:first-media');
    }
  }

  async presentPopover(ev: any) {
    const popover = await this.popoverCtrl.create({
      component: StarMenuComponent,
      event: ev,
      translucent: false,
      showBackdrop: true,
      dismissOnSelect: true,
      cssClass: 'star-menu-popover'
    });

    await popover.present();

    const { data } = await popover.onDidDismiss();

    if (data && data.action) {
      this.handleMenuAction(data.action);
    }
  }

  handleMenuAction(action: string) {
    switch (action) {
      case 'search':
        this.router.navigate(['/lobby']);
        break;

      case 'refresh':
        this.loadStarDetail();
        break;

      case 'report':
        this.openReportDialog();
        break;
    }
  }

  async openReportDialog() {
    const alert = await this.alertCtrl.create({
      header: '🚨 Report Creator/Post',
      message: 'Please provide details for the report. Our team will review it shortly.',
      inputs: [
        {
          name: 'reportContent',
          type: 'textarea',
          placeholder: 'e.g., Inappropriate photos, abusive language, etc.'
        }
      ],
      buttons: [
        {
          text: 'Cancel',
          role: 'cancel'
        },
        {
          text: 'Submit via Email',
          handler: (data) => {
            if (!data.reportContent || data.reportContent.trim() === '') {
              return false;
            }
            this.sendReportEmail(data.reportContent);
          }
        }
      ]
    });

    await alert.present();
  }

  sendReportEmail(content: string) {
    const email = 'witchhunting777@gmail.com';

    const starName = this.starInfo?.name || this.starId;
    const subject = encodeURIComponent(`[StarPlatform Report] Report regarding ${starName} page`);

    const bodyText = `Please submit your report below.\n\n- Target Creator: ${starName} (ID: ${this.starId})\n- Report Details:\n${content}\n\n------------------------\n* Please do not modify the form above for faster processing.`;
    const body = encodeURIComponent(bodyText);

    window.location.href = `mailto:${email}?subject=${subject}&body=${body}`;
  }

  sendAdLog(adType: string, action: string) {
    const isBot = /bot|crawler|spider|google|bing|yandex|duckduck/i.test(navigator.userAgent || '');
    if (isBot) {
      console.log('🤖 봇 트래픽 - 통계 로그 전송을 차단합니다.');
      return;
    }

    if (this.isStar) {
      console.log('🛡️ 방문자가 본인(스타)이므로 조회수/광고 로그를 전송하지 않습니다.');
      return;
    }

    if (action === 'IMPRESSION') {
      const now = Date.now();
      const cooldown = 60 * 1000;

      let lastViewed = JSON.parse(localStorage.getItem('ad_last_viewed') || '{}');

      if (lastViewed[this.starId]) {
        const timeDiff = now - lastViewed[this.starId];

        if (timeDiff < cooldown) {
          const remainSec = Math.ceil((cooldown - timeDiff) / 1000);
          console.log(`⏳ Abuse prevention: Skipped ad impression for ${this.starId} (Remaining time: ${remainSec}s)`);
          return;
        }
      }

      lastViewed[this.starId] = now;
      localStorage.setItem('ad_last_viewed', JSON.stringify(lastViewed));
    }

    let os = 'ANDROID';
    if (this.platform.is('ios') || this.platform.is('ipad') || this.platform.is('iphone')) {
      os = 'IOS';
    }

    const payload = {
      MEM_ID: this.getEffectiveDeviceId() || 'GUEST',
      PRS_ID: this.starId,
      AD_TYPE: adType,
      OS: os,
      ACTION: action
    };

    console.log('📊 Sending Ad Log:', payload);

    this.http.post('/api/ad/log', payload).subscribe({
      next: (res) => console.log('✅ Ad Log Saved'),
      error: (err) => console.error('❌ Ad Log Failed', err)
    });
  }

  handleImageError(event: any) {
    event.target.src = this.defaultAvatar;
  }

  async goDetail(conId: number) {
    try {
      await Haptics.impact({ style: ImpactStyle.Light });
    } catch (e) { }

    // 🎬 모든 동영상 일시 정지
    this.pauseAllVideos();

    this.isGoingToDetail = true;
    this.router.navigate(['/feed-detail', conId]);
  }

  // 🎬 동영상 일시 정지
  pauseAllVideos() {
    this.videoElements.forEach(el => {
      const video = el.nativeElement as HTMLVideoElement;
      if (video && !video.paused) {
        video.pause();
      }
    });
  }

  // 🎬 동영상 재생 (돌아왔을 때)
  resumeAllVideos() {
    this.videoElements.forEach(el => {
      const video = el.nativeElement as HTMLVideoElement;
      if (video) {
        video.play().catch(err => {
          console.log('Autoplay blocked:', err);
        });
      }
    });
  }

  initIntersectionObserver() {
    if (this.observer) {
      this.observer.disconnect();
    }

    const options = {
      root: null,
      rootMargin: '0px',
      threshold: 0.8
    };

    this.observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        const video = entry.target as HTMLVideoElement;

        if (entry.isIntersecting) {
          video.play().catch(err => {
            console.log('Autoplay blocked:', err);
          });
        } else {
          video.pause();
        }
      });
    }, options);

    this.videoElements.forEach(el => {
      this.observer.observe(el.nativeElement);
    });
  }

  toggleSound(item: any, event: Event) {
    event.stopPropagation();
    item.isMuted = !item.isMuted;
  }

  startPolling() {
    // 재진입 시 중복 인터벌 방지
    this.stopPolling();

    this.http.post('/api/super/lobby/poll', { lastCheckTime: '', starId: this.starId }).subscribe((res: any) => {
      if (res.result === 'OK') this.lastCheckTime = res.currentTime;
    });

    this.pollingIntervalId = setInterval(() => {
      if (!this.lastCheckTime) return;

      this.http.post('/api/super/lobby/poll', {
        lastCheckTime: this.lastCheckTime,
        starId: this.starId
      }).subscribe((res: any) => {
        if (res.result === 'OK') {
          this.lastCheckTime = res.currentTime;

          const myStarData = res.data?.find((item: any) => item.PRS_ID === this.starId);

          if (myStarData && myStarData.NEW_VIEWS > 0) {
            this.distributePolledViews(myStarData.NEW_VIEWS);
          }
        }
      });
    }, 30000);
  }

  stopPolling() {
    if (this.pollingStartTimeoutId) {
      clearTimeout(this.pollingStartTimeoutId);
      this.pollingStartTimeoutId = null;
    }
    if (this.pollingIntervalId) {
      clearInterval(this.pollingIntervalId);
      this.pollingIntervalId = null;
    }
  }

  distributePolledViews(newViews: number) {
    for (let i = 0; i < newViews; i++) {
      const randomDelay = Math.floor(Math.random() * 29000);

      setTimeout(() => {
        this.viewCount++;

        this.showPlus = false;
        setTimeout(() => this.showPlus = true, 50);
        setTimeout(() => this.showPlus = false, 800);

        const audio = new Audio('assets/sounds/tick.mp3');
        audio.volume = 0.65;
        audio.play().catch(e => console.log('Audio playback error:', e));

        try {
          import('@capacitor/haptics').then(({ Haptics, ImpactStyle }) => {
            Haptics.impact({ style: ImpactStyle.Light });
          });
        } catch (e) { }

        this.runSlotMachineEffect(this.viewCount);

      }, randomDelay);
    }
  }

  runSlotMachineEffect(targetNumber: number) {
    let ticks = 0;
    const maxTicks = 15;

    const interval = setInterval(() => {
      ticks++;
      if (ticks >= maxTicks) {
        clearInterval(interval);
        this.displayViewCount = targetNumber;
      } else {
        const randomOffset = Math.floor(Math.random() * 30) - 15;
        this.displayViewCount = Math.max(0, targetNumber + randomOffset);
      }
    }, 30);
  }

  // 🌟 [수정] 최종 feedList 기준으로 광고 빈칸 삽입
  // 모든 콘텐츠(스타 포스트 + 관리자 공지)가 feedList에 확정된 후 호출됩니다.
  insertAdSlots() {
    // 1. 기존 광고 슬롯이 있으면 먼저 제거 (새로고침 시 중복 방지)
    this.feedList = this.feedList.filter((item: any) => !item.isAd);

    // 2. 최종 리스트 길이 기준으로 광고 슬롯 삽입 (큰 인덱스부터 삽입해야 앞쪽 인덱스가 밀리지 않음)
    if (this.feedList.length >= 16) this.feedList.splice(16, 0, { isAd: true, adId: 3 });
    if (this.feedList.length >= 9) this.feedList.splice(9, 0, { isAd: true, adId: 2 });
    if (this.feedList.length >= 3) this.feedList.splice(3, 0, { isAd: true, adId: 1 });
  }

  formatNumber(num: number): string {
    if (!num) return '0';

    if (num >= 1000000) {
      return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K';
    } else {
      return num.toString();
    }
  }

  async deleteAdminFeed(item: any, event: Event) {
    event.stopPropagation();

    const alert = await this.alertCtrl.create({
      header: 'Delete Post',
      message: 'Are you sure you want to delete this post?',
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Delete',
          handler: () => {
            const payload = {
              adminId: this.currentAdminId,
              adminPw: localStorage.getItem('adminPw'),
              conId: item.CON_ID
            };

            this.http.post('/api/super/admin/feed/delete', payload).subscribe((res: any) => {
              if (res.result === 'OK') {
                this.loadStarDetail();
              } else {
                this.showError(res.msg || 'Failed to delete.');
              }
            });
          }
        }
      ]
    });
    await alert.present();
  }

  async deleteFeed(item: any, event: Event) {
    event.stopPropagation();

    // 1. 관리자 글일 경우 기존 로직 그대로 태우기
    if (item.FEED_TYPE === 'ADMIN') {
      this.deleteAdminFeed(item, event);
      return;
    }

    // 2. 스타 본인 글일 경우
    const alert = await this.alertCtrl.create({
      header: 'Delete Post',
      message: 'Are you sure you want to delete this post? This cannot be undone.',
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Delete',
          handler: () => {
            const payload = {
              starId: this.starId,
              starToken: localStorage.getItem('starToken'),
              conId: item.CON_ID
            };

            this.http.post('/api/super/star/feed/delete', payload).subscribe((res: any) => {
              if (res.result === 'OK') {
                this.loadStarDetail(); // 삭제 성공 시 화면 새로고침
              } else {
                this.showError(res.msg || 'Failed to delete.');
              }
            });
          }
        }
      ]
    });
    await alert.present();
  }

  async openPinLink(url: string, event: Event) {
    event.stopPropagation();
    await this.deepLink.openExternal(url || 'https://google.com');
  }

  async showError(msg: string) {
    const alert = await this.alertCtrl.create({ header: 'Notice', message: msg, buttons: ['OK'] });
    await alert.present();
  }

  async claimPage() {
    const alert = await this.alertCtrl.create({
      header: '👑 Claim This Page',
      // 🌟 메시지도 비밀번호를 입력하라는 내용으로 살짝 수정했습니다.
      message: 'Would you like to manage this page? Enter your email and a new password to receive an admin access link.',
      inputs: [
        { name: 'email', type: 'email', placeholder: 'your@email.com' },
        // 🌟 비밀번호 입력 칸 추가
        { name: 'password', type: 'password', placeholder: 'New Password' }
      ],
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Send Magic Link',
          handler: (data) => {
            // 이메일이나 비밀번호가 비어있으면 진행 불가
            if (!data.email || data.email.trim() === '') return false;
            if (!data.password || data.password.trim() === '') {
              // 필요하다면 여기서 this.showError('Please enter a password.') 등을 띄워줄 수도 있습니다.
              return false;
            }

            this.http.post('/api/super/page/claim', {
              pageId: this.starId,
              email: data.email,
              password: data.password // 🌟 백엔드로 비밀번호 함께 전송!
            }).subscribe((res: any) => {
              if (res.result === 'OK') {
                // 서버에서 발급한 토큰을 로컬에 저장 (나중에 새로고침 시 대조용)
                if (res.token) {
                  localStorage.setItem('pendingClaimToken', res.token);
                }
                this.showError('Magic link sent! After clicking the link in your email, please pull down this page to refresh.');
              } else {
                this.showError(res.msg || 'Failed to send the link.');
              }
            });
          }
        }
      ]
    });
    await alert.present();
  }

  async doRefresh(event: any) {
    const pendingToken = localStorage.getItem('pendingClaimToken');
    console.log(pendingToken)

    if (pendingToken) {
      // 대기 중인 토큰이 있다면 서버에 인증 완료 여부 확인
      this.http.get(`/api/super/page/claim/status?token=${pendingToken}`).subscribe({
        next: (res: any) => {
          if (res.result === 'OK') {
            // 🎉 인증 성공! 자동 로그인 처리
            this.processMagicLogin(res.starId, res.email);
            // localStorage.removeItem('pendingClaimToken'); // 토큰 사용 완료로 삭제
          } else {
            // 아직 인증 전이라면 일반 데이터만 새로고침
            this.loadStarDetail();
          }
          event.target.complete();
        },
        error: () => {
          this.loadStarDetail();
          event.target.complete();
        }
      });
    } else {
      // 대기 토큰이 없으면 그냥 데이터 새로고침
      this.loadStarDetail();
      event.target.complete();
    }
  }

  // 🌟 [신규] 2. 매직 링크 기반 자동 로그인 처리
  processMagicLogin(starId: string, email: string) {
    // 로컬 스토리지에 로그인 정보 기록
    localStorage.setItem('isStar', 'true');
    localStorage.setItem('starId', starId);
    localStorage.setItem('ownerEmail', email);

    this.isStar = true; // UI 즉시 반영 (Edit 버튼 노출 등)
    this.isClaimed = true; // 소유권 상태 갱신

    this.showError('Congratulations! You are now the owner of this page.');

    // 소유자가 되었으므로 데이터를 다시 로드하여 관리자 메뉴 등을 활성화
    this.loadStarDetail();
  }

  // 🌟 [신규] 2. 하단 트래픽 순환을 위한 추천 페이지 로드 & 이동
  loadRecommendedPages() {
    this.http.post('/api/super/page/discover', {
      excludePageId: this.starId
    }).subscribe((res: any) => {
      if (res.result === 'OK' && res.list) {
        this.recommendedPages = res.list;
      }
    });
  }

  async goNextPage() {
    if (this.recommendedPages.length > 0) {
      try { await Haptics.impact({ style: ImpactStyle.Light }); } catch (e) { }

      const nextStarId = this.recommendedPages[0].PRS_ID;

      // reload()를 과감히 지우고, 부드럽게 라우팅만 수행!
      this.router.navigate(['/star', nextStarId]);
    }
  }

  // 🌟 [신규] 1. 닉네임 수정 로직
  async editName() {
    const alert = await this.alertCtrl.create({
      header: 'Edit Display Name',
      message: 'Please enter your new display name.', // 변경하실 닉네임을 입력해주세요.
      inputs: [
        {
          name: 'newName',
          type: 'text',
          value: this.starInfo?.name,
          placeholder: 'New Display Name'
        }
      ],
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Save',
          handler: (data) => {
            if (data.newName && data.newName.trim() !== '') {
              this.updateProfile({ name: data.newName.trim() });
            }
          }
        }
      ]
    });
    await alert.present();
  }

  // 소개문 탭: 소유자는 편집, 방문자는 전문 보기 (헤더에는 1줄 말줄임만 보인다 — 2-28차)
  onBioClick() {
    if (this.isStar) {
      this.editBio();
    } else {
      this.showFullBio();
    }
  }

  // 🌟 [신규 2-28차] 방문자용 소개문 전문 보기
  async showFullBio() {
    const bio = this.starInfo?.PRS_BIO;
    if (!bio) return;

    const alert = await this.alertCtrl.create({
      header: this.starInfo?.name || 'About',
      message: bio,
      buttons: [{ text: 'Close', role: 'cancel' }]
    });
    await alert.present();
  }

  // 🌟 [신규 2-27차] 소개문 수정 로직 — 어드민 입력(웹 랜딩 About)과 같은 컬럼(PRS_BIO)을 쓴다
  async editBio() {
    const alert = await this.alertCtrl.create({
      header: 'Edit Bio',
      message: 'Introduce yourself to your fans.',
      inputs: [
        {
          name: 'newBio',
          type: 'textarea',
          value: this.starInfo?.PRS_BIO || '',
          placeholder: 'Write a short introduction',
          attributes: { maxlength: 2000, rows: 4 }
        }
      ],
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Save',
          handler: (data) => {
            // 빈 값 저장 = 소개문 비우기 (서버가 trim 후 그대로 반영)
            this.updateProfile({ bio: (data.newBio || '').trim() });
          }
        }
      ]
    });
    await alert.present();
  }

  // 🌟 [신규] 2. 프로필 사진 선택 및 확인 로직
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (!file) return;

    // 사진을 Base64로 변환
    const reader = new FileReader();
    reader.onload = async (e: any) => {
      const base64Data = e.target.result;

      const alert = await this.alertCtrl.create({
        header: 'Change Profile Picture',
        message: 'Do you want to change your profile picture?', // 변경하시겠습니까?
        buttons: [
          {
            text: 'No',
            role: 'cancel',
            handler: () => { event.target.value = ''; } // 취소 시 인풋 초기화
          },
          {
            text: 'Yes',
            handler: () => {
              // 닉네임은 기존 값 그대로 유지하고 사진만 전송
              this.updateProfile({ name: this.starInfo?.name, imageBase64: base64Data });
            }
          }
        ]
      });
      await alert.present();
    };
    reader.readAsDataURL(file);
  }

  // 🌟 [신규] 3. 공통 프로필 업데이트 API 호출 로직
  updateProfile(data: { name?: string, imageBase64?: string, bio?: string }) {
    // 보낸 항목만 서버가 갱신한다. bio만 고칠 때 이름이 지워지지 않도록 undefined는 싣지 않는다(2-27차)
    const payload: any = {
      starId: this.starId,
      starToken: localStorage.getItem('starToken')
    };
    if (data.name !== undefined) payload.name = data.name;
    if (data.imageBase64 !== undefined) payload.imageBase64 = data.imageBase64;
    if (data.bio !== undefined) payload.bio = data.bio;

    // 로딩 인디케이터나 버튼 비활성화 처리를 해주면 더 좋습니다.
    this.http.post('/api/super/star/profile/update', payload).subscribe((res: any) => {
      if (res.result === 'OK') {
        this.showError('Profile updated successfully!');
        this.loadStarDetail(); // 성공 시 화면 새로고침하여 변경된 데이터 반영
      } else {
        this.showError(res.msg || 'Failed to update profile.');
      }
    });
  }

  // 🎬 YouTube URL에서 썸네일 이미지 추출
  getYoutubeThumbnail(youtubeUrl: string): string {
    if (!youtubeUrl) return '';

    const videoId = this.extractYoutubeVideoId(youtubeUrl);

    if (videoId) {
      // YouTube 공식 썸네일 (hqdefault.jpg - 가장 안정적)
      const thumbnailUrl = `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
      return thumbnailUrl;
    }
    return '';
  }

  // 🎬 YouTube VIDEO_ID 추출 (다양한 형식 지원)
  extractYoutubeVideoId(youtubeUrl: string): string {
    if (!youtubeUrl) return '';

    const patterns = [
      /(?:youtube\.com\/(?:watch\?v=|embed\/|v\/|shorts\/)|youtu\.be\/)([\w\-]{11})/,
      /youtube\.com\/watch\?v=([^&]+)/,
      /m\.youtube\.com\/watch\?v=([^&]+)/,
      /youtu\.be\/([^?&]+)/,
    ];

    for (const pattern of patterns) {
      const match = youtubeUrl.match(pattern);
      if (match && match[1]) {
        return match[1];
      }
    }

    return '';
  }

  // 🌟 [신규] 프로필 사진 확대 뷰어 열기
  openProfileViewer() {
    this.isProfileViewerOpen = true;
  }

  // 🌟 [신규] My Daily Insight 모달 열기
  async openMyInsight() {
    const modal = await this.modalCtrl.create({
      component: MyInsightModalComponent,
      componentProps: {
        starId: this.starId
      }
    });
    await modal.present();
  }

  // 🌟 [신규] 프로필 사진 확대 뷰어 닫기
  closeProfileViewer() {
    this.isProfileViewerOpen = false;
  }

  handleWriteButtonClick() {
    this.writeModalService.openWriteModal(
      this.isStar,
      this.starId,
      localStorage.getItem('adminLevel') || '',
      () => {
        this.loadStarDetail(); // Reload page feeds on success
      }
    );
  }

  goBack() {
    this.navCtrl.navigateBack('/lobby');
  }
}