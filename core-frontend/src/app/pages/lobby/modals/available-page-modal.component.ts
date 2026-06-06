import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms"; // 🌟 [(ngModel)] 사용을 위해 필수!
import { IonicModule, ModalController, AlertController, ToastController, LoadingController } from "@ionic/angular";
import { HttpService } from "src/app/services/http.service";

import { FirebaseAuthService } from "src/app/services/oauth/firebase-auth.service";

@Component({
  selector: 'app-available-page-modal',
  standalone: true,
  imports: [CommonModule, IonicModule, FormsModule],
  template: `
    <ion-header class="ion-no-border">
      <ion-toolbar color="light">
        <ion-title>{{ step === 4 ? 'Welcome!' : 'Create Your Page' }}</ion-title>
        <ion-buttons slot="end">
          <ion-button (click)="dismiss()">Close</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding" style="--background: #f4f5f8;">
      
      <div *ngIf="step === 1" class="step-container">
        <div style="margin-bottom: 20px; text-align: center;">
          <h2 style="font-weight: 800; color: #333;">Choose Your Country</h2>
          <p style="color: #666; font-size: 14px;">Select a region to view available names.</p>
        </div>
        <ion-list lines="none" style="background: transparent;">
          <ion-item *ngFor="let c of countries" (click)="loadAvailablePages(c)" button 
                    style="--background: white; --border-radius: 12px; margin-bottom: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
            <ion-label style="font-weight: 600;">{{ c.name }}</ion-label>
            
          </ion-item>
        </ion-list>
      </div>

      <div *ngIf="step === 2" class="step-container">
        <h2 style="font-weight: 800; text-align: center;">Select Your Name</h2>
        <p style="color: #666; font-size: 14px; text-align: center; margin-bottom: 20px;">
          Available in {{ selectedCountryName }}
        </p>
        
        <ion-list lines="none" style="background: transparent;">
          <ion-item *ngFor="let page of availablePages" (click)="selectPage(page)" button 
                    style="--background: white; --border-radius: 12px; margin-bottom: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.04);">
            <ion-avatar slot="start" style="background: #e0e0e0; padding: 2px;">
              <img src="assets/img/defaultImg/avatar.svg" />
            </ion-avatar>
            <ion-label>
              <h3 style="font-weight: 800; font-size: 18px; color: #1e88e5;">{{ page.slug || page.name }}</h3>
              <p style="color: #e53935; font-size: 12px; font-weight: bold; margin-top: 4px;">
                🔥 {{ page.viewers }} people viewing
              </p>
            </ion-label>
            <ion-badge color="success" slot="end">Available</ion-badge>
          </ion-item>
        </ion-list>
        <ion-button expand="block" fill="clear" (click)="step = 1">Back</ion-button>
      </div>

      <div *ngIf="step === 3" class="step-container">
        <h2 style="font-weight: bold; text-align: center;">Claim This Page</h2>
        <div style="background: white; padding: 20px; border-radius: 12px; text-align: center; margin: 20px 0; box-shadow: 0 4px 10px rgba(0,0,0,0.05);">
          <p style="font-weight: 900; font-size: 22px; color: #1e88e5; margin: 0;">{{ selectedPage?.slug || selectedPage?.name }}</p>
        </div>
        
        <p style="text-align: center; color: #666; margin-bottom: 20px;">This page is almost yours.</p>

        <ion-button expand="block" color="dark" style="margin-bottom: 12px;" (click)="loginSocial('apple')">
          <ion-icon name="logo-apple" slot="start"></ion-icon> Continue with Apple
        </ion-button>
        <ion-button expand="block" color="danger" style="margin-bottom: 20px;" (click)="loginSocial('google')">
          <ion-icon name="logo-google" slot="start"></ion-icon> Continue with Google
        </ion-button>

        
        <ion-button expand="block" fill="clear" (click)="step = 2">Back</ion-button>
      </div>

      <div *ngIf="step === 4" class="step-container" style="text-align: center; padding-top: 40px;">
        <h1 style="font-size: 60px; margin: 0;">🎉</h1>
        <h2 style="font-weight: bold; margin-top: 20px;">Welcome!</h2>
        <p>You now own<br><b style="color: #1e88e5; font-size: 18px;">{{ selectedPage?.slug || selectedPage?.name }}</b></p>
        <ion-button expand="block" style="margin-top: 40px;" (click)="dismissSuccess()">
          Go to My Page
        </ion-button>
      </div>

    </ion-content>
  `
})
export class AvailablePageModalComponent {
  
  step = 1;
  countries = [
    { code: 'KR', name: 'Korea' }, { code: 'US', name: 'United States' },
    { code: 'JP', name: 'Japan' }, { code: 'UK', name: 'United Kingdom' },
    { code: 'FR', name: 'France' }, { code: 'DE', name: 'Germany' }
  ];
  
  selectedCountryCode = '';
  selectedCountryName = '';
  availablePages: any[] = [];
  selectedPage: any = null;
  claimEmail = '';

  constructor(
    private modalCtrl: ModalController,
    private toastCtrl: ToastController,
    private alertCtrl: AlertController,
    private loadingCtrl: LoadingController,
    private http: HttpService,
    private firebaseAuth: FirebaseAuthService,
  ) {}

  // 1. 백엔드에서 빈 페이지 목록 가져오기
  async loadAvailablePages(country: any) {
    this.selectedCountryCode = country.code;
    this.selectedCountryName = country.name;
    
    const loading = await this.loadingCtrl.create({ spinner: 'crescent' });
    await loading.present();

    this.http.get(`/api/super/page/available?country=${country.code}`).subscribe({
      next: (res: any) => {
        loading.dismiss();
        if (res.result === 'OK' && res.list && res.list.length > 0) {
          this.availablePages = res.list;
          // 가짜 시청자 수 주입
          this.availablePages.forEach(p => p.viewers = Math.floor(Math.random() * 5) + 1);
          this.step = 2;
        } else {
          // 백엔드 API가 아직 준비 안 됐을 때를 위한 임시 방어 코드 (테스트용)
          this.availablePages = [
            { slug: 'minji', viewers: 3 }, { slug: 'alex', viewers: 1 }, 
            { slug: 'sakura', viewers: 5 }, { slug: 'junho', viewers: 2 }
          ];
          this.step = 2;
        }
      },
      error: () => {
        loading.dismiss();
        this.showToast('Failed to load pages. Please try again.');
      }
    });
  }

  // 2. 페이지 선택
  selectPage(page: any) {
    this.selectedPage = page;
    this.step = 3;
  }

  // 🌟 3. 소셜 로그인 흐름
  async loginSocial(provider: string) {
    try {
      const loading = await this.loadingCtrl.create({ message: 'Claiming your page...' });
      await loading.present();

      // 🌟 새로 만든 파이어베이스 서비스 호출
      const user = provider === 'google' 
        ? await this.firebaseAuth.signInWithGoogle()
        : await this.firebaseAuth.signInWithApple();

      // 획득한 정보로 백엔드 Claim 찌르기 (🌟 uid 파라미터 추가)
      if (user && user.email) {
        this.http.post('/api/super/page/claim/social', {
          reqName: this.selectedPage.slug || this.selectedPage.name,
          country: this.selectedCountryCode,
          email: user.email,
          uid: user.uid  // 🌟 [핵심] 백엔드 FIREBASE_UID 컬럼에 저장될 값
        }).subscribe((res: any) => {
          loading.dismiss();
          if (res.result === 'OK') {
            localStorage.setItem('isStar', 'true');
            localStorage.setItem('starId', res.starId);
            localStorage.setItem('starToken', res.starToken);

            this.step = 4;
          } else {
            this.showAlert('Oops!', res.msg || 'Already taken by someone else.');
            this.step = 2; // 뺏겼으니 다시 목록으로
          }
        });
      } else {
        loading.dismiss();
        this.showAlert('Error', 'Could not get account information.');
      }
    } catch (e) {
      console.error(e);
      this.showAlert('Error', 'Social login failed or canceled.');
    }
  }

  // 4. 이메일 매직링크 발송 (기존 Alert 방식을 폼 방식으로 개선)
  async sendMagicLink() {
    if (!this.claimEmail) {
      this.showToast('Please enter your email.');
      return;
    }

    const loading = await this.loadingCtrl.create({ message: 'Sending magic link...' });
    await loading.present();

    this.http.post('/api/super/page/claim/lazy', {
      reqName: this.selectedPage.slug || this.selectedPage.name,
      country: this.selectedCountryCode,
      email: this.claimEmail,
      password: 'tmp' + new Date().getTime()
    }).subscribe({
      next: (res: any) => {
        loading.dismiss();
        if (res.result === 'OK') {
          this.showAlert('Check your Inbox', `A magic link has been sent to ${this.claimEmail}. Click it to instantly own your page!`);
        } else {
          this.showAlert('Oops!', res.msg || 'Already taken.');
        }
      },
      error: () => {
        loading.dismiss();
        this.showToast('Failed to send magic link.');
      }
    });
  }

  async showToast(msg: string) {
    const toast = await this.toastCtrl.create({ message: msg, duration: 3000, position: 'bottom' });
    toast.present();
  }

  async showAlert(header: string, message: string) {
    const alert = await this.alertCtrl.create({ header, message, buttons: ['OK'] });
    await alert.present();
  }

  dismiss() { 
    this.modalCtrl.dismiss(); 
  }

  dismissSuccess() {
    this.modalCtrl.dismiss({
      isStar: true,
      starId: localStorage.getItem('starId') || '',
      starToken: localStorage.getItem('starToken') || '',
      isSuccessful: true
    }); 
  }
}