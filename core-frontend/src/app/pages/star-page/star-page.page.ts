import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpService } from '../../services/http.service';
import { ActionSheetController, Platform } from '@ionic/angular';
import NativeBridge from 'src/app/plugins/native-bridge';

@Component({
  selector: 'app-star-page',
  templateUrl: './star-page.page.html',
  styleUrls: ['./star-page.page.scss'],
})
export class StarPagePage implements OnInit {
  public defaultAvatar = 'assets/img/defaultImg/avatar.svg';
  starId: string;
  starInfo: any = {};
  gallery: any[] = []; // 갤러리 이미지

  private lastScrollTop: number = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router, // [추가] 주입
    private http: HttpService,
    private actionSheetCtrl: ActionSheetController, // [추가] 주입
    private platform: Platform
  ) {}

  ngOnInit() {
    this.starId = this.route.snapshot.paramMap.get('starId');
    this.loadStarDetail();
  }

  ionViewDidEnter() {
    if (this.platform.is('capacitor')) {
      console.log('Star Page Entered: Showing Native Ad');
      
      // 안드로이드에 "스타 페이지용 광고(172dp 위치) 보여줘" 요청
      NativeBridge.setShow({ show: true, page: 'star' });

      // [수익화 핵심] 서버에 "광고 노출됨" 로그 전송
      this.sendAdLog('NATIVE_BANNER_VIEW');
    }
  }

  ionViewWillLeave() {
    if (this.platform.is('capacitor')) {
      console.log('Star Page Leaving: Hiding Native Ad');
      
      // 안드로이드에 "광고 숨겨줘" 요청
      NativeBridge.setShow({ show: false, page: 'star' });
    }
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
        console.log('스타 상세 정보:', res);
        this.starInfo = res.starInfo;
        this.gallery = res.starInfo.photos || [];
        
        // (선택) 상세 페이지 들어오자마자 로그를 남기고 싶다면 바로 호출
        // this.sendAdLog('NATIVE'); 
      }
    });
  }

  async presentPopover(ev: any) {
    const actionSheet = await this.actionSheetCtrl.create({
      header: '더 보기',
      buttons: [
        {
          text: '검색 로비로 이동', // 체크리스트 37번 항목 연계
          icon: 'search',
          handler: () => {
            // 로비로 이동하면서 검색 모드로 들어가는 UX를 흉내 냅니다.
            this.router.navigate(['/lobby']);
          }
        },
        {
          text: '새로고침',
          icon: 'refresh',
          handler: () => {
            this.loadStarDetail();
          }
        },
        {
          text: '신고하기', // SNS 필수 기능
          icon: 'alert-circle',
          role: 'destructive',
          handler: () => {
            alert('신고가 접수되었습니다.');
          }
        },
        {
          text: '취소',
          icon: 'close',
          role: 'cancel'
        }
      ]
    });
    await actionSheet.present();
  }

  // [핵심] 광고 시청 완료 시 호출할 함수 (나중에 광고 컴포넌트에서 이 함수를 부르면 됩니다)
  sendAdLog(adType: string) {
    const payload = {
      MEM_ID: 'GUEST', // 로그인했다면 AuthService에서 가져온 ID, 아니면 GUEST
      PRS_ID: this.starId,
      AD_TYPE: adType
    };

    this.http.post('/api/ad/log', payload).subscribe((res: any) => {
      console.log('광고 수익 로그 기록됨:', res);
    });
  }

  handleImageError(event: any) {
    event.target.src = this.defaultAvatar;
  }
}