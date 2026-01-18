import { TermsAgreementPage } from './../modals/terms-agreement/terms-agreement.page';
import { AppleLoginService } from './oauth/apple-login.service';
import { LoginModalPage } from './../modals/login-modal/login-modal.page';
import { HelperService } from './helper.service';
import { HttpService } from './http.service';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import type {
  UserInfo,
  AuthType,
  ModifyNicknameInfo,
  ModifyNicknameResponse,
  ModifyProfileImgRequest,
  ModifyIntroductionInfo,
  ModifyIntroductionResponse
} from '../types/Auth';
import { KakaoLoginService } from './oauth/kakao-login.service';
import { EmailLoginService } from './oauth/email-login.service';
import { GoogleLoginService } from './oauth/google-login.service';
import { ItemCountPopoverComponent } from '../components/item-count-popover/item-count-popover.component';
import { StorageService } from './storage.service';
import { ContentResponse } from '../types/Content';
import { TermsBigModal } from '../modals/terms-big/terms-big.component';
import { TermsSmallModal } from '../modals/terms-small/terms-small.component';
import { FCM } from '@capacitor-community/fcm';
import { CheckMessageService } from './check-message.service';
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private inloginProcess: boolean;
  private authed: BehaviorSubject<boolean> = new BehaviorSubject(false);
  private defaultProfile = '../../../assets/img/defaultImg/avatar.svg';
  private guestUser: UserInfo = {
    MEM_ID: null,
    MEM_NAME: 'guest',
    MEM_PICTURE: this.defaultProfile,
  };
  private userInfo: BehaviorSubject<UserInfo> = new BehaviorSubject(
    this.guestUser,
  );
  constructor(
    private http: HttpService,
    private kakaoLogin: KakaoLoginService,
    private emailLogin: EmailLoginService,
    private appleLogin: AppleLoginService,
    private googleLogin: GoogleLoginService,
    private helper: HelperService,
    private storage: StorageService,
  ) {}

  isAuthed() {
    return this.authed.value;
  }

  getUser() {
    return this.userInfo.value;
  }

  getUserProperty(prop: keyof UserInfo): UserInfo[keyof UserInfo] | null {
    const user = this.getUser();
    return prop in user ? user[prop] : null;
  }

  updateUserProperty(newUserInfo: UserInfo): void {
    const user = this.getUser();
    this.userInfo.next({ ...user, ...newUserInfo });
  }

  checkUserProperty(
    prop: keyof UserInfo,
    compareValue: UserInfo[keyof UserInfo],
  ): boolean {
    const currentValue = this.getUserProperty(prop);
    return currentValue && currentValue === compareValue;
  }

  getUserAsObservable(): Observable<UserInfo | null> {
    return this.userInfo.asObservable();
  }

  isAuthedAsObservable(): Observable<boolean> {
    return this.authed.asObservable();
  }

  async showUserCount(): Promise<void> {
    const {
      MEM_STAR = 0,
      MEM_STAR_CHG = 0,
      PRICE = 0,
      PRICE_CHG = 0,
    } = this.getUser();

    const countInfo = {
      type: 'star',
      free: MEM_STAR,
      charged: MEM_STAR_CHG,
      earning: PRICE + PRICE_CHG,
    };

    this.helper.popOver({
      component: ItemCountPopoverComponent,
      componentProps: { countInfo },
    });
  }

  async setUser(userInfo: UserInfo): Promise<boolean> {
    if (userInfo.MEM_USE_YN === 0) {
      this.helper.alert({
        header: '정지 안내입니다.',
        subHeader: '규정 위반으로 사용이 정지됐습니다.',
      });
      return false;
    }

    this.userInfo.next(userInfo);
    this.http.setUserToken(userInfo.MEM_TOKEN);

    const termsAgree = await this.checkUserAgreements(
      userInfo.MEM_AGREEMENT || 0,
    );

    if (!termsAgree) {
      await this.removeUser();
      return false;
    }

    if (!userInfo.MEM_PICTURE?.length) {
      userInfo.MEM_PICTURE = `${this.defaultProfile}`;
    }

    await this.storage.set('authorization', userInfo.MEM_TOKEN);
    this.authed.next(true);
    return true;
  }

  async removeUser(): Promise<void> {
    this.userInfo.next(this.guestUser);
    this.http.setUserToken('');
    await Promise.all([
      this.storage.remove('authorization'),
      this.storage.remove('lastPopup'),
      this.storage.remove('applied_event'),
      this.storage.remove('freeChargedDate'),
    ]);
    this.authed.next(false);
  }

  async initialize() {
    await this.kakaoLogin.ready();
    await this.emailLogin.ready();
    await this.googleLogin.ready();
  }

  getTermsAgreementsPolicy(index: number = 0): Promise<ContentResponse> {
    const url = `/app/policyDetail?index=${index}`;
    return this.http.post<ContentResponse>(url, null).toPromise();
  }

  putUserAgreementPolicy(id: string): Promise<UserInfo> {
    const url = `/api/user?MEM_AGREEMENT=1`;
    return this.http
      .put<UserInfo>(url, { id }, { needToken: true })
      .toPromise();
  }

  async checkUserAgreements(userAgreement: number): Promise<boolean> {
    try {
      if (userAgreement !== 0) {
        return true;
      }

      const termsCount = [0, 1];
      const termsRequest = termsCount.map((idx) =>
        this.getTermsAgreementsPolicy(idx),
      );

      const termsResponses = await Promise.all(termsRequest);

      const receivedTerms = termsResponses.filter(
        (term) => (term.result as { isSuccess: number }).isSuccess === 1,
      );

      if (receivedTerms.length !== termsCount.length) {
        console.error('약관을 받아오지 못함');
        return false;
      }

      const agreement = await this.helper.callModal({
        component: TermsSmallModal,
        componentProps: {
          terms: receivedTerms.map((terms) => terms.contents),
        },
        cssClass: 'loginView',
      });

      agreement.present();
      const { data } = await agreement.onDidDismiss();

      if (!data) {
        return false;
      }

      const MEM_ID = this.getUserProperty('MEM_ID') as string;
      // const MEM_ID = "2022092900004"
      const receivedUserInfo = await this.putUserAgreementPolicy(MEM_ID);
      this.updateUserProperty(receivedUserInfo);

      const welcome = await this.helper.alert({
        header: '환영합니다!',
        message: '별 10개를 선물로 드립니다.',
      });

      await welcome.present();

      return true;
    } catch (error) {
      console.warn('terms error', error);
      return false;
    }
  }

  async login(authType: AuthType): Promise<boolean> {
    try {

      if (this.isAuthed()) {
        return null;
      }

      let userInfo: UserInfo;

      // 테스트 로그인
      // userInfo = {
      //   MEM_ID : '2024012700001',
      //   MEM_TYPE : 'AP',
      //   MEM_NAME : '터미네이터',
      //   MEM_STAR : 960,
      //   MEM_STAR_CHG : 0,
      //   MEM_TOKEN : '000499.913cf59eaab043d6974e99311dbf9f57.2132',
      //   MEM_AGREEMENT : 1,
      //   MEM_USE_YN : 1,
      //   MEM_PICTURE : null,
      //   PRICE : 23,
      //   PRICE_CHG :4,
      //   CONTENT_CNT : 25,
      //   FOLLOWING_CNT : 1,
      //   FOLLOWER_CNT : 4,
      // }

      switch (authType) {
        case 'KT':
          userInfo = await this.kakaoLogin.login();
          break;
        case 'FB':
          userInfo = await this.emailLogin.login();
          break;
        case 'AP':
          userInfo = await this.appleLogin.login();
          break;
        case 'GO':
          userInfo = await this.googleLogin.login();
          break;
      }

      if (!userInfo) {
        throw new Error('no UserInfo');
      }

      const authedUser: UserInfo = await this.postUserInfo(userInfo);
      return await this.setUser(authedUser);
    } catch (error) {
      console.error(`[Auth] loginFailed - ${error.message}`);
      return false;
    }
  }

  async tryAutoLogin(): Promise<boolean> {
    try {
      if (this.inloginProcess) {
        return false;
      }
      this.inloginProcess = true;
      const MEM_TOKEN = await this.storage.get('authorization');
      if (!MEM_TOKEN) {
        return false;
      }
      const { item: userInfo, RESULT } = await this.requestUserByToken(
        MEM_TOKEN,
      );
      if (RESULT === 'FAIL') {
        return false;
      }
      console.log('userinfo');
      console.log(userInfo);
      return await this.setUser(userInfo);
    } catch (error) {
      return false;
    } finally {
      this.inloginProcess = false;
    }
  }

  async logout(): Promise<boolean> {
    try {
      if (!this.isAuthed()) {
        throw new Error('User not authenticated');
      }

      const userInfo: UserInfo = this.getUser();
      if (!userInfo) {
        throw new Error('Userinfo not exist');
      }

      let logoutFlag = false;
      switch (userInfo.MEM_TYPE) {
        case 'KT':
          logoutFlag = await this.kakaoLogin.logout();
          break;
        case 'FB':
          logoutFlag = await this.emailLogin.logout();
          break;
        case 'AP':
          logoutFlag = await this.appleLogin.logout();
          break;
        case 'GO':
          logoutFlag = await this.googleLogin.logout();
          break;
      }
      if (!logoutFlag) {
        return false;
      }
      await this.removeUser();
      return true;
    } catch (error) {
      console.error(`[Auth Service] LogoutFailed - ${error.message}`);
      return false;
    }
  }

  async signOut(): Promise<boolean> {
    const MEM_ID = this.getUserProperty('MEM_ID') as string;
    console.log(await this.userSignOut(MEM_ID));
    return this.logout();
  }

  async postUserInfo(user: UserInfo): Promise<UserInfo> {
    const url = `/app/userJoin`;
    let fcmToken = null;

    try {
      const { token } = await FCM.getToken();
      fcmToken = token;
    } catch(error) {
      console.error('FCM 토큰을 가져오는 데 실패했습니다.', error);
    }

    user = { ...user, FCM_TOKEN: fcmToken };

    return this.http.post<UserInfo>(url, user).toPromise();
  }

  requestUserByToken(
    MEM_TOKEN: string,
  ): Promise<{ item?: UserInfo; RESULT: 'OK' | 'FAIL' }> {
    const url = `/app/user?MEM_TOKEN=${MEM_TOKEN}`;
    return this.http
      .post<{ item?: UserInfo; RESULT: 'OK' | 'FAIL' }>(url, null)
      .toPromise();
  }

  userSignOut(MEM_ID: string): Promise<UserInfo> {
    const url = `/api/user?MEM_ID=${MEM_ID}`;
    return this.http.delete<UserInfo>(url, { needToken: true }).toPromise();
  }

  modifyNickname(info: ModifyNicknameInfo): Promise<ModifyNicknameResponse>{
    const url = `/api/modifyNickname`;
    return this.http.post<ModifyNicknameResponse>(url, info, { needToken: true }).toPromise();
  }

  modifyIntroduction(info: ModifyIntroductionInfo): Promise<ModifyIntroductionResponse> {
    const url = '/api/modifyIntroduction';
    return this.http.post<ModifyIntroductionResponse>(url, info, { needToken: true }).toPromise();
  }

  modifyProfileImage(info: ModifyProfileImgRequest): Promise<UserInfo>{
    const url = `/api/modifyProfileImage`;
    return this.http.post<UserInfo>(url, info, {needToken: true}).toPromise();
  }

  async checkAuthState(): Promise<boolean> {
    if (this.isAuthed()) {
      return true;
    }

    if (await this.tryAutoLogin()) {
      return true;
    }

    const loginModal = await this.helper.callModal({
      component: LoginModalPage,
      backdropDismiss: true,
      showBackdrop: true,
      cssClass: 'loginView',
    });
    loginModal.present();
    const { data: authed } = await loginModal.onWillDismiss();
    return authed ?? false;
  }
}
