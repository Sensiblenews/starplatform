import { Injectable } from '@angular/core';
import { FirebaseAuthentication } from '@capacitor-firebase/authentication';

@Injectable({
  providedIn: 'root'
})
export class FirebaseAuthService {

  /**
   * 마지막 소셜 인증 실패 사유 (2-34 3차 1번). 실패해도 null 만 돌려줘 화면에 아무 반응이 없었다 —
   * 호출부가 사용자 취소인지 구분해 오류 코드를 보여 줄 수 있게 남긴다. 성공하면 비운다
   */
  lastError: { message: string; cancelled: boolean } | null = null;

  constructor() {}

  /** 플러그인 오류 → 메시지·취소 여부. Android GoogleSignIn 취소 코드는 12501 */
  private recordError(error: any): void {
    const message = String((error && (error.message || error.code)) || error || 'Unknown error');
    this.lastError = { message, cancelled: /cancel|12501/i.test(message) };
  }

  // 🔴 구글 로그인
  async signInWithGoogle(): Promise<{ email: string; uid: string } | null> {
    try {
      this.lastError = null;
      const result = await FirebaseAuthentication.signInWithGoogle();
      return { 
        email: result.user.email || '', 
        uid: result.user.uid 
      };
    } catch (error) {
      console.error('[Firebase] Google Auth Error:', error);
      this.recordError(error);
      return null;
    }
  }

  // 🍏 애플 로그인
  async signInWithApple(): Promise<{ email: string; uid: string } | null> {
    try {
      this.lastError = null;
      const result = await FirebaseAuthentication.signInWithApple();
      
      // 🌟 애플은 사용자가 이메일 가리기를 선택하면 email이 null로 올 수 있습니다.
      // 이 경우 파이어베이스 고유 uid를 활용해 임시 가상 이메일을 만들어 서버 에러를 방지합니다.
      const email = result.user.email || `${result.user.uid}@apple.starplatform.com`;
      
      return { email: email, uid: result.user.uid };
    } catch (error) {
      console.error('[Firebase] Apple Auth Error:', error);
      this.recordError(error);
      return null;
    }
  }

  // 로그아웃
  async signOut() {
    await FirebaseAuthentication.signOut();
  }
}