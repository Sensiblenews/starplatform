import { AfterViewInit, Component, ElementRef, NgZone, OnDestroy, OnInit, QueryList, ViewChildren } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpService } from '../../services/http.service';
import { Platform, PopoverController, AlertController, ModalController } from '@ionic/angular';
import NativeBridge from 'src/app/plugins/native-bridge';
import { AdMobService } from 'src/app/services/ad-mob.service';
import { AdProtectionService } from 'src/app/services/ad-protection.service';
import { StarMenuComponent } from './star-menu.component';
import { Share } from '@capacitor/share';
import { Haptics, ImpactStyle } from '@capacitor/haptics';
import { CommentModalComponent } from './modals/comment-modal.component';
import { MyInsightModalComponent } from './modals/my-insight-modal.component';
import { Device } from '@capacitor/device';

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
  private observer: IntersectionObserver;

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
  private lastCheckTime: string = '';

  isAdmin: boolean = false;
  currentAdminId: string = '';

  // 🌟 [신규] 온보딩 및 추천 페이지 관련 변수
  isClaimed: boolean = true; // 기본값은 주인이 있는 것으로 설정 (API에서 받아옴)
  recommendedPages: any[] = []; // 하단 무한 스크롤(Next Page)용 추천 리스트

  isStar: boolean = false; // 🌟 추가: 현재 사용자가 이 페이지의 주인인지 여부

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
  ) { }

  async ngOnInit() {
    const info = await Device.getId();
    this.deviceId = info.identifier;
    this.starId = this.route.snapshot.paramMap.get('starId');

    this.isStar = localStorage.getItem('isStar') === 'true' && localStorage.getItem('starId') === this.starId;
    this.isAdmin = localStorage.getItem('isAdmin') === 'true';
    this.currentAdminId = localStorage.getItem('adminId') || '';

    this.loadStarDetail();
    this.checkFavoriteState();
  }

  checkFavoriteState() {
    const favList = JSON.parse(localStorage.getItem('favorite_stars') || '[]');
    this.isFavorite = favList.includes(this.starId);
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

    this.http.post('/api/super/star/toggleFollow', { starId: this.starId, isAdd: this.isFavorite, deviceId: this.deviceId }).subscribe();
  }

  // 🌟 [신규] 포인트 계산 (조회수 / 10 의 내림)
  getPoints(views: number): number {
    return Math.floor((views || 0) / 10);
  }

  ngAfterViewInit(): void {
    this.videoElements.changes.subscribe(() => {
      this.initIntersectionObserver();
    });
  }

  ngOnDestroy(): void {
    this.removeClickListener();
  }

  async ionViewDidEnter() {
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

    // if (this.platform.is('capacitor')) {
    if (true) { // 개발용
      let canShow = await this.adProtection.shouldShowAd(this.starId);
      console.log("Ad Protection Check for Star ID:", this.starId, "Can Show Ad:", canShow);
      canShow = true // 개발용

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

  loadStarDetail() {
    this.http.post(`/api/super/star/${this.starId}`, { deviceId: this.deviceId }).subscribe((res: any) => {
      if (res.result === 'OK') {
        this.starInfo = res.starInfo;
        this.followerCount = this.starInfo.FOLLOWER_CNT || 0;

        // 🌟 [신규] 백엔드에서 받은 랭킹/전체 수 세팅
        this.globalRank = this.starInfo.GLOBAL_RANK || 0;
        // loadStarDetail() 내부 (res.result === 'OK' 안쪽)

        this.totalStars = this.starInfo.TOTAL_STARS || 0;

        this.viewCount = this.starInfo.viewCount || 0;
        this.displayViewCount = this.viewCount;

        this.starAction = {
          hasLiked: this.starInfo.IS_LIKED == 1 || this.starInfo.IS_LIKED === true,
          likeCount: this.starInfo.LIKE_CNT || 0,
          commentCount: this.starInfo.COMMENT_CNT || 0,
          shareCount: this.starInfo.SHARE_CNT || 0
        };

        const photos = res.starInfo.photos || [];

        photos.forEach((item: any) => {
          if (item.MEDIA_TYPE === 'VIDEO') item.isMuted = true;
          item.isLoaded = false;

          item.hasLiked = item.IS_LIKED == 1 || item.IS_LIKED === true;
          item.likeCount = item.LIKE_CNT || 0;
          item.commentCount = item.COMMENT_CNT || 0;
        });

        // 🌟 [수정] 모든 콘텐츠(스타 포스트 + 관리자 공지 등)를 먼저 feedList에 확정한 뒤,
        // 최종 배열 기준으로 광고 빈칸을 삽입합니다.
        this.feedList = photos;
        this.insertAdSlots();
        // 🌟 [신규] OWNER_EMAIL 값이 비어있으면(null) 주인이 없는 페이지!
        this.isClaimed = this.starInfo.OWNER_EMAIL ? true : false;
        // 🌟 [신규] 페이지 디테일 로딩 시 하단 추천 페이지도 같이 불러옵니다.
        this.loadRecommendedPages();
      }
    });
  }

  toggleLike(target: any, type: 'star' | 'feed', event?: Event) {
    if (event) event.stopPropagation();

    const targetId = type === 'star' ? this.starId : target.CON_ID;
    const payload = {
      targetType: type.toUpperCase(),
      targetId: targetId,
      deviceId: this.deviceId
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

    const baseUrl = 'https://witch-hunting.com/witch';
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
      MEM_ID: this.deviceId || 'GUEST',
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
    if (this.pollingIntervalId) {
      clearInterval(this.pollingIntervalId);
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
  updateProfile(data: { name?: string, imageBase64?: string }) {
    const payload = {
      starId: this.starId,
      starToken: localStorage.getItem('starToken'),
      name: data.name,
      imageBase64: data.imageBase64
    };

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
}