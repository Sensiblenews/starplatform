import { Injectable } from '@angular/core';
import { FirebaseAuthentication } from '@capacitor-firebase/authentication';

@Injectable({
  providedIn: 'root'
})
export class FirebaseAuthService {

  constructor() {}

  // 🔴 구글 로그인
  async signInWithGoogle(): Promise<{ email: string; uid: string } | null> {
    try {
      const result = await FirebaseAuthentication.signInWithGoogle();
      return { 
        email: result.user.email || '', 
        uid: result.user.uid 
      };
    } catch (error) {
      console.error('[Firebase] Google Auth Error:', error);
      return null;
    }
  }

  // 🍏 애플 로그인
  async signInWithApple(): Promise<{ email: string; uid: string } | null> {
    try {
      const result = await FirebaseAuthentication.signInWithApple();
      
      // 🌟 애플은 사용자가 이메일 가리기를 선택하면 email이 null로 올 수 있습니다.
      // 이 경우 파이어베이스 고유 uid를 활용해 임시 가상 이메일을 만들어 서버 에러를 방지합니다.
      const email = result.user.email || `${result.user.uid}@apple.starplatform.com`;
      
      return { email: email, uid: result.user.uid };
    } catch (error) {
      console.error('[Firebase] Apple Auth Error:', error);
      return null;
    }
  }

  // 로그아웃
  async signOut() {
    await FirebaseAuthentication.signOut();
  }
}