import { Component, ElementRef, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { HttpService } from '../../../services/http.service';
import { HelperService } from 'src/app/services/helper.service';
import { Auth, createUserWithEmailAndPassword, fetchSignInMethodsForEmail, signInWithEmailAndPassword } from '@angular/fire/auth';

@Component({
  selector: 'app-email-login-modal',
  templateUrl: './email-login-modal.component.html',
  styleUrls: ['./email-login-modal.component.scss'],
})
export class EmailLoginModalComponent implements OnInit {
  email = '';
  password = '';
  isLoggingIn = false;

  constructor(
    private modalCtrl: ModalController,
    private http: HttpService,
    private helper: HelperService,
    private auth: Auth
  ) { }

  ngOnInit() {}

  async onLogin() {
    let userCredential: any;
    this.isLoggingIn = true;
    if (!this.email || !this.password) {
      await this.presentAlert('입력 오류', '이메일과 비밀번호를 모두 입력해주세요.');
      this.isLoggingIn = false;
      return;
    }

    if (!this.isValidPassword(this.password) || !this.isValidEmail(this.email)) {
      await this.presentAlert('입력 오류', '이메일 또는 비밀번호의 형식이 올바르지 않습니다.');
      this.isLoggingIn = false;
      return;
    }

    this.email = this.email.trim().toLowerCase();
    this.password = this.password.trim();

    console.log(this.email);
    console.log(this.password);

    try {
      console.log("start");
      userCredential = await signInWithEmailAndPassword(this.auth, this.email, this.password);
      console.log("end");
      await this.modalCtrl.dismiss({ email: this.email, password: this.password, new: false, userCredential });
      this.isLoggingIn = false;
    } catch (error: any) {
      console.log("error")
      console.log(error.code);
      if (error.code === 'auth/invalid-credential') {
        try {
          const userCredential = await createUserWithEmailAndPassword(this.auth, this.email, this.password);
          await this.modalCtrl.dismiss({ email: this.email, password: this.password, new: true, userCredential });
        } catch (e) {
          if (e.code === 'auth/email-already-in-use') {
            await this.presentAlert('계정 정보 오류', '이메일과 비밀번호를 확인해주세요.');
          } else {
            await this.presentAlert('오류', '알 수 없는 오류가 발생했습니다.');
          }
        }
      } else {
        await this.presentAlert('오류', '알 수 없는 오류가 발생했습니다.');
      }
      this.isLoggingIn = false;
    }
  }

  async dismiss() {
    await this.modalCtrl.dismiss();
  }

  async presentAlert(header: string, message: string) {
    const alert = await this.helper.alert({
      header,
      message,
      buttons: ['확인'],
    });
    await alert.present();

    await alert.onDidDismiss();
  }

  isValidPassword(password: string) {
    return /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9])[^\s]{8,}$/.test(password);
  }

  isValidEmail(email: string): boolean {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
  }
}
