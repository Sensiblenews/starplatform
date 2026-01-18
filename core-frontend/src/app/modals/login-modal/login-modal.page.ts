import { AuthService } from './../../services/auth.service';
import { Component } from '@angular/core';
import { ModalController, Platform } from '@ionic/angular';
import { AuthType } from 'src/app/types/Auth';
import { InAppBrowser } from '@awesome-cordova-plugins/in-app-browser/ngx';
import { CheckMessageService } from 'src/app/services/check-message.service';

@Component({
  selector: 'app-login-modal',
  templateUrl: './login-modal.page.html',
  styleUrls: ['./login-modal.page.scss'],
})
export class LoginModalPage {
  public available: boolean;
  constructor(
    public modalCtrl: ModalController,
    private auth: AuthService,
    private platform: Platform,
    private iab: InAppBrowser,
    private checkMessageService: CheckMessageService,
  ) {
    if (this.auth.isAuthed()) {
      this.modalCtrl.dismiss(true);
    }
    this.platform.ready().then(() => {
      // this.available = this.platform.is('ios');
      this.available = true;
    });
  }

  async doLogin(type: AuthType) {
    if (type === 'AP') {
      let loginSuccess = false;
      try {
        const MEM_TOKEN = await this.tempAppleLogin();
        const { item: userInfo, RESULT } = await this.auth.requestUserByToken(
          MEM_TOKEN,
        );
        if (RESULT === 'FAIL') {
          return false;
        }
        loginSuccess = await this.auth.setUser(userInfo);
      } catch (e) {
        console.log('apple login cancelled');
      } finally {
        this.modalClose(loginSuccess);
      }
    } else {
      const loginSuccess = await this.auth.login(type);
      this.checkMessageService.triggerCheck();
      this.modalClose(loginSuccess);
    }
  }

  async tempAppleLogin(): Promise<string> {
    return new Promise((resolve, reject) => {
      const browser = this.iab.create(
        // 'http://192.168.0.5:8080/appleRestLogin',
        'https://witch-hunting.com/appleRestLogin',
        '_blank'
      );
      let inProgress = true;
      browser.on('loadstop').subscribe((event) => {
        if (event.url.endsWith('loginCallBackApple')) {
          inProgress = false;
          browser
            .executeScript({ code: `document.querySelector('xmp').innerHTML` })
            .then((xmp) => {
              const { sub } = JSON.parse(xmp);
              browser.close();
              resolve(sub);
            });
        }
      });

      browser.on('exit').subscribe(() => {
        if (inProgress) {
          reject('userManuallyClosed');
        }
      });
    });
  }

  modalClose(result = false) {
    this.modalCtrl.dismiss(result);
  }
}
