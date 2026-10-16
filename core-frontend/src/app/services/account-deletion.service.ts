import { Injectable } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { FirebaseAuthentication } from '@capacitor-firebase/authentication';
import { AlertController, LoadingController } from '@ionic/angular';
import { HttpService } from './http.service';

/** 확인 입력란에 그대로 쳐야 하는 문구 */
export const DELETE_CONFIRM_WORD = 'DELETE';

/**
 * 크리에이터 계정 삭제 (2026-10-17 App Store 심사 5.1.1(v)).
 *
 * 흐름: 확인 창(DELETE 입력) → iOS·Apple 로그인 계정이면 Apple 토큰 해지 → 서버 삭제 → 로그인 정보 정리.
 * 서버가 지우는 범위는 SuperAppService.deleteStarAccount 주석 참조 (게시물·DM·로그인 정보, 페이지는 소유 해제).
 *
 * Apple 토큰 해지: Apple 은 "Sign in with Apple 계정을 삭제할 때 토큰도 해지"를 요구한다.
 * 해지에는 새 authorization code 가 필요해 Apple 로그인을 한 번 더 거친다(Face ID). 사용자가 그 창을 닫으면
 * 삭제를 중단한다. 해지 자체가 실패하면(네트워크 등) 삭제는 계속한다 — 계정 데이터 삭제가 우선이다.
 * 이 해지는 iOS 에서만 된다(플러그인 제약). Android 의 Apple 로그인 계정은 서버가 Firebase 계정을 지우는 데까지만.
 *
 * 화면은 성공 시 로그인 상태만 비우면 된다.
 */
@Injectable({ providedIn: 'root' })
export class AccountDeletionService {

  constructor(
    private http: HttpService,
    private alertCtrl: AlertController,
    private loadingCtrl: LoadingController,
  ) { }

  /** 입력값이 확인 문구와 같은지 (대소문자·앞뒤 공백 무시). 순수 함수 */
  static isConfirmed(input: unknown): boolean {
    return typeof input === 'string' && input.trim().toUpperCase() === DELETE_CONFIRM_WORD;
  }

  /** Firebase 사용자 정보에 Apple 로그인이 있는지. 순수 함수 */
  static usesApple(user: { providerData?: Array<{ providerId?: string }> } | null | undefined): boolean {
    return !!user && Array.isArray(user.providerData) && user.providerData.some(p => p && p.providerId === 'apple.com');
  }

  /**
   * 확인부터 삭제까지. 삭제됐으면 true (화면이 로그인 상태를 비운다), 취소·실패면 false
   */
  async confirmAndDelete(starId: string): Promise<boolean> {
    const starToken = localStorage.getItem('starToken') || '';
    if (!starId || !starToken) {
      await this.notice('Please log in again, then try deleting your account.');
      return false;
    }

    if (!(await this.askConfirmation())) return false;

    // iOS: Apple 로그인 계정이면 토큰 해지 (사용자가 Apple 창을 닫으면 중단)
    const revoke = await this.revokeAppleIfNeeded();
    if (revoke === 'cancelled') return false;

    const loading = await this.loadingCtrl.create({ message: 'Deleting your account...' });
    await loading.present();
    let res: any = null;
    try {
      res = await this.http.post('/api/super/star/account/delete', { starId, starToken }).toPromise();
    } catch (e) {
      console.error('[AccountDeletion] request failed', e);
    } finally {
      await loading.dismiss();
    }

    if (!res || res.result !== 'OK') {
      await this.notice((res && res.msg) || 'Failed to delete your account. Please check your connection and try again.');
      return false;
    }

    this.clearLocalLogin();
    try {
      await FirebaseAuthentication.signOut();
    } catch { /* 이미 서버에서 Firebase 계정이 지워졌을 수 있다 */ }

    await this.notice('Your account has been deleted. Your page is no longer linked to you, and your posts and messages have been removed.');
    return true;
  }

  private async askConfirmation(): Promise<boolean> {
    const alert = await this.alertCtrl.create({
      header: 'Delete Account',
      message:
        'This permanently deletes your account: your posts, messages, profile photo, bio and login information. ' +
        'Your Star Page will be released and you will be logged out. This cannot be undone.<br><br>' +
        `Type <b>${DELETE_CONFIRM_WORD}</b> to confirm.`,
      inputs: [{ name: 'confirm', type: 'text', placeholder: DELETE_CONFIRM_WORD }],
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        { text: 'Delete', role: 'destructive' },
      ],
    });
    await alert.present();
    const { data, role } = await alert.onDidDismiss();
    if (role !== 'destructive') return false;
    if (!AccountDeletionService.isConfirmed(data && data.values && data.values.confirm)) {
      await this.notice(`Please type ${DELETE_CONFIRM_WORD} exactly to delete your account.`);
      return false;
    }
    return true;
  }

  /** 'revoked' | 'skipped'(Apple 아님·iOS 아님·해지 실패) | 'cancelled'(사용자가 Apple 창을 닫음) */
  private async revokeAppleIfNeeded(): Promise<'revoked' | 'skipped' | 'cancelled'> {
    if (Capacitor.getPlatform() !== 'ios') return 'skipped';
    try {
      const { user } = await FirebaseAuthentication.getCurrentUser();
      if (!AccountDeletionService.usesApple(user)) return 'skipped';
    } catch {
      return 'skipped';
    }

    let code: string | undefined;
    try {
      const result = await FirebaseAuthentication.signInWithApple();
      code = result && result.credential ? result.credential.authorizationCode : undefined;
    } catch (e: any) {
      const message = String((e && (e.message || e.code)) || e || '');
      if (/cancel|1001/i.test(message)) return 'cancelled'; // ASAuthorizationError.canceled = 1001
      console.warn('[AccountDeletion] Apple re-auth failed', e);
      return 'skipped';
    }
    if (!code) return 'skipped';

    try {
      await FirebaseAuthentication.revokeAccessToken({ token: code });
      return 'revoked';
    } catch (e) {
      console.warn('[AccountDeletion] Apple token revoke failed', e);
      return 'skipped';
    }
  }

  private clearLocalLogin(): void {
    ['isStar', 'starId', 'starToken', 'starPw', 'ownerEmail', 'fcmToken'].forEach(key => localStorage.removeItem(key));
  }

  private async notice(message: string): Promise<void> {
    const alert = await this.alertCtrl.create({ header: 'Notice', message, buttons: ['OK'] });
    await alert.present();
    await alert.onDidDismiss();
  }
}
