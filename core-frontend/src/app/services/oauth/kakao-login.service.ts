import { Injectable } from '@angular/core';
import { KAKAO_API } from '../../constants/Keys';
import type { KTUser, UserInfo } from '../../types/Auth';
import { KakaoLoginPlugin } from 'capacitor-kakao-login-plugin';

@Injectable({
  providedIn: 'root',
})
export class KakaoLoginService {
  constructor() {}

  async ready(): Promise<boolean> {
    return true;
    // try {
      // await KakaoLoginPlugin.initializeKakao({
      //   webKey: KAKAO_API.WEB_KEY,
      //   appKey: KAKAO_API.APP_KEY,
      // });
    //   return true;
    // } catch (error) {
    //   console.error(`[KakaoAuth] initializeFailed - ${error.message}`);
    //   return false;
    // }
  }

  async login(): Promise<UserInfo> {
    try {
      const { accessToken } = await KakaoLoginPlugin.goLogin();
      const userInfo = await this.getUserInfo();
      if (!userInfo) {
        throw new Error('No UserData');
      }

      const MEM_TYPE = 'KT';
      const MEM_NAME = userInfo.properties.nickname;
      const ktUser: KTUser = {
        KT_KEY: String(userInfo.id),
        KT_TOKEN: accessToken,
        KT_TOKEN_EXPD: '',
        KT_PICTURE: userInfo.properties.profileImage || userInfo.properties.profile_image,
      };

      return { MEM_TYPE, MEM_NAME, ktUser };
    } catch (error) {
      console.error(`[KakaoAuth] loginFailed - ${error.message}`);
      return null;
    }
  }

  async logout(): Promise<boolean> {
    try {
      KakaoLoginPlugin.goLogout();
      return true;
    } catch (error) {
      console.error(`[KakaoAuth] logoutFailed - ${error.message}`);
      return false;
    }
  }

  async getUserInfo(): Promise<any> {
    try {
      const { value: userInfo } = await KakaoLoginPlugin.getUserInfo();
      if (!userInfo) {
        throw new Error('No UserData');
      }
      return userInfo;
    } catch (error) {
      console.error(`[KakaoAuth] getUserInfoFailed - ${error.message}`);
      return null;
    }
  }
}
