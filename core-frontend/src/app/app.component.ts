import { AfterViewChecked, Component, NgZone, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { AdOptions } from '@capacitor-community/admob';
import { App, BackButtonListenerEvent, URLOpenListenerEvent } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { SplashScreen } from '@capacitor/splash-screen';
import { ModalController, Platform } from '@ionic/angular';
import { PopupModalPage } from './modals/popup-modal/popup-modal.page';
import { AdMobService } from './services/ad-mob.service';
import { AuthService } from './services/auth.service';
import { HelperService } from './services/helper.service';
import { NotificationsService } from './services/notifications.service';
import { SensibleEventService } from './services/sensible-event.service';
import { StorageService } from './services/storage.service';
import { StoreService } from './services/store.service';
import { SystemService } from './services/system.service';
import NativeBridge from './plugins/native-bridge';
import { ContentResponse } from './types/Content';
import { HttpService } from './services/http.service';
import { TermsBigModal } from './modals/terms-big/terms-big.component';
import { CheckMessageService } from './services/check-message.service';
import { TextZoom } from '@capacitor/text-zoom';
import { PushNotifications } from '@capacitor/push-notifications';
import { Badge } from '@capawesome/capacitor-badge';

export const interstitialOptions: AdOptions = {
  adId: 'ca-app-pub-9109251900558498/1182509295',
};

const adRoutes = ['goblin', 'fairy', 'tinker-bell', 'peter-pan'];

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  styleUrls: ['app.component.scss'],
})
export class AppComponent implements AfterViewChecked, OnDestroy {
  private entry = 'root';
  private checked = false;
  private backButtonSubscription: any;

  constructor(
    private platform: Platform,
    private zone: NgZone,
    private store: StoreService,
    private auth: AuthService,
    private helper: HelperService,
    private admob: AdMobService,
    private router: Router,
    private sensibleEvent: SensibleEventService,
    private storage: StorageService,
    private system: SystemService,
    private pushService: NotificationsService,
    private http: HttpService,
    private modalCtrl: ModalController,
    private checkMessageService: CheckMessageService,
  ) {
    this.initApp();
  }

  async initApp() {
    await this.platform.ready();

    if (Capacitor.isNativePlatform()) {
      console.log('initializing..');
      this.registerDeepLink();
      this.setupBackButtonExit();
      await this.limitTextZoom();

      this.setupPushNotifications(); // 🌟 [신규 추가] 앱 시작 시 1회만 등록되는 전역 푸시 리스너

      // 🌟 [신규 추가] 앱 활성화(포그라운드 복귀) 시 뱃지 및 알림 초기화 (iOS 전용)
      App.addListener('appStateChange', (state) => {
        if (state.isActive) {
          this.clearBadgesAndNotifications();
        }
      });

      // 앱 구동 시 최초 1회 초기화 (iOS 전용)
      this.clearBadgesAndNotifications();

      // 🌟 [최적화] 미사용 기능 주석 처리로 병목 구간 완벽 제거
      // await this.setStores();

      // 광고 초기화는 필요하므로 단독으로 실행
      Promise.all([
        this.setAdMob(),
        await this.auth.initialize(),
        // this.setStores(), // 스토어 초기화는 광고보다 나중에 실행해도 무방하므로 병목 제거 위해 주석 처리
      ]).catch(error => {
        console.error('초기화 중 오류 발생:', error);
      });
    }

    // 🌟 로그인 기능 미사용으로 주석 처리

    // await this.auth.tryAutoLogin();

    this.setSensibleEventPopup();

    if (this.entry === 'root') {
      await this.restoreLastRoute();
    }

    this.system.setInitialized(true);

    // 🚀 무거운 짐을 다 내려놓았으니 여기서 스플래시가 즉시 닫힙니다!
    await SplashScreen.hide({
      fadeOutDuration: 1000,
    });

    const agreedPush = await this.storage.get('agreed_push');
    if (agreedPush === null) {
      // 푸시서비스 동의 팝업
      // await this.AgreePush();
    }

    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        NativeBridge.setShow({
          show: adRoutes.some(route => this.router.url.includes(route)),
          page: event.url
        });
      }
    });

    this.router.events.subscribe(async (event) => { // async 추가
      if (event instanceof NavigationEnd) {
        const currentUrl = event.urlAfterRedirects || event.url;

        NativeBridge.setShow({
          show: adRoutes.some(route => currentUrl.includes(route)),
          page: currentUrl
        });

        // 로비나 기본 경로일 때는 저장된 기록을 초기화하고, 그 외의 페이지는 저장
        if (currentUrl === '/lobby' || currentUrl === '/') {
          await this.storage.set('last_visited_url', '');
        } else {
          await this.storage.set('last_visited_url', currentUrl);
        }
      }
    });

    this.checkMessageService.triggerCheck();
  }

  ngAfterViewChecked() {
    if (!this.checked) {
      this.checked = true;

      // this.checkTermsAndShowPopup();
      setTimeout(() => {
        console.log(this.router.url);
        NativeBridge.setShow({
          show: adRoutes.some(route => this.router.url.includes(route)),
          page: this.router.url,
        });
      }, 300);
    }
  }

  ngOnDestroy(): void {
    if (this.backButtonSubscription) {
      this.backButtonSubscription.unsubscribe();
    }
  }

  // 🌟 [신규 추가] 앱 시작 시 1회만 등록되는 전역 푸시 리스너
  async setupPushNotifications() {
    console.log('[starfcm] 🚀 setupPushNotifications 시작');
    if (Capacitor.isNativePlatform()) {

      if (this.platform.is('android')) {
        await PushNotifications.createChannel({
          id: 'star_visitor_channel',
          name: 'Visitor Alerts',
          description: 'Notifications for star visitors',
          importance: 5, // Max importance for heads-up notifications
          visibility: 1, // Public visibility on lock screen
          sound: 'tick',
          vibration: true
        })
        console.log('[starfcm] ✅ Android 알림 채널 생성 완료');
      }

      // 1. 토큰 발급/갱신 시 localStorage에 캐싱 + 백엔드로 자동 전송
      PushNotifications.addListener('registration', (token) => {
        console.log('[starfcm] ✅ 토큰 생성 성공:', token.value);

        // 🔴 [Critical Fix] 토큰을 localStorage에 캐싱 (로그인 시 직접 전송용)
        localStorage.setItem('fcmToken', token.value);

        const isStar = localStorage.getItem('isStar') === 'true';
        const starId = localStorage.getItem('starId');

        // 로그인된 상태라면 조용히 서버에 최신 토큰 업데이트
        if (isStar && starId) {
          console.log('[starfcm] 📡 로그인 상태 — 서버에 토큰 전송 시도 (starId:', starId, ')');
          this.http.post('/api/super/star/push/token', {
            starId: starId,
            fcmToken: token.value
          }).subscribe({
            next: () => console.log('[starfcm] ✅ 서버 토큰 전송 성공'),
            error: (err: any) => console.error('[starfcm] ❌ 서버 토큰 전송 실패:', err)
          });
        } else {
          console.log('[starfcm] ℹ️ 비로그인 상태 — 토큰 캐싱만 완료 (서버 전송 생략)');
        }
      });

      // 1-1. 토큰 생성 실패 리스너
      PushNotifications.addListener('registrationError', (error) => {
        console.error('[starfcm] ❌ 토큰 생성 실패:', JSON.stringify(error));
      });

      // 2. 포그라운드(앱 켜진 상태)에서 푸시 도착
      PushNotifications.addListener('pushNotificationReceived', (notification) => {
        console.log('[starfcm] 📩 포그라운드 푸시 수신:', notification);
      });

      // 3. 알림 클릭 시 동작 (딥링크 등 연동 가능)
      PushNotifications.addListener('pushNotificationActionPerformed', (notification) => {
        console.log('[starfcm] 👆 푸시 알림 클릭:', notification);
      });

      // 4. 앱 구동 시 권한 체크 후, 이미 허용(granted)된 유저면 바로 등록(토큰 갱신)
      const permStatus = await PushNotifications.checkPermissions();
      console.log('[starfcm] 🔑 현재 권한 상태:', permStatus.receive);

      if (permStatus.receive === 'granted') {
        console.log('[starfcm] ✅ 권한 이미 허용됨 — register() 호출');
        try {
          await PushNotifications.register();
          console.log('[starfcm] ✅ register() 호출 성공 (토큰 대기 중...)');
        } catch (error) {
          console.error('[starfcm] ❌ register() 호출 실패:', error);
        }
      } else {
        console.log('[starfcm] ⚠️ 권한 미허용 — requestPermissions() 호출');
        try {
          const permission = await PushNotifications.requestPermissions();
          console.log('[starfcm] 🔑 권한 요청 결과:', permission.receive);
          if (permission.receive === 'granted') {
            await PushNotifications.register();
            console.log('[starfcm] ✅ 권한 허용 후 register() 호출 성공');
          } else {
            console.warn('[starfcm] ⛔ 사용자가 푸시 권한 거부');
          }
        } catch (error) {
          console.error('[starfcm] ❌ 권한 요청 중 에러:', error);
        }
      }
    } else {
      console.log('[starfcm] ℹ️ 웹 환경 — 푸시 알림 비활성화');
    }
  }

  async limitTextZoom() {
    try {
      // 기기의 현재 텍스트 줌 비율을 가져옵니다 (1.0이 기본)
      const { value } = await TextZoom.getPreferred();

      // 만약 1.2배(120%)보다 크다면, 1.2배로 고정해버립니다.
      if (value > 1.1) {
        await TextZoom.set({ value: 1.1 });
      }
    } catch (e) {
      console.error('Text Zoom error:', e);
    }
  }

  async restoreLastRoute() {
    const lastUrl = await this.storage.get('last_visited_url');

    // 저장된 경로가 존재하고, 그 경로가 로비가 아닐 경우에만 이동
    if (lastUrl && lastUrl !== '/' && lastUrl !== '/lobby') {
      console.log('🔄 마지막 방문 페이지로 복구:', lastUrl);

      // 앱 초기화 및 렌더링 사이클을 고려해 약간의 딜레이 후 이동
      setTimeout(() => {
        this.router.navigateByUrl(lastUrl);
      }, 150);
    }
  }

  async AgreePush() {
    const confirmModal = await this.helper.alert({
      header: '알림 동의',
      message: '스타플랫폼에서 보내는 알림(푸시)을 받아보시겠습니까?',
      buttons: [
        { text: '확인', role: 'ok' },
        { text: '취소', role: 'cancel' },
      ],
    });
    confirmModal.present();

    const { role } = await confirmModal.onDidDismiss();

    if (role === 'ok') {
      try {
        this.pushService.initPush();
        await this.storage.set('agreed_push', 'yes');
      } catch (error) {
        this.helper.toast(`에러가 발생했습니다. ${error.message}`, 'middle');
      } finally {
        await this.helper.loadEnd();
      }
    } else {
      await this.storage.set('agreed_push', 'no');
    }
  }

  async navigateToLastTab() {
    const lastTab = await this.storage.get('lastTab');
    this.router.navigate(['tabs', lastTab?.length ? lastTab : 'fairy']);
  }

  async setAdMob(): Promise<void> {
    await this.admob.initialize();
    // await this.admob.showBanner();
  }

  async setSensibleEventPopup(): Promise<void> {
    const popupContent = await this.sensibleEvent.updatePopup();
    if (!popupContent) {
      return;
    }
    const popupModule = await this.helper.callModal({
      component: PopupModalPage,
      componentProps: { popupContent },
      backdropDismiss: true,
      keyboardClose: true,
    });
    popupModule.present();
    const { data } = await popupModule.onDidDismiss();
    if (data) {
      this.sensibleEvent.saveAppliedPopup({
        CON_ID: popupContent.CON_ID,
        timestamp: Date.now(),
      });
    }
  }

  async setStores(): Promise<void> {
    await this.store.initialize();
    this.store.purchased.subscribe((info) => {
      if (!info) {
        return;
      }

      let alertOption = {};
      if (info.state === 'success') {
        if (info.id === 'monthly_subscription_2.99') {
          // A. 구독 상품일 경우: 별 충전 메시지 대신 구독 완료 메시지 표시
          alertOption = {
            header: '구독 완료!',
            subHeader: `상품명 - ${info.chargeInfo.name || 'Exclusive Subscription'}`,
            message: '구독이 성공적으로 시작되었습니다.',
          };
        } else {
          // B. 기존 별 상품일 경우: 별 충전 메시지 표시
          alertOption = {
            header: '결제완료 !',
            subHeader: `상품명 - ${info.chargeInfo.name}`,
            message: `별 ${info.chargeInfo.starCount}가 충전되었습니다.`,
          };
        }
      }
      if (info.state === 'cancelled') {
        alertOption = {
          header: '결제취소 !',
          subHeader: `상품명 - ${info.chargeInfo.name}`,
          message: `결제가 취소 되었습니다.`,
        };
      }
      // this.helper
      //   .alert(alertOption)
      //   .then((confirmAlert) => confirmAlert.present());
    });
  }

  async registerDeepLink() {
    // 1. 앱이 완전히 종료된 상태에서 딥링크로 켜졌을 때 (콜드 스타트 - 추가됨)
    const launchUrl = await App.getLaunchUrl();
    if (launchUrl && launchUrl.url) {
      console.log('🚀 콜드 스타트 딥링크 감지됨:', launchUrl.url);
      this.handleDeepLinkUrl(launchUrl.url);
    }

    // 2. 앱이 백그라운드에 있을 때 딥링크로 켜졌을 때 (기존 로직)
    App.addListener('appUrlOpen', (event: URLOpenListenerEvent) => {
      console.log('🔗 백그라운드 딥링크 감지됨:', event.url);
      this.handleDeepLinkUrl(event.url);
    });
  }

  // 기존에 appUrlOpen 안에 있던 파싱 및 라우팅 로직을 별도 함수로 분리
  private handleDeepLinkUrl(urlStr: string) {
    this.entry = 'deeplink';

    this.zone.run(() => {
      try {
        const urlObj = new URL(urlStr);

        // 🌟 1. 커스텀 스킴 (witchhunting://) 감지 시
        if (urlObj.protocol === 'witchhunting:') {
          const host = urlObj.hostname;

          if (host === 'claim-verify') {
            const starId = urlObj.searchParams.get('starId');
            const email = urlObj.searchParams.get('email');
            const pw = urlObj.searchParams.get('pw');

            console.log('✅ 매직 로그인 감지:', { starId, email });

            if (starId && email) {
              this.executeMagicLogin(starId, email, pw);
              return;
            }
          } else {
            let path = '/' + host + urlObj.pathname;

            if (path.startsWith('/witch')) {
              path = path.replace('/witch', '');
            }

            const routeParts = path.split('/').filter(p => p !== '');

            if (routeParts.length >= 2) {
              const type = routeParts[0];
              const id = routeParts[1];

              if (type === 'post') {
                console.log('✅ 딥링크 피드 페이지 이동:', id);
                this.router.navigate(['/feed-detail', id]);
                return;
              } else if (type === 'star') {
                console.log('✅ 딥링크 스타 페이지 이동:', id);
                this.router.navigate(['/star', id]);
                return;
              } else {
                console.log('✅ 딥링크 일반 이동:', path);
                this.router.navigateByUrl(path);
                return;
              }
            } else {
              console.log('✅ 딥링크 일반 이동:', path);
              this.router.navigateByUrl(path);
              return;
            }
          }
        }

        // 🌟 2. 기존 유니버셜 링크 (https://witch-hunting.com/...) 감지 시
        const domain = 'witch-hunting.com';
        if (urlStr.includes(domain)) {
          if (urlStr.includes('/witch/privacy') || urlStr.includes('/witch/terms')) {
            return;
          }
          const pathArray = urlStr.split(domain);

          if (pathArray.length > 1) {
            let path = pathArray[1];

            if (path.startsWith('/witch')) {
              path = path.replace('/witch', '');
            }

            const routeParts = path.split('/').filter(p => p !== '');

            if (routeParts.length >= 2) {
              const type = routeParts[0];
              const id = routeParts[1];

              if (type === 'post') {
                this.router.navigate(['/feed-detail', id]);
              } else if (type === 'star') {
                this.router.navigate(['/star', id]);
              } else {
                this.router.navigateByUrl(path);
              }
            } else {
              this.router.navigateByUrl(path);
            }
          }
        }
      } catch (error) {
        console.error('URL 파싱 중 에러 발생:', error);
      }
    });
  }

  private executeMagicLogin(starId: string, email: string, pw: string) {
    console.log('✨ Magic Login 실행:', starId);

    // 1. 권한 정보를 로컬 스토리지에 즉시 주입
    localStorage.setItem('isStar', 'true');
    localStorage.setItem('starId', starId);
    localStorage.setItem('ownerEmail', email);
    localStorage.setItem('starPw', pw);

    // 2. 알림 메시지 출력
    this.helper.toast('Login successful! You are now the owner.', 'bottom');

    // 3. 내 스타페이지로 즉시 이동
    this.router.navigate(['/star', starId]);
  }

  async checkTermsAndShowPopup() {
    const hasAgreed = await this.storage.get('hasAgreedToTerms');
    console.log('약관 동의 팝업 실행');

    if (!hasAgreed) {
      await this.presentTermsPopup();
    }
  }

  async presentTermsPopup() {
    const modal = await this.modalCtrl.create({
      component: TermsBigModal,
      backdropDismiss: false,
      componentProps: {
        terms: await this.getTerms()
      },
      // cssClass: 'loginView'
    });

    this.backButtonSubscription = this.platform.backButton.subscribeWithPriority(9999, () => {
      App.exitApp();
    });

    await modal.present();

    const { data } = await modal.onDidDismiss();

    if (this.backButtonSubscription) {
      this.backButtonSubscription.unsubscribe();
      this.backButtonSubscription = null;
    }

    if (data) {
      await this.storage.set('hasAgreedToTerms', 'true');
    } else {
      await App.exitApp(); // 네이티브에서만 동작
    }
  }

  getTermsAgreementsPolicy(index: number = 0): Promise<ContentResponse> {
    const url = `/app/policyDetail?index=${index}`;
    return this.http.post<ContentResponse>(url, null).toPromise();
  }

  async getTerms() {
    const termsCount = [0, 1];
    const termsRequest = termsCount.map((i) => this.getTermsAgreementsPolicy(i));
    const termsResponses = await Promise.all(termsRequest);
    const receivedTerms = termsResponses.filter(term => (term.result as { isSuccess: number }).isSuccess === 1,);
    if (receivedTerms.length !== termsCount.length) {
      console.error('약관 받아오지 못함');
      return [];
    }
    return receivedTerms.map(terms => terms.contents);
  }

  setupBackButtonExit() {
    this.backButtonSubscription = App.addListener('backButton', (event: BackButtonListenerEvent) => {
      if (document.fullscreenElement) {
        document.exitFullscreen();
      } else if (event.canGoBack) {
        window.history.back();
      } else {
        App.exitApp();
      }
    });
  }

  checkNewMessages(memId: number | string) {
    const body = { MEM_ID: memId };
    console.log("checking new messages for MEM_ID:", memId);
    this.http.post('/api/newMessage', body).subscribe((res: any) => {
      console.log("newMessage API response: ", res.toString());
      const hasNewMessage = res.UNREAD_COUNT && res.UNREAD_COUNT > 0;
      this.checkMessageService.updateNewMessageState(hasNewMessage);
    });
  }

  private async clearBadgesAndNotifications() {
    if (Capacitor.isNativePlatform() && this.platform.is('ios')) {
      try {
        await Badge.clear();
        await PushNotifications.removeAllDeliveredNotifications();
        console.log('[Badge/Notification] Cleared badge and notifications successfully on iOS');
      } catch (error) {
        console.error('[Badge/Notification] Failed to clear badge and notifications:', error);
      }
    }
  }
}
