import { Injectable } from '@angular/core';
import {
  AlertController,
  AlertOptions,
  LoadingController,
  LoadingOptions,
  ModalController,
  ModalOptions,
  PopoverController,
  ToastController,
} from '@ionic/angular';
import {
  EmailComposer,
  EmailComposerOptions,
} from '@awesome-cordova-plugins/email-composer/ngx';
import { SocialSharing } from '@awesome-cordova-plugins/social-sharing/ngx';
import { InAppBrowser } from '@awesome-cordova-plugins/in-app-browser/ngx';

interface ShareOptions {
  subject?: string;
  message?: string;
  files?: string[];
  url?: string;
  chooserTitle?: string;
}

@Injectable({
  providedIn: 'root',
})
export class HelperService {
  public loader: HTMLIonLoadingElement;
  public loaderTimer: ReturnType<typeof setTimeout>;
  constructor(
    private alertCtrl: AlertController,
    private email: EmailComposer,
    private loadingCtrl: LoadingController,
    private toastCtrl: ToastController,
    private modalCtrl: ModalController,
    private popOverCtrl: PopoverController,
    private iab: InAppBrowser,
    private sharing: SocialSharing,
  ) {}

  async alert(option?: AlertOptions): Promise<HTMLIonAlertElement> {
    const customOpt = {
      backdropDismiss: false,
      keyboardClose: false,
      message: '',
      buttons: [
        {
          text: '확인',
          role: 'cancel',
        },
      ],
      ...option,
    };
    const alert = await this.alertCtrl.create(customOpt);
    return alert;
  }

  async loading(msg?: string, opt?: LoadingOptions): Promise<void> {
    await this.loadEnd();
    if (!this.loaderTimer) {
      this.loader = await this.loadingCtrl.create({
        cssClass: `clean-loading ${msg ? 'with-text' : ''}`,
        spinner: msg ? 'crescent' : 'dots',
        message: msg,
        ...opt,
      });

      this.loaderTimer = setTimeout(() => {
        if (this.loader) {
          this.loader.present();
        }
        this.loaderTimer = null;
      }, 500);
    }
  }

  async loadEnd(): Promise<boolean> {
    try {
      if (!this.loader) {
        return true;
      }
      return this.loader.dismiss();
    } catch (e) {
      console.warn('Nothing to dismiss');
    } finally {
      if (this.loaderTimer) {
        clearTimeout(this.loaderTimer);
        this.loaderTimer = null;
      }
      this.loader = null;
    }
  }

  async callModal(modalOpt: ModalOptions): Promise<HTMLIonModalElement> {
    if (!modalOpt.component) {
      throw new Error('No Components');
    }
    return await this.modalCtrl.create(modalOpt);
  }

  openURL(openOptions: { url: string }): void {
    if (!openOptions.url?.length) {
      throw new Error('No Specific URL');
    }
    const dimmer = document.getElementById('iab-dimmer');
    
    // 1. InAppBrowser 열기 직전에 웹뷰에 오버레이 활성화
    if (dimmer) {
      dimmer.classList.add('active');
    }

    // 2. InAppBrowser 인스턴스 생성 및 이벤트 리스너 추가
    // InAppBrowser는 새로운 브라우저 객체를 반환합니다.
    const browser = this.iab.create(openOptions.url, '_blank');
    
    // 3. 브라우저가 닫힐 때 오버레이 비활성화
    browser.on('exit').subscribe(() => {
      if (dimmer) {
        dimmer.classList.remove('active');
      }
    });
  }

  toastSet(msg, position: 'top' | 'bottom' | 'middle' = 'bottom'): void {
    this.toastCtrl
      .create({
        message: msg,
        cssClass: 'earthToast',
        animated: true,
        mode: 'ios',
        position,
        duration: 1500,
      })
      .then((toast) => toast.present());
  }

  toast(msg: string, position: 'top' | 'bottom' | 'middle' = 'bottom') {
    this.toastCtrl.dismiss().finally(() => {
      this.toastSet(msg, position);
    });
  }

  async popOver(popOverOptions): Promise<HTMLIonPopoverElement> {
    const popOver = await this.popOverCtrl.create(popOverOptions);
    popOver.present();
    return popOver;
  }

  async sendEmail(emailOptions: EmailComposerOptions): Promise<boolean> {
    await this.email.open(emailOptions);
    return true;
  }

  async share(shareOption: ShareOptions): Promise<void> {
    const defaultOptions: ShareOptions = {
      chooserTitle: '공유하기',
      message: '스타플랫폼 앱을 다운받아 보세요.\r\nhttps://witch-hunting.com/appUrl',
      // url: 'https://witch-hunting.com/appUrl',
      subject: '스타플랫폼',
    };
    await this.sharing.shareWithOptions(
      Object.assign({}, defaultOptions, shareOption),
    );
  }
}
