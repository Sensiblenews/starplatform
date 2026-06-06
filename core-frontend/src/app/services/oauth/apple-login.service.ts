import { Injectable } from '@angular/core';
import {
  SignInWithApple,
  SignInWithAppleResponse,
  SignInWithAppleOptions,
} from '@capacitor-community/apple-sign-in';
import { Capacitor } from '@capacitor/core';
import { APUser, UserInfo } from 'src/app/types/Auth';
@Injectable({
  providedIn: 'root',
})
export class AppleLoginService {
  constructor() {}

  async authorize(): Promise<any> {
    try {
      // 🌟 현재 실행 중인 기기가 iOS인지 확인 (분기 처리의 핵심)
      const platform = Capacitor.getPlatform();

      // 🌟 [Android Bypass] Show an English alert and exit gracefully
      if (platform === 'android') {
        alert('Apple Login is currently not supported on Android devices. Please use Google Login instead.');
        return null;
      }

      // Proceed with normal native plugin execution for iOS
      const options: SignInWithAppleOptions = {
        clientId: 'kr.co.sensiblenews.witchHunting',
        redirectURI: '',
        scopes: 'email name',
      };
      
      const { response: userInfo } = await SignInWithApple.authorize(options);

      if (!userInfo) {
        throw new Error('No UserData returned from Apple');
      }

      return { 
        apUser: userInfo, 
        email: userInfo.email || '' 
      };
    } catch (error) {
      console.error(`[Apple sign in] loginFailed - ${error.message}`);
      return null;
    }
  }

  async login(): Promise<UserInfo | null> {
    try {
      const userInfo = await this.authorize();
      console.log(userInfo);
      // identityToken을 보내야 하지만 서버에 user key로 처리되어 있어 맞춤
      if (!userInfo.user) {
        throw new Error('No User Identify');
      }

      const MEM_TYPE = 'AP';
      const MEM_NAME = `${userInfo.givenName}${userInfo.familyName}`;
      const apUser: APUser = {
        AP_KEY: String(userInfo.email),
        AP_TOKEN: userInfo.user,
        AP_TOKEN_EXPD: '',
        AP_PICTURE: '',
      };
      return { MEM_TYPE, MEM_NAME, apUser };
    } catch (error) {
      console.error(`[Apple sign in] loginFailed - ${error.message}`);
      return null;
    }
  }

  async logout(): Promise<boolean> {
    return true;
  }
}
