import { Component, ElementRef, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { HttpService } from '../../services/http.service';
import { Platform, ModalController, PopoverController, AlertController, ActionSheetController } from '@ionic/angular';
import { Subject, Subscription, forkJoin, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { MarketMenuPopoverComponent } from './market-menu-popover.component';
import { BoardModalComponent } from './modals/board-modal.component';
import { Haptics, ImpactStyle } from '@capacitor/haptics';
import { WriteModalService } from '../../services/write-modal.service';
import { ProfileMenuPopoverComponent } from './profile-menu-popover.component';
import { MessageModalComponent } from './modals/message-modal.component';
import { AvailablePageModalComponent } from './modals/available-page-modal.component';
import { FirebaseAuthService } from 'src/app/services/oauth/firebase-auth.service';
import { PushNotifications } from '@capacitor/push-notifications';
import { GeneralRankingModalComponent } from './modals/rankings/general-ranking-modal.component';
import { RevenueRankingModalComponent } from './modals/rankings/revenue-ranking-modal.component';
import { DailyRankingModalComponent } from './modals/rankings/daily-ranking-modal.component';
import { HallOfFameModalComponent } from './modals/rankings/hall-of-fame-modal.component';
import { Device } from '@capacitor/device';

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.page.html',
  styleUrls: ['./lobby.page.scss'],
})
export class LobbyPage implements OnInit, OnDestroy {
  public defaultAvatar = 'assets/img/defaultImg/avatar.svg';

  isAdmin: boolean = false;
  adminLevel: string = '';
  isStar: boolean = false;
  starId: string = '';

  private titleTapCount = 0;
  private tapTimeout: any;

  popularStars: any[] = [];
  allStars: any[] = [];
  allStarsOriginal: any[] = [];
  newCreators: any[] = [];
  newCreatorsOriginal: any[] = [];

  searchResults: any[] = [];
  contentResults: any[] = [];
  isSearching: boolean = false;
  private searchInput$ = new Subject<string>();
  private searchSub: Subscription;

  isShowingFavorites: boolean = false;
  favoriteStars: any[] = [];

  isShowingRanking = false;
  showHallOfFameInline = false;
  rankingMode: 'general' | 'revenue' | 'daily' = 'general';
  revenueRankingStars: any[] = [];
  dailyRankingStars: any[] = [];
  hallOfFameStars: any[] = [];

  // 🌟 [신규] 누적 수익 데이터
  myRevenue: { totalVisits: number; revenueUsd: number; revenueKrw: number } | null = null;
  isLoadingRevenue = false;

  // 🌟 [신규] 하단 Discover Global Pages 추천 리스트
  recommendedPages: any[] = [];

  private readonly rankingEndpoints = {
    general: '/api/super/leaderboard',
    revenue: '/api/super/leaderboard/revenue',
    daily: '/api/super/leaderboard/daily'
  } as const;

  deviceId: string = '';

  private backButtonSub: Subscription;
  private pollingIntervalId: any;
  private lastCheckTime: string = '';
  private observer: IntersectionObserver;

  // 🌟 [신규] 상단 슬롯머신(슬라이드) 제어 변수
  @ViewChild('topScroll') topScroll: ElementRef;
  slideOpts = {
    slidesPerView: 3.5,
    spaceBetween: 10,
    freeMode: true,
    loop: true // 무한 롤링 허용
  };

  // 🌟 [신규] 타이머 변수
  private topSlideInterval: any;
  private shuffleInterval: any;

  rankingStars: any[] = [];

  constructor(
    private router: Router,
    private http: HttpService,
    private platform: Platform,
    private popoverCtrl: PopoverController,
    private modalCtrl: ModalController,
    private alertCtrl: AlertController,
    private actionSheetCtrl: ActionSheetController, // 🌟 추가
    private firebaseAuth: FirebaseAuthService,
    private writeModalService: WriteModalService,
    // private globalFeedback: GlobalFeedbackService,
  ) { }

  async ngOnInit() {
    const info = await Device.getId();
    this.deviceId = info.identifier;

    this.loadLobbyData();
    this.startPolling();
    this.searchSub = this.searchInput$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((rawQuery) => {
        const query = (rawQuery || '').toLowerCase().trim();

        if (!query) {
          this.isSearching = false;
          this.searchResults = [];
          this.contentResults = [];
          this.allStars = this.allStarsOriginal.slice(0, 32);
          this.setupMotionObserver();
          return of(null);
        }

        this.isSearching = true;
        this.isShowingFavorites = false;
        this.isShowingRanking = false;

        const starSearch$ = this.http.post('/api/super/star/search', {
          query,
          page: 1,
          pageSize: 30,
          includeStats: true
        }).pipe(
          catchError(() => of({ result: 'FAIL', list: [] }))
        );

        const contentSearch$ = this.http.post('/api/super/content/search', {
          query,
          page: 1,
          pageSize: 30
        }).pipe(
          catchError(() => of({ result: 'FAIL', list: [] }))
        );

        return forkJoin({ stars: starSearch$, contents: contentSearch$ });
      })
    ).subscribe((res: any) => {
      if (!res) return;

      if (res.stars?.result === 'OK') {
        this.searchResults = (res.stars.list || []).map(this.initStarData);
      } else {
        this.searchResults = [];
      }

      if (res.contents?.result === 'OK') {
        this.contentResults = res.contents.list || [];
      } else {
        this.contentResults = [];
      }

      this.setupMotionObserver();
    });

    this.isAdmin = localStorage.getItem('isAdmin') === 'true';
    this.adminLevel = localStorage.getItem('adminLevel') || '';
    this.isStar = localStorage.getItem('isStar') === 'true';
    this.starId = localStorage.getItem('starId') || '';
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.stopAutoSlide();
    this.stopAutoShuffle();
    if (this.searchSub) this.searchSub.unsubscribe();
    if (this.observer) this.observer.disconnect();
  }

  ionViewWillEnter() {
    // 딥링크 로그인 후 로비로 돌아왔을 때, 최신 스토리지를 다시 읽어옵니다.
    this.isAdmin = localStorage.getItem('isAdmin') === 'true';
    this.adminLevel = localStorage.getItem('adminLevel') || '';
    this.isStar = localStorage.getItem('isStar') === 'true';
    this.starId = localStorage.getItem('starId') || '';

    if (this.isShowingFavorites) {
      this.loadFavoriteStars();
    }
  }

  ionViewDidEnter() {
    // 🌟 화면에 보일 때만 타이머 가동 (성능 최적화)
    this.startAutoSlide();
    this.startAutoShuffle();

    this.backButtonSub = this.platform.backButton.subscribeWithPriority(10, (processNextHandler) => {
      if (this.isShowingFavorites) {
        this.isShowingFavorites = false;
      } else if (this.isShowingRanking) {
        this.isShowingRanking = false;
      } else if (this.isSearching) {
        this.isSearching = false;
        this.searchResults = [];
        this.contentResults = [];
      } else {
        processNextHandler();
      }
    });
  }

  ionViewWillLeave() {
    // 🌟 화면에서 나가면 타이머 정지
    this.stopAutoSlide();
    this.stopAutoShuffle();

    if (this.backButtonSub) {
      this.backButtonSub.unsubscribe();
    }
  }

  // 🌟 [신규] FCM 토큰 발급 및 서버 전송
  async registerFCMToken(targetStarId: string) {
    console.log("registerFCMToken called for starId:", targetStarId);
    if (this.platform.is('capacitor')) {
      let permStatus = await PushNotifications.checkPermissions();

      // 처음 묻는 거라면 권한 팝업 띄우기
      if (permStatus.receive === 'prompt') {
        permStatus = await PushNotifications.requestPermissions();
      }

      // 유저가 '허용'을 누르면 기기 등록 실행
      if (permStatus.receive === 'granted') {
        await PushNotifications.register();

        // 🔴 [Critical Fix] registration 이벤트 미발생 대비:
        // 이미 캐싱된 토큰이 있으면 직접 서버에 전송 (1초 대기 후 체크)
        setTimeout(() => {
          const cachedToken = localStorage.getItem('fcmToken');
          if (cachedToken && targetStarId) {
            console.log('🌟 Sending cached FCM token to server for starId:', targetStarId);
            this.http.post('/api/super/star/push/token', {
              starId: targetStarId,
              fcmToken: cachedToken
            }).subscribe({
              next: () => console.log('✅ FCM token sent to server successfully'),
              error: (err: any) => console.error('❌ FCM token send failed:', err)
            });
          }
        }, 1500);
      }
    }
  }

  // 🌟 [신규] 4초마다 상단 6명 카드 스와이프
  startAutoSlide() {
    this.stopAutoSlide();
    this.topSlideInterval = setInterval(() => {
      if (this.topScroll && this.topScroll.nativeElement) {
        const el = this.topScroll.nativeElement;

        // 스크롤이 맨 오른쪽 끝에 도달했는지 확인
        // (scrollWidth: 전체 가로 길이, clientWidth: 현재 화면에 보이는 길이, scrollLeft: 현재 스크롤 위치)
        if (el.scrollLeft + el.clientWidth >= el.scrollWidth - 10) {
          // 끝에 도달하면 맨 처음으로 휙! 되돌아가기
          el.scrollTo({ left: 0, behavior: 'smooth' });
        } else {
          // 아직 끝이 아니면 오른쪽으로 카드 한 칸(약 120px) 이동
          el.scrollBy({ left: 120, behavior: 'smooth' });
        }
      }
    }, 4000);
  }

  stopAutoSlide() {
    if (this.topSlideInterval) clearInterval(this.topSlideInterval);
  }

  // 🌟 [신규] 10초마다 하단 16명 리스트 섞기
  startAutoShuffle() {
    this.stopAutoShuffle();
    this.shuffleInterval = setInterval(() => {
      // 검색 중이거나 즐겨찾기 화면일 때는 안 섞임
      if (!this.isSearching && !this.isShowingFavorites && (this.allStarsOriginal.length > 0 || this.newCreatorsOriginal.length > 0)) {
        this.shuffleStars();
      }
    }, 10000);
  }

  stopAutoShuffle() {
    if (this.shuffleInterval) clearInterval(this.shuffleInterval);
  }

  // 🌟 [신규] 리스트 셔플 알고리즘
  shuffleStars() {
    // 전체 데이터를 가중치(랜덤) 기반으로 섞기
    const shuffled = [...this.allStarsOriginal].sort(() => Math.random() - 0.5);

    // 배열을 비우지 않고 바로 교체 (스크롤 위치 유지)
    this.allStars = shuffled.slice(0, 32);

    const shuffledNew = [...this.newCreatorsOriginal].sort(() => Math.random() - 0.5);
    this.newCreators = shuffledNew.slice(0, 32);

    this.setupMotionObserver(); // 등장 애니메이션 재부착
  }

  setupMotionObserver() {
    if (this.observer) this.observer.disconnect();

    this.observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('animate');
        } else {
          entry.target.classList.remove('animate');
        }
      });
    }, { threshold: 0.2 });

    setTimeout(() => {
      const cards = document.querySelectorAll('.motion-card');
      cards.forEach(card => this.observer.observe(card));
    }, 100);
  }

  loadLobbyData(event?: any) {
    this.http.post('/api/super/lobby', {}).subscribe(
      (res: any) => {
        if (res.result === 'OK') {
          this.popularStars = (res.popularStars || []).map(this.initStarData);
          this.allStarsOriginal = (res.allStars || []).map(this.initStarData);
          this.newCreatorsOriginal = (res.newCreators || []).map(this.initStarData);

          // 🌟 로딩 완료 즉시 16명 셔플해서 초기화
          this.shuffleStars();

          if (this.isShowingFavorites) {
            this.loadFavoriteStars();
          }
        }
        if (event) event.target.complete();
      },
      (error) => { if (event) event.target.complete(); }
    );

    setTimeout(() => {
      this.loadLeaderboard();
    }, 100);

    // 🌟 Discover Global Pages 데이터 로드
    this.loadRecommendedPages();
  }

  loadLeaderboard() {
    // 백엔드 API 호출 (Top 100)
    this.http.post(this.rankingEndpoints.general, {}).subscribe((res: any) => {
      if (res.result === 'OK') {
        this.rankingStars = (res.list || []).map(this.initStarData);
      }
    });

    this.http.post(this.rankingEndpoints.revenue, {}).subscribe((res: any) => {
      if (res.result === 'OK') {
        this.revenueRankingStars = (res.list || []).map(this.initStarData);
      }
    });

    this.http.post(this.rankingEndpoints.daily, {}).subscribe((res: any) => {
      if (res.result === 'OK') {
        this.dailyRankingStars = (res.list || []).map(this.initStarData);
      }
    });

    this.hallOfFameStars = [];
  }

  private initStarData = (star: any) => {
    const favIds = JSON.parse(localStorage.getItem('favorite_stars') || '[]');
    const isFav = favIds.includes(String(star.id));

    return {
      ...star,
      viewCount: star.viewCount || 0,
      displayViewCount: star.viewCount || 0,
      showPlus: false,
      isFavorite: isFav,
      followerCount: star.FOLLOWER_CNT || star.followerCount || 0,
      globalRank: star.GLOBAL_RANK || star.globalRank || 0,
      totalStars: star.TOTAL_STARS || 0
    };
  };

  getPoints(views: number): number {
    return Math.floor((views || 0) / 10);
  }

  doRefresh(event: any) {
    this.loadLobbyData(event);
  }

  toggleFavoriteList() {
    this.isShowingFavorites = !this.isShowingFavorites;
    if (this.isShowingFavorites) {
      this.isSearching = false;
      this.isShowingRanking = false;
      this.loadFavoriteStars();
    }
  }

  toggleRankingList() {
    this.isShowingRanking = !this.isShowingRanking;
    if (this.isShowingRanking) {
      this.isSearching = false;
      this.isShowingFavorites = false; // 즐겨찾기 끄기
      this.rankingMode = 'general';
      this.loadMyRevenue();
    } else {
      this.showHallOfFameInline = false;
    }
  }

  // 🌟 [신규] 누적 수익 API 호출
  loadMyRevenue() {
    if (!this.isStar || !this.starId) {
      this.myRevenue = null;
      return;
    }

    this.isLoadingRevenue = true;
    const starToken = localStorage.getItem('starToken') || '';

    this.http.post('/api/super/ranking/my-revenue', {
      starId: this.starId,
      starToken: starToken
    }).subscribe({
      next: (res: any) => {
        this.isLoadingRevenue = false;
        if (res.result === 'OK') {
          const visits = res.totalVisits || 0;
          this.myRevenue = {
            totalVisits: visits,
            revenueUsd: visits * 0.0000667,
            revenueKrw: Math.floor(visits * 0.1)
          };
        }
      },
      error: () => {
        this.isLoadingRevenue = false;
      }
    });
  }

  // 🌟 [신규] USD 포맷 (소수점 2자리)
  formatUsd(value: number): string {
    return '$' + value.toFixed(2);
  }

  // 🌟 [신규] KRW 포맷 (소수점 버림, 천 단위 콤마)
  formatKrw(value: number): string {
    return Math.floor(value).toLocaleString('ko-KR') + ' KRW';
  }

  setRankingMode(mode: 'general' | 'revenue' | 'daily') {
    this.rankingMode = mode;
  }

  async openGeneralRankingModal() {
    this.showHallOfFameInline = false;
    const modal = await this.modalCtrl.create({
      component: GeneralRankingModalComponent,
      componentProps: {
        stars: this.rankingStars
      }
    });
    await modal.present();
  }

  async openRevenueRankingModal() {
    this.showHallOfFameInline = false;
    const modal = await this.modalCtrl.create({
      component: RevenueRankingModalComponent,
      componentProps: {
        stars: this.revenueRankingStars
      }
    });
    await modal.present();
  }

  async openDailyRankingModal() {
    this.showHallOfFameInline = false;
    const modal = await this.modalCtrl.create({
      component: DailyRankingModalComponent,
      componentProps: {
        stars: this.dailyRankingStars
      }
    });
    await modal.present();
  }

  openHallOfFameModal() {
    this.showHallOfFameInline = !this.showHallOfFameInline;
  }

  loadFavoriteStars() {
    const favIds = JSON.parse(localStorage.getItem('favorite_stars') || '[]');

    if (!favIds.length) {
      this.favoriteStars = [];
      this.setupMotionObserver();
      return;
    }

    this.http.post('/api/super/star/favorites', {
      deviceId: this.deviceId,
      starIds: favIds
    }).subscribe({
      next: (res: any) => {
        if (res.result === 'OK' && res.list) {
          this.favoriteStars = res.list.map(this.initStarData);
        } else {
          // API 실패 시 기존 로컬 필터링 폴백
          this.favoriteStars = this.allStarsOriginal.filter(star =>
            favIds.includes(String(star.id))
          );
        }
        this.setupMotionObserver();
      },
      error: () => {
        // 네트워크 에러 시 기존 로컬 필터링 폴백
        this.favoriteStars = this.allStarsOriginal.filter(star =>
          favIds.includes(String(star.id))
        );
        this.setupMotionObserver();
      }
    });
  }

  filterStars(event: any) {
    this.searchInput$.next(event?.detail?.value ?? '');
  }

  getStarImage(imageUrl: string | null): string {
    return (imageUrl && imageUrl.length > 0) ? imageUrl : this.defaultAvatar;
  }

  handleImageError(event: any) {
    event.target.src = this.defaultAvatar;
  }

  async goToStarPage(starId: string) {
    try { Haptics.impact({ style: ImpactStyle.Light }); } catch (e) { }

    if (event && event.currentTarget) {
      const targetCard = event.currentTarget as HTMLElement;
      targetCard.classList.add('zoom-active');
      setTimeout(() => {
        targetCard.classList.remove('zoom-active');
        this.router.navigate(['/star', starId]);
      }, 200);
    } else {
      this.router.navigate(['/star', starId]);
    }
  }

  async goToContentPage(contentId: string) {
    try { Haptics.impact({ style: ImpactStyle.Light }); } catch (e) { }
    this.router.navigate([`/feed-detail/${contentId}`]);
  }

  async openMarketMenu(ev: any) {
    const popover = await this.popoverCtrl.create({
      component: MarketMenuPopoverComponent,
      componentProps: { isLoggedIn: this.isAdmin || this.isStar },
      event: ev,
      alignment: 'end',
      side: 'bottom',
      translucent: true
    });

    await popover.present();
    const { data } = await popover.onDidDismiss();

    if (data && data.action) {
      if (data.action === 'ipo') this.openAvailablePageModal();
      else if (data.action === 'board') this.openBoardModal();
      else if (data.action === 'login_star') this.openCreatorLogin();
      else if (data.action === 'login_admin') this.showLogin('ADMIN');
    }
  }

  async openCreatorLogin() {
    const actionSheet = await this.actionSheetCtrl.create({
      header: 'Creator Login',
      buttons: [
        {
          text: 'Continue with Apple',
          icon: 'logo-apple',
          handler: () => { this.processSocialLogin('apple'); }
        },
        {
          text: 'Continue with Google',
          icon: 'logo-google',
          handler: () => { this.processSocialLogin('google'); }
        },
        // {
        //   text: 'Login with ID / PW',
        //   icon: 'mail-outline',
        //   handler: () => { this.showLogin('STAR'); } // 기존 ID/PW 알럿창
        // },
        {
          text: 'Cancel',
          role: 'cancel'
        }
      ]
    });
    await actionSheet.present();
  }

  // 3. 실제 소셜 인증 및 백엔드 로그인 처리
  async processSocialLogin(provider: string) {
    try {
      // 🌟 새로 만든 파이어베이스 서비스 호출
      const user = provider === 'google'
        ? await this.firebaseAuth.signInWithGoogle()
        : await this.firebaseAuth.signInWithApple();

      if (user && user.email) {
        // 백엔드의 소셜 로그인 엔드포인트 호출 (🌟 uid 파라미터 추가)
        this.http.post('/api/super/star/login/social', {
          email: user.email,
          uid: user.uid  // 🌟 [핵심] 백엔드에서 비밀번호처럼 검증할 값
        }).subscribe({
          next: (res: any) => {
            if (res.result === 'OK') {
              this.isStar = true;
              this.starId = res.starId;
              localStorage.setItem('isStar', 'true');
              localStorage.setItem('starId', res.starId);
              localStorage.setItem('starToken', res.starToken);
              // this.globalFeedback.startPolling();
              this.registerFCMToken(this.starId); // 로그인 성공한 스타의 ID로 FCM 토큰 등록
              this.showSimpleAlert('Login successful! Welcome back.');
            } else {
              this.showSimpleAlert(res.msg || 'Account not found. Please create a page first.');
            }
          }
        });
      }
    } catch (e) {
      console.error('Social login error', e);
    }
  }

  async openAvailablePageModal() {
    const modal = await this.modalCtrl.create({ component: AvailablePageModalComponent });
    await modal.present();

    const { data } = await modal.onDidDismiss();
    console.log(data);

    if (data && data.isSuccessful) {
      this.isAdmin = localStorage.getItem('isAdmin') === 'true';
      this.adminLevel = localStorage.getItem('adminLevel') || '';
      this.isStar = localStorage.getItem('isStar') === 'true';
      this.starId = localStorage.getItem('starId') || '';


      if (this.isShowingFavorites) {
        this.loadFavoriteStars();
      }
      // this.globalFeedback.startPolling();
      this.registerFCMToken(data.starId);
      this.router.navigate([`/star/${data.starId}`]);
      this.showSimpleAlert('Page claimed successfully. You are now logged in!');
    }
  }

  async openBoardModal() {
    const modal = await this.modalCtrl.create({ component: BoardModalComponent });
    await modal.present();
  }

  async triggerDopamine(star: any, event: Event) {
    event.stopPropagation();
    star.viewCount += 1;
    star.showPlus = true;
    setTimeout(() => star.showPlus = false, 800);

    try { await Haptics.impact({ style: ImpactStyle.Light }); } catch (e) { }

    const audio = new Audio('assets/sounds/tick.mp3');
    audio.volume = 0.65;
    audio.play().catch(e => console.log('Audio playback error:', e));

    this.runSlotMachineEffect(star);
  }

  formatNumber(num: number): string {
    if (!num) return '0';
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    else if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    else return num.toString();
  }

  startPolling() {
    this.http.post('/api/super/lobby/poll', { lastCheckTime: '' }).subscribe((res: any) => {
      if (res.result === 'OK') this.lastCheckTime = res.currentTime;
    });

    this.pollingIntervalId = setInterval(() => {
      if (!this.lastCheckTime) return;
      this.http.post('/api/super/lobby/poll', { lastCheckTime: this.lastCheckTime }).subscribe((res: any) => {
        if (res.result === 'OK') {
          this.lastCheckTime = res.currentTime;
          if (res.data && res.data.length > 0) this.distributePolledViews(res.data);
        }
      });
    }, 30000);
  }

  stopPolling() {
    if (this.pollingIntervalId) clearInterval(this.pollingIntervalId);
  }

  distributePolledViews(polledData: any[]) {
    polledData.forEach(item => {
      const star = this.allStarsOriginal.find(s => s.id === item.PRS_ID) || this.newCreatorsOriginal.find(s => s.id === item.PRS_ID);
      const newViews = item.NEW_VIEWS;

      if (star && newViews > 0) {
        for (let i = 0; i < newViews; i++) {
          const randomDelay = Math.floor(Math.random() * 29000);
          setTimeout(() => {
            star.viewCount++;
            star.showPlus = false;
            setTimeout(() => star.showPlus = true, 50);
            setTimeout(() => star.showPlus = false, 800);

            const audio = new Audio('assets/sounds/tick.mp3');
            audio.volume = 0.65;
            audio.play().catch(e => console.log('Audio playback error:', e));

            try {
              import('@capacitor/haptics').then(({ Haptics, ImpactStyle }) => {
                Haptics.impact({ style: ImpactStyle.Light });
              });
            } catch (e) { }

            this.runSlotMachineEffect(star);
          }, randomDelay);
        }
      }
    });
  }

  runSlotMachineEffect(star: any) {
    let ticks = 0;
    const maxTicks = 15;
    const targetNumber = star.viewCount;

    const interval = setInterval(() => {
      ticks++;
      if (ticks >= maxTicks) {
        clearInterval(interval);
        star.displayViewCount = targetNumber;
      } else {
        const randomOffset = Math.floor(Math.random() * 30) - 15;
        star.displayViewCount = Math.max(0, targetNumber + randomOffset);
      }
    }, 30);
  }

  onTitleTap() {
    this.titleTapCount++;
    clearTimeout(this.tapTimeout);
    this.tapTimeout = setTimeout(() => { this.titleTapCount = 0; }, 1500);

    if (this.titleTapCount >= 5) {
      this.titleTapCount = 0;
      if (this.isAdmin) this.logout();
      else this.showLogin('ADMIN');
    }
  }

  async showLogin(type: 'STAR' | 'ADMIN') {
    const headerTitle = type === 'STAR' ? 'Creator Login' : 'Admin Login';
    const alert = await this.alertCtrl.create({
      header: headerTitle,
      inputs: [
        { name: 'id', type: 'text', placeholder: 'ID' },
        { name: 'pw', type: 'password', placeholder: 'Password' }
      ],
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Login',
          handler: (data) => {
            const url = type === 'STAR' ? '/api/super/star/login' : '/api/super/admin/login';
            this.http.post(url, { id: data.id, pw: data.pw }).subscribe({
              next: (res: any) => {
                if (res.result === 'OK') {
                  if (type === 'ADMIN') {
                    this.isAdmin = true;
                    this.adminLevel = res.level;
                    localStorage.setItem('isAdmin', 'true');
                    localStorage.setItem('adminId', data.id);
                    localStorage.setItem('adminPw', data.pw);
                    localStorage.setItem('adminLevel', res.level);
                  } else {
                    this.isStar = true;
                    this.starId = res.starId || data.id;
                    localStorage.setItem('isStar', 'true');
                    localStorage.setItem('starId', this.starId);
                    localStorage.setItem('starPw', data.pw);
                  }

                  if (type === 'STAR') {
                    // this.globalFeedback.startPolling();
                    this.registerFCMToken(this.starId); // 로그인 성공한 스타의 ID로 FCM 토큰 등록
                  }

                  this.showSimpleAlert('Login successful!');
                } else {
                  this.showSimpleAlert('Invalid credentials.');
                }
              },
              error: () => this.showSimpleAlert('Server communication error occurred.')
            });
          }
        }
      ]
    });
    await alert.present();
  }

  async logout() {
    const alert = await this.alertCtrl.create({
      header: 'Logout',
      message: 'Are you sure you want to log out?',
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Logout',
          handler: () => {
            // 🔴 [Critical Fix] 초기화 전에 starId를 백업 (토큰 삭제 API에 필요)
            const prevStarId = this.starId;

            this.isAdmin = false;
            this.adminLevel = '';
            this.isStar = false;
            this.starId = '';

            localStorage.removeItem('isAdmin');
            localStorage.removeItem('adminId');
            localStorage.removeItem('adminPw');
            localStorage.removeItem('adminLevel');
            localStorage.removeItem('isStar');
            localStorage.removeItem('starId');
            localStorage.removeItem('starPw');
            localStorage.removeItem('fcmToken'); // 로컬 토큰 캐시도 정리

            // this.globalFeedback.stopPolling();

            this.showSimpleAlert('Logged out successfully.');

            // 🔴 [Critical Fix] 백업한 starId로 서버의 FCM 토큰 해제
            if (prevStarId) {
              this.http.post('/api/super/star/push/token', { starId: prevStarId, fcmToken: '' }).subscribe();
            }
          }
        }
      ]
    });
    await alert.present();
  }

  async showSimpleAlert(msg: string) {
    const alert = await this.alertCtrl.create({ header: 'Notice', message: msg, buttons: ['OK'] });
    await alert.present();
  }

  async openAdminWriteModal() {
    this.writeModalService.openWriteModal(
      this.isStar,
      this.starId,
      this.adminLevel,
      () => {
        if (this.isStar && this.starId) {
          this.router.navigate(['/star', this.starId]);
        } else {
          this.loadLobbyData();
        }
      }
    );
  }

  toggleFavorite(star: any, event: Event) {
    event.stopPropagation();
    let favList = JSON.parse(localStorage.getItem('favorite_stars') || '[]');

    if (star.isFavorite) {
      favList = favList.filter((id: string) => id !== String(star.id));
      star.followerCount = Math.max(0, star.followerCount - 1);
    } else {
      favList.push(String(star.id));
      star.followerCount++;
    }

    localStorage.setItem('favorite_stars', JSON.stringify(favList));
    star.isFavorite = !star.isFavorite;

    const allLists = [this.allStars, this.popularStars, this.newCreators, this.allStarsOriginal, this.newCreatorsOriginal];
    allLists.forEach(list => {
      if (list) {
        list.forEach(s => {
          if (String(s.id) === String(star.id)) {
            s.isFavorite = star.isFavorite;
            s.followerCount = star.followerCount;
          }
        });
      }
    });

    this.http.post('/api/super/star/toggleFollow', { starId: star.id, isAdd: star.isFavorite, deviceId: this.deviceId }).subscribe();

    if (this.isShowingFavorites && !star.isFavorite) {
      this.loadFavoriteStars();
    }
  }

  async goToMyStarPage() {
    if (this.isStar && this.starId) {
      // 가벼운 햅틱(진동) 피드백 (선택사항)
      try {
        const { Haptics, ImpactStyle } = await import('@capacitor/haptics');
        Haptics.impact({ style: ImpactStyle.Light });
      } catch (e) { }

      // 내 스타페이지로 라우팅
      this.router.navigate(['/star', this.starId]);
    }
  }

  // 🌟 [신규 추가] 사람 아이콘 클릭 시 팝업 메뉴 띄우기
  async openProfileMenu(ev: any) {
    const popover = await this.popoverCtrl.create({
      component: ProfileMenuPopoverComponent,
      componentProps: { isAdmin: this.isAdmin },
      event: ev, // 클릭한 버튼 밑에 예쁘게 뜨도록 이벤트 전달
      alignment: 'end',
      side: 'bottom',
      translucent: true
    });

    await popover.present();

    // 메뉴에서 뭔가를 클릭하고 창이 닫혔을 때의 처리
    const { data } = await popover.onDidDismiss();
    if (data) {
      if (data.action === 'mypage') {
        this.goToMyStarPage();
      }
      // 🌟 [신규] 메시지 액션 처리
      else if (data.action === 'messages') {
        this.openMessageModal();
      }
      // 🌟 [신규] 로그아웃 액션 처리
      else if (data.action === 'logout') {
        this.logout();
      }
    }
  }

  handleWriteButtonClick() {
    // 1. 이미 로그인된 상태(관리자 또는 스타)라면 기존처럼 글쓰기 모달 띄우기
    if (this.isAdmin || this.isStar) {
      this.openAdminWriteModal();
    }
    // 2. 로그인되지 않은 상태라면 크리에이터(STAR) 로그인 팝업 띄우기
    else {
      this.openCreatorLogin();
    }
  }

  async openMessageModal() {
    // 햅틱 피드백
    try { await Haptics.impact({ style: ImpactStyle.Light }); } catch (e) { }

    const modal = await this.modalCtrl.create({
      component: MessageModalComponent, // 대화창 컴포넌트
      componentProps: {
        pageId: this.starId, // 현재 내 페이지 ID 전달
        isStar: this.isStar
      },
      // 바텀 시트 스타일로 띄우고 싶다면 아래 설정 추가
      // initialBreakpoint: 0.8,
      // breakpoints: [0, 0.8, 1.0]
    });

    await modal.present();
  }

  // 🌟 [신규] Discover Global Pages - 추천 페이지 로드
  loadRecommendedPages() {
    this.http.post('/api/super/page/discover', {
      excludePageId: ''
    }).subscribe((res: any) => {
      if (res.result === 'OK' && res.list) {
        this.recommendedPages = res.list;
      }
    });
  }
}