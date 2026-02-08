import { AfterViewInit, Component, ElementRef, NgZone, OnDestroy, OnInit, QueryList, ViewChildren } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpService } from '../../services/http.service';
import { Platform, PopoverController } from '@ionic/angular';
import NativeBridge from 'src/app/plugins/native-bridge';
import { AdMobService } from 'src/app/services/ad-mob.service';
import { AdProtectionService } from 'src/app/services/ad-protection.service';
import { StarMenuComponent } from './star-menu.component';

@Component({
  selector: 'app-star-page',
  templateUrl: './star-page.page.html',
  styleUrls: ['./star-page.page.scss'],
})
export class StarPagePage implements OnInit, AfterViewInit, OnDestroy {
  public defaultAvatar = 'assets/img/defaultImg/avatar.svg';
  starId: string;
  starInfo: any = {};
  gallery: any[] = []; // 갤러리 이미지
  feedList: any[] = [];


  isAdLocked: boolean = false; 
  private clickListener: any;
  private lastScrollTop: number = 0;

  // [신규] 상세 페이지로 이동 중인지 체크하는 깃발
  private isGoingToDetail: boolean = false;

  @ViewChildren('feedVideo') videoElements: QueryList<ElementRef>;
  private observer: IntersectionObserver;

  constructor(
    private route: ActivatedRoute,
    private router: Router, // [추가] 주입
    private http: HttpService,
    // private actionSheetCtrl: ActionSheetController, // [추가] 주입
    private popoverCtrl: PopoverController,
    private platform: Platform,
    private adMobService: AdMobService,
    private adProtection: AdProtectionService, // [주입]
    private ngZone: NgZone // [주입] UI 업데이트용
  ) {}

  async ngOnInit() {
    this.starId = this.route.snapshot.paramMap.get('starId');
    this.loadStarDetail();
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
    if (this.platform.is('capacitor')) {
      const canShow = await this.adProtection.shouldShowAd(this.starId);

      if (canShow) {
        NativeBridge.setShow({ show: true, page: 'star' });
        this.isAdLocked = false;
        
        // [로그 전송] 네이티브 - 노출
        this.sendAdLog('NATIVE', 'IMPRESSION'); 
        
        this.setupClickListener();
      } else {
        NativeBridge.setShow({ show: false, page: 'star' });
        this.isAdLocked = true;
      }
    }
  }

  ionViewWillLeave() {
    if (this.platform.is('capacitor')) {
      NativeBridge.setShow({ show: false, page: 'star' });
      this.removeClickListener();

      if (!this.isGoingToDetail) {
        console.log("Leaving to Lobby/Home -> Show Interstitial Ad");
        
        // 1. 전면 광고 표시
        this.adMobService.showInterstitial();
        
        // 2. [로그 전송] 전면 - 노출 (OS 정보는 함수 내부에서 자동 처리됨)
        this.sendAdLog('INTERSTITIAL', 'IMPRESSION');

      } else {
        this.isGoingToDetail = false;
      }
    }
  }

  // [신규] 클릭 감지 리스너 설정
  setupClickListener() {
    // 중복 등록 방지
    this.removeClickListener();

    this.clickListener = () => {
      console.log('⚡️ 웹에서 광고 클릭 감지됨! (Event Received)');
      
      // Angular Zone 안에서 실행해야 화면(isAdLocked)이 즉시 바뀜
      this.ngZone.run(() => {
        this.handleAdClick();
      });
    };

    // Android/iOS Native에서 보내는 이벤트 수신
    window.addEventListener('ad_click_detected', this.clickListener);
  }

  // [신규] 리스너 제거 헬퍼
  removeClickListener() {
    if (this.clickListener) {
      window.removeEventListener('ad_click_detected', this.clickListener);
      this.clickListener = null;
    }
  }

  // [신규] 실제 클릭 발생 시 처리 (잠금 실행)
  async handleAdClick() {
    console.log('🔒 Ad Clicked -> Activating 24h Lock');
    
    // [로그 전송] 네이티브 - 클릭
    this.sendAdLog('NATIVE', 'CLICK'); 

    await this.adProtection.lockAd(this.starId);
    NativeBridge.setShow({ show: false, page: 'star' });
    this.isAdLocked = true;
    this.removeClickListener();
  }

  onScroll(event: any) {
    if (this.platform.is('capacitor')) {
      const scrollTop = event.detail.scrollTop;
      
      // [방향 계산 로직]
      // 현재 위치가 이전 위치보다 크면 'down', 작으면 'up'
      // (단, 바운스 효과 등으로 음수가 나올 수 있으므로 0보다 작으면 'up' 처리 등 방어 코드 포함 가능)
      let direction = 'down';
      if (scrollTop < this.lastScrollTop) {
        direction = 'up';
      }

      // 안드로이드로 위치(value)와 방향(direction) 전송
      NativeBridge.updateAdPosition({ 
        value: scrollTop, 
        direction: direction 
      });

      // 현재 위치를 '이전 위치'로 저장 (다음 계산을 위해)
      this.lastScrollTop = scrollTop;
    }
  }

  // 백엔드 API 호출: /api/super/star/{starId}
  loadStarDetail() {
    this.http.post(`/api/super/star/${this.starId}`, {}).subscribe((res: any) => {
      if (res.result === 'OK') {
        this.starInfo = res.starInfo;
        const photos = res.starInfo.photos || [];
        
        // [핵심] 광고 슬롯 끼워넣기 (3, 7, 13번째)
        // 배열 인덱스가 밀리는 것을 방지하기 위해 뒤에서부터 넣습니다.
        // 13번째 데이터 뒤 (Index 13 위치)
        if (photos.length >= 13) {
           photos.splice(13, 0, { isAd: true, adId: 3 });
        }
        // 7번째 데이터 뒤 (Index 7 위치)
        if (photos.length >= 7) {
           photos.splice(7, 0, { isAd: true, adId: 2 });
        }
        // 3번째 데이터 뒤 (Index 3 위치)
        if (photos.length >= 3) {
           photos.splice(3, 0, { isAd: true, adId: 1 });
        }

        this.feedList = photos;
      }
    });
  }

  async presentPopover(ev: any) {
    const popover = await this.popoverCtrl.create({
      component: StarMenuComponent, // 아까 만든 메뉴 컴포넌트
      event: ev,                    // [핵심] 클릭한 위치(이벤트)를 전달해야 그 옆에 뜸
      translucent: false,
      showBackdrop: true,
      dismissOnSelect: true,
      cssClass: 'star-menu-popover' // 필요시 스타일 커스텀용 클래스
    });

    await popover.present();

    // 팝오버가 닫힐 때 어떤 메뉴를 눌렀는지 확인
    const { data } = await popover.onDidDismiss();

    if (data && data.action) {
      this.handleMenuAction(data.action);
    }
  }

  handleMenuAction(action: string) {
    switch (action) {
      case 'search':
        // 검색 로비로 이동
        this.router.navigate(['/lobby']);
        break;
      
      case 'refresh':
        // 새로고침
        this.loadStarDetail();
        break;

      case 'report':
        // 신고하기
        alert('신고가 접수되었습니다.');
        break;
    }
  }

  // [핵심] 광고 시청 완료 시 호출할 함수 (나중에 광고 컴포넌트에서 이 함수를 부르면 됩니다)
  sendAdLog(adType: string, action: string) {
    // 1. OS 판별
    let os = 'ANDROID';
    if (this.platform.is('ios') || this.platform.is('ipad') || this.platform.is('iphone')) {
      os = 'IOS';
    }

    const payload = {
      MEM_ID: 'GUEST', // 로그인했다면 실제 ID
      PRS_ID: this.starId,
      AD_TYPE: adType, // 'NATIVE' or 'INTERSTITIAL'
      OS: os,          // 'ANDROID' or 'IOS'
      ACTION: action   // 'IMPRESSION' or 'CLICK'
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

  goDetail(conId: number) {
    this.isGoingToDetail = true;
    // 상세 페이지로 이동 (라우팅 설정 필요)
    this.router.navigate(['/feed-detail', conId]);
  }

  initIntersectionObserver() {
    if (this.observer) {
      this.observer.disconnect();
    }

    const options = {
      root: null, // 뷰포트 기준
      rootMargin: '0px',
      threshold: 0.6 // [중요] 화면에 60% 이상 보일 때 재생
    };

    this.observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        const video = entry.target as HTMLVideoElement;
        
        if (entry.isIntersecting) {
          // 화면에 들어옴 -> 재생
          // (브라우저 정책상 Mute 상태여야 자동재생 성공 확률 높음)
          video.play().catch(err => {
             console.log('Autoplay blocked:', err);
          });
        } else {
          // 화면에서 나감 -> 일시정지
          video.pause();
        }
      });
    }, options);

    // 모든 비디오 태그 감시 등록
    this.videoElements.forEach(el => {
      this.observer.observe(el.nativeElement);
    });
  }

  // [신규] 소리 켜기/끄기 토글
  toggleSound(item: any, event: Event) {
    event.stopPropagation();
    item.isMuted = !item.isMuted;
  }
}