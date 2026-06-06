import { Component } from '@angular/core';
import { ModalController, AlertController, LoadingController } from '@ionic/angular';
import { HttpService } from 'src/app/services/http.service';
import { GoogleLoginService } from 'src/app/services/oauth/google-login.service';
import { AppleLoginService } from 'src/app/services/oauth/apple-login.service';

// TODO: 지구로또에서 사용하던 소셜 로그인 서비스 임포트
// import { AuthService } from 'src/app/services/auth.service'; 

@Component({
  selector: 'app-ipo-modal',
  templateUrl: './ipo-modal.component.html',
  styleUrls: ['./ipo-modal.component.scss'] 
})
export class IpoModalComponent {

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
    private alertCtrl: AlertController,
    private loadingCtrl: LoadingController,
    private http: HttpService,
    private googleLogin: GoogleLoginService,
    private appleLogin: AppleLoginService
    // private authService: AuthService // 지구로또의 소셜 로그인 서비스 주입
  ) {}

  closeModal() {
    this.modalCtrl.dismiss();
  }

  // 1. 빈 페이지 목록 불러오기 API
  async loadAvailablePages(country: any) {
    this.selectedCountryCode = country.code;
    this.selectedCountryName = country.name;
    
    const loading = await this.loadingCtrl.create({ spinner: 'crescent' });
    await loading.present();

    this.http.get(`/api/super/page/available?country=${country.code}`).subscribe({
      next: (res: any) => {
        loading.dismiss();
        if (res.result === 'OK') {
          this.availablePages = res.list;
          this.availablePages.forEach(p => p.viewers = Math.floor(Math.random() * 5) + 1);
          this.step = 2;
        }
      },
      error: () => { loading.dismiss(); }
    });
  }

  selectPage(page: any) {
    this.selectedPage = page;
    this.step = 3;
  }

  // 🌟 2. 소셜 로그인 흐름 (지구로또 코드 적용부)
  async loginSocial(provider: string) {
    try {
      const loading = await this.loadingCtrl.create({ message: 'Claiming...' });
      await loading.present();

      let socialEmail = '';

      // 🌟 1. 소셜 로그인 서비스 직접 호출하여 정보 빼오기
      if (provider === 'google') {
        const userInfo = await this.googleLogin.login();
        socialEmail = userInfo?.email; // 구글 이메일 확보
      } 
      else if (provider === 'apple') {
        const userInfo = await this.appleLogin.login();
        // 애플은 이메일이 안 올 수 있으므로, 고유 식별자인 AP_TOKEN(user.id)을 이메일 대용으로 씁니다.
        socialEmail = userInfo?.apUser?.AP_TOKEN ? `${userInfo.apUser.AP_TOKEN}@apple.com` : ''; 
      }

      // 🌟 2. 빼온 이메일로 백엔드 Claim API 찌르기
      if (socialEmail) {
        this.http.post('/api/super/page/claim/social', {
          reqName: this.selectedPage.slug,
          country: this.selectedCountryCode,
          email: socialEmail
        }).subscribe(async (res: any) => {
          loading.dismiss();
          if (res.result === 'OK') {
            this.step = 4;
          } else {
            this.showAlert('Oops!', res.msg || 'Already taken.');
            this.step = 2; // 목록으로 돌려보냄
          }
        });
      } else {
        loading.dismiss();
        this.showAlert('Error', 'Could not retrieve user information.');
      }
    } catch (e) {
      console.error(e);
      this.showAlert('Error', 'Social login failed.');
    }
  }

  // 3. 기존에 만들었던 매직링크 발송 (이메일 로그인)
  async sendMagicLink() {
    if (!this.claimEmail) {
      this.showAlert('Notice', 'Please enter your email.');
      return;
    }

    const loading = await this.loadingCtrl.create({ message: 'Sending magic link...' });
    await loading.present();

    this.http.post('/api/super/page/claim/lazy', {
      reqName: this.selectedPage.slug,
      country: this.selectedCountryCode,
      email: this.claimEmail,
      password: 'tmp' + new Date().getTime()
    }).subscribe({
      next: (res: any) => {
        loading.dismiss();
        if (res.result === 'OK') {
          this.showAlert('Check your Inbox', `A magic link has been sent to ${this.claimEmail}. Click it to own your page!`);
        } else {
          this.showAlert('Oops!', res.msg);
        }
      },
      error: () => { loading.dismiss(); }
    });
  }

  async showAlert(header: string, message: string) {
    const alert = await this.alertCtrl.create({ header, message, buttons: ['OK'] });
    await alert.present();
  }
}