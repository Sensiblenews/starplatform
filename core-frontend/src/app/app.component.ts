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
import { Subscription } from 'rxjs';

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
  private backButtonActive = false;
  private checkMessageSubscription: Subscription;

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
      await this.setStores();
      await this.setAdMob();
    }
    // TODO: CORS 에러 설정 (서버)
    await this.auth.initialize();
    await this.auth.tryAutoLogin();
    this.setSensibleEventPopup();
    if (this.entry === 'root') {
      await this.navigateToLastTab();
    }
    this.system.setInitialized(true);
    await SplashScreen.hide({
      fadeOutDuration: 1000,
    });
    const agreedPush = await this.storage.get('agreed_push');
    if (agreedPush === null) {
      // 푸시서비스 동의 팝업
      await this.AgreePush();
    }

    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        NativeBridge.setShow({
          show: adRoutes.some(route => this.router.url.includes(route)),
          page: event.url
        });
      }
    });

    this.checkMessageService.triggerCheck();
  }

  ngAfterViewChecked() {
    if (!this.checked) {
      this.checked = true;

      this.checkTermsAndShowPopup();
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
      this.helper
        .alert(alertOption)
        .then((confirmAlert) => confirmAlert.present());
    });
  }

  registerDeepLink() {
    App.addListener('appUrlOpen', (event: URLOpenListenerEvent) => {
      this.entry = 'deeplink';
      this.zone.run(() => {
        const domain = 'witch-hunting.com';
        const slug = event.url.split(domain).pop();
        if (!slug) {
          return;
        }
        this.router.navigateByUrl(slug);
      });
    });
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
      if(document.fullscreenElement) {
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
}
