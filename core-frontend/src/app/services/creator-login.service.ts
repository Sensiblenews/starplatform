import { Injectable } from '@angular/core';
import { PushNotifications } from '@capacitor/push-notifications';
import { ActionSheetController, AlertController, Platform } from '@ionic/angular';
import { DmService } from './dm.service';
import { HttpService } from './http.service';
import { FirebaseAuthService } from './oauth/firebase-auth.service';

/** 로그인 결과. 화면마다 로그인 후 할 일이 달라 성공 여부와 starId만 돌려준다 */
export interface CreatorLoginResult {
  ok: boolean;
  starId: string;
}

/**
 * 크리에이터(스타) 로그인 (2-28차에 로비에서 분리).
 *
 * 원래 로비 안에만 있던 흐름이다. 스타페이지 글쓰기 버튼에도 같은 로그인 유도가 필요해지면서
 * 화면마다 복사하는 대신 서비스로 뺐다 — 토큰 저장과 FCM 등록이 두 벌이 되면
 * 한쪽만 고쳐지는 사고가 난다.
 *
 * 화면이 할 일은 로그인 성공 후의 후처리(상태 갱신·데이터 재조회)뿐이다.
 */
@Injectable({
  providedIn: 'root'
})
export class CreatorLoginService {

  constructor(
    private http: HttpService,
    private platform: Platform,
    private alertCtrl: AlertController,
    private actionSheetCtrl: ActionSheetController,
    private firebaseAuth: FirebaseAuthService,
    private dm: DmService,
  ) { }

  /**
   * 크리에이터 로그인 시트를 띄우고 끝까지 처리한다.
   * 사용자가 취소하면 ok=false로 조용히 끝난다 (알림 없음).
   */
  async promptLogin(): Promise<CreatorLoginResult> {
    return new Promise<CreatorLoginResult>(async (resolve) => {
      let picked = false;

      const actionSheet = await this.actionSheetCtrl.create({
        header: 'Creator Login',
        buttons: [
          {
            text: 'Continue with Apple',
            icon: 'logo-apple',
            handler: () => {
              picked = true;
              this.processSocialLogin('apple').then(resolve);
            }
          },
          {
            text: 'Continue with Google',
            icon: 'logo-google',
            handler: () => {
              picked = true;
              this.processSocialLogin('google').then(resolve);
            }
          },
          {
            text: 'Cancel',
            role: 'cancel'
          }
        ]
      });

      await actionSheet.present();
      await actionSheet.onDidDismiss();
      // 취소로 닫힌 경우에만 여기서 끝낸다. 로그인을 고른 경우는 위 핸들러가 resolve한다
      if (!picked) resolve({ ok: false, starId: '' });
    });
  }

  /** 소셜 인증 → 백엔드 로그인 → 토큰 저장 → FCM 등록까지 */
  async processSocialLogin(provider: string): Promise<CreatorLoginResult> {
    try {
      const user = provider === 'google'
        ? await this.firebaseAuth.signInWithGoogle()
        : await this.firebaseAuth.signInWithApple();

      if (!user || !user.email) return { ok: false, starId: '' };

      const res: any = await this.http.post('/api/super/star/login/social', {
        email: user.email,
        uid: user.uid   // 백엔드에서 비밀번호처럼 검증하는 값
      }).toPromise();

      if (!res || res.result !== 'OK') {
        await this.alert((res && res.msg) || 'Account not found. Please create a page first.');
        return { ok: false, starId: '' };
      }

      localStorage.setItem('isStar', 'true');
      localStorage.setItem('starId', res.starId);
      localStorage.setItem('starToken', res.starToken);
      this.dm.refreshUnread();
      this.registerFCMToken(res.starId);
      await this.alert('Login successful! Welcome back.');
      return { ok: true, starId: res.starId };
    } catch (e) {
      console.error('Social login error', e);
      return { ok: false, starId: '' };
    }
  }

  /**
   * 로그인한 스타 ID로 FCM 토큰을 서버에 올린다.
   *
   * registration 이벤트가 안 오는 기기가 있어 캐시된 토큰을 직접 보낸다.
   * 1.5초·4초 두 번 보내는 이유: 로그아웃 시 fcmToken:'' 초기화 요청이 늦게 도착해
   * 재로그인 직후 올린 토큰을 덮어쓰는 레이스가 있다. 두 번째 전송으로 복구한다
   * (같은 값 UPDATE라 부작용 없음).
   */
  async registerFCMToken(targetStarId: string) {
    if (!this.platform.is('capacitor')) return;

    let permStatus = await PushNotifications.checkPermissions();
    if (permStatus.receive === 'prompt') {
      permStatus = await PushNotifications.requestPermissions();
    }
    if (permStatus.receive !== 'granted') return;

    await PushNotifications.register();

    const sendCachedToken = () => {
      const cachedToken = localStorage.getItem('fcmToken');
      if (cachedToken && targetStarId) {
        this.http.post('/api/super/star/push/token', {
          starId: targetStarId,
          fcmToken: cachedToken
        }).subscribe({
          next: () => { },
          error: (err: any) => console.error('FCM token send failed:', err)
        });
      }
    };
    setTimeout(sendCachedToken, 1500);
    setTimeout(sendCachedToken, 4000);
  }

  private async alert(msg: string) {
    const alert = await this.alertCtrl.create({ header: 'Notice', message: msg, buttons: ['OK'] });
    await alert.present();
  }
}
