import { Injectable } from '@angular/core';
import {
  SignInWithApple,
  SignInWithAppleResponse,
  SignInWithAppleOptions,
} from '@capacitor-community/apple-sign-in';
import { APUser, UserInfo } from 'src/app/types/Auth';
@Injectable({
  providedIn: 'root',
})
export class AppleLoginService {
  constructor() {}

  async authorize(): Promise<SignInWithAppleResponse['response'] | null> {
    try {
      const options: SignInWithAppleOptions = {
        clientId: 'kr.co.sensiblenews.witchHunting',
        redirectURI: '',
        scopes: 'email name',
      };
      const { response: userInfo } = await SignInWithApple.authorize(options);

      if (!userInfo) {
        throw new Error('No UserData');
      }
      return userInfo;
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
