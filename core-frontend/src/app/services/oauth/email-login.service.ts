import { Injectable } from '@angular/core';
import type { FBUser, UserInfo } from '../../types/Auth';
import { ModalController } from '@ionic/angular';
import { EmailLoginModalComponent } from '../../modals/login-modal/email-login-modal/email-login-modal.component';
import { Auth, signOut } from '@angular/fire/auth';

@Injectable({
  providedIn: 'root',
})
export class EmailLoginService {
  constructor(
    private modalCtrl: ModalController,
    private auth: Auth,
  ) {}

  async ready(): Promise<boolean> {
    return true;
  }

  /**
   * 이메일 로그인 기능 구현(구 페이스북 로그인과 통합함);
   * @returns 유저정보
   */
  async login(): Promise<UserInfo> {

    const modal = await this.modalCtrl.create({
      component: EmailLoginModalComponent
    });
    await modal.present();
    const { data } = await modal.onDidDismiss();
    
    if (data.email) {
      const userCredential = data.userCredential;

      const MEM_TYPE = 'FB';
      const MEM_NAME = userCredential.user.email;
      const fbUser: FBUser = {
        FB_KEY: userCredential.user.uid,
        FB_TOKEN: userCredential.user.accessToken,
        FB_TOKEN_EXPD: '',
        FB_PICTURE: '',
      };

      return { MEM_TYPE, MEM_NAME, fbUser };
    }

    return null;
  }

  async logout(): Promise<boolean> {
    try {
      signOut(this.auth);
      return true;
    } catch (error) {
      console.error(`[FacebookAuth] logoutFailed - ${error.message}`);
      return false;
    }
  }
}
