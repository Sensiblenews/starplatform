import { WriteModalPage } from 'src/app/modals/write-modal/write-modal.page';
import { HelperService } from './../../services/helper.service';
import { AuthService } from './../../services/auth.service';
import { Component } from '@angular/core';
import { ModifyNicknameInfo, UserInfo, ModifyProfileImgRequest, ModifyIntroductionInfo } from 'src/app/types/Auth';

import { ContentService } from 'src/app/services/content.service';
import { StorageService } from './../../services/storage.service';
import { NotificationsService } from './../../services/notifications.service';

import { Router } from '@angular/router';
import { NavController } from '@ionic/angular';
import { ContentManagerService } from 'src/app/services/content-manager.service';
import { PhotoService } from './../../services/photo.service';
import { HttpService } from 'src/app/services/http.service';
import { SubscriptionListModalPage } from './modal/subscription-list-modal.page';

@Component({
  selector: 'app-my-page',
  templateUrl: './my-page.page.html',
  styleUrls: ['./my-page.page.scss'],
})
export class MyPagePage {
  public setAlarm: boolean;
  public uInfo: UserInfo = null;
  public isAuthed: boolean;
  public loaded = false;
  public contentList = [];
  constructor(
    private storage: StorageService,
    private notification: NotificationsService,
    private auth: AuthService,
    private helper: HelperService,
    private contentService: ContentService,
    private cm: ContentManagerService,
    public router: Router,
    private photo: PhotoService,
    private http: HttpService,
  ) {
    this.initAlarmCheck();

    this.subscribeUserInfo();
  }

  ionViewDidEnter(){
    this.updateUserInfo();
  }

  // 유저정보 업데이트
  async updateUserInfo(){
    const MEM_TOKEN = await this.storage.get('authorization');
    if (!MEM_TOKEN) {
      return false;
    }
    const { item: userInfo, RESULT } = await this.auth.requestUserByToken(
      MEM_TOKEN,
    );
    if (RESULT === 'FAIL') {
      return false;
    }

    this.auth.setUser(userInfo);
  }

  initAlarmCheck(){
    console.log('init alarm check');
    this.storage.get('agreed_push').then((agreed)=>{
      if(agreed === 'yes'){
        this.setAlarm = true;
      }
      else{
        this.setAlarm = false;
      }
    });
  }

  alarmCheck(value){
    if(value){
      this.storage.set('agreed_push', 'yes');
      this.notification.registerPush();
    }
    else{
      this.storage.set('agreed_push', 'no');
      this.notification.unregisterPush();
    }
  }

  subscribeUserInfo() {
    this.auth.getUserAsObservable().subscribe((user) => {
      console.log(user);
      this.uInfo = user;
    });
    this.auth.isAuthedAsObservable().subscribe((authed) => {
      this.isAuthed = authed;
      if (!authed) {
        this.contentList.length = 0;
        this.loaded = true;
        return;
      }
      if (authed && !this.contentList.length) {
        this.updateContentList();
      }
    });
    this.cm.changed.subscribe(({ action, content }) => {
      if (!['changed', 'created', 'deleted'].includes[action]) {
        return;
      }
      this.updateContentList();
    });
  }

  updateContentList() {
    this.loaded = false;
    this.contentService.getMyContents().subscribe((a: any) => {
      this.contentList = a.contents;
      this.loaded = true;
    });
  }

  async login() {
    await this.auth.checkAuthState();
    // await this.auth.login('AP');
  }

  async logOut() {
    try {
      const alert = await this.helper.alert({
        cssClass: 'my-custom-class',
        backdropDismiss: true,
        header: '로그아웃',
        message: `
          로그인을 해제 하시겠습니까? <br>
          자동로그인도 같이 해제됩니다.
        `,
        buttons: [
          {
            text: '아니오',
            role: 'cancel',
            cssClass: 'secondary',
          },
          {
            text: '예',
            role: 'confirm',
          },
        ],
      });

      await alert.present();
      const { role } = await alert.onDidDismiss();
      if (role !== 'confirm') {
        return;
      }

      await this.helper.loading();
      await this.auth.logout();
      this.helper.toast('로그아웃 되었습니다.', 'middle');
    } catch (error) {
      console.log('logout failed - ', error);
    } finally {
      await this.helper.loadEnd();
    }
  }

  async signOut() {
    try {
      const alert = await this.helper.alert({
        cssClass: 'my-custom-class',
        backdropDismiss: true,
        header: '회원탈퇴',
        message: `
          정말로 회원탈퇴를 하시겠습니까?<br/>
          회원님의 컨텐츠와 포인트가<br/>
          모두 삭제되며 복구할 수 없습니다.<br/><br/>
          회원 탈퇴를 원하시면 <br/>
          <b>${this.uInfo.MEM_NAME}</b>을 입력 해주세요.
        `,
        inputs: [
          {
            name: 'MEM_NAME',
            type: 'text',
            placeholder: this.uInfo.MEM_NAME,
          },
        ],
        buttons: [
          {
            text: '아니오',
            role: 'cancel',
            cssClass: 'secondary',
          },
          {
            text: '예',
            role: 'confirm',
            handler: ({ MEM_NAME }) => {
              if (this.uInfo.MEM_NAME !== MEM_NAME) {
                this.helper.toast('탈퇴 문구를 다시 입력해 주세요.', 'middle');
                return false;
              }
            },
          },
        ],
      });

      await alert.present();
      const { role, data } = await alert.onDidDismiss();
      if (role !== 'confirm') {
        return;
      }

      await this.helper.loading();
      if (data.values?.MEM_NAME === this.uInfo.MEM_NAME) {
        // 탈퇴처리
        this.auth.signOut();
        // this.helper.toast('탈퇴 기능은 아직 준비중입니다.', 'middle');
      }
    } catch (error) {
      console.log('logout failed - ', error);
    } finally {
      await this.helper.loadEnd();
    }
  }

  async doWrite() {
    const modal = await this.helper.callModal({
      component: WriteModalPage,
    });
    return await modal.present();
  }

  async modifyNicknameConfirm() {
    try{
      var nickname = ''
      const alert = await this.helper.alert({
        cssClass: 'my-custom-class',
        backdropDismiss: true,
        header: '닉네임 설정',
        message: `
          닉네임을 설정하시겠습니까? <br/>
          비속어나 타인에게 불쾌감을 주는 단어를 사용하면 계정이 정지될 수도 있습니다.
        `,
        inputs: [
          {
            name: 'MEM_NAME',
            type: 'text',
          },
        ],
        buttons: [
          {
            text: '아니오',
            role: 'cancel',
            cssClass: 'secondary',
          },
          {
            text: '예',
            role: 'confirm',
            handler: ({ MEM_NAME }) => {
              if (MEM_NAME === "") {
                this.helper.toast('닉네임을 입력해주세요.', 'middle');
                return false;
              }

              nickname = MEM_NAME;
            },
          },
        ],
      });

      await alert.present();
      const { role, data } = await alert.onDidDismiss();
      if (role !== 'confirm') {
        return;
      }

      await this.helper.loading();
      const modifyBody: ModifyNicknameInfo = {
        MEM_ID: this.uInfo.MEM_ID,
        MEM_NAME: nickname
      };
      await this.auth.modifyNickname(modifyBody);

      // 닉네임 변경후 재로그인처리하여 닉네임 업데이트
      const MEM_TOKEN = await this.storage.get('authorization');
      if (!MEM_TOKEN) {
        return false;
      }
      const { item: userInfo, RESULT } = await this.auth.requestUserByToken(
        MEM_TOKEN,
      );
      if (RESULT === 'FAIL') {
        return false;
      }

      this.auth.setUser(userInfo);

      this.helper.toast('닉네임 변경을 완료했습니다.', 'middle');
    }
    catch (error) {
      console.log('modify Nickname failed - ', error);
    } finally {
      await this.helper.loadEnd();
    }
  }

  async modifyIntroductionConfirm() {
    try{
      let introduction = ''
      const alert = await this.helper.alert({
        cssClass: 'my-custom-class',
        backdropDismiss: true,
        header: '자기소개 설정',
        message: `
          자기소개를 설정하시겠습니까? <br/>
          비속어나 타인에게 불쾌감을 주는 단어를 사용하면 계정이 정지될 수도 있습니다. <br/>
          최대 20자까지 설정 가능합니다.
        `,
        inputs: [
          {
            name: 'MEM_INTRODUCTION',
            type: 'text',
          },
        ],
        buttons: [
          {
            text: '아니오',
            role: 'cancel',
            cssClass: 'secondary',
          },
          {
            text: '예',
            role: 'confirm',
            handler: ({ MEM_INTRODUCTION }) => {
              if (MEM_INTRODUCTION === "") {
                this.helper.toast('닉네임을 입력해주세요.', 'middle');
                return false;
              }

              introduction = MEM_INTRODUCTION;
            },
          },
        ],
      });

      await alert.present();
      const { role, data } = await alert.onDidDismiss();
      if (role !== 'confirm') {
        return;
      }

      await this.helper.loading();
      const modifyBody: ModifyIntroductionInfo = {
        MEM_ID: this.uInfo.MEM_ID,
        MEM_INTRODUCTION: introduction.substring(0, 20)
      };
      await this.auth.modifyIntroduction(modifyBody);

      // 닉네임 변경후 재로그인처리하여 닉네임 업데이트
      const MEM_TOKEN = await this.storage.get('authorization');
      if (!MEM_TOKEN) {
        return false;
      }
      const { item: userInfo, RESULT } = await this.auth.requestUserByToken(
        MEM_TOKEN,
      );
      if (RESULT === 'FAIL') {
        return false;
      }

      this.auth.setUser(userInfo);

      this.helper.toast('자기소개 변경을 완료했습니다.', 'middle');
    }
    catch (error) {
      console.log('modify Nickname failed - ', error);
    } finally {
      await this.helper.loadEnd();
    }
  }

  goDetail(contentType: number) {
    this.router.navigate(['/contents', contentType]);
  }

  // 팔로워 목록
  showFollowers() {
    const memInfo = this.uInfo;
    const mode = 1;
    const me = 1;

    this.router.navigate(['followers'], {
      state: { memInfo, mode, me },
    });
  }

  // 게시글 조회자 목록
  showViewers(event, item) {
    event.stopPropagation();
    event.preventDefault();

    const memInfo = this.uInfo;
    const conId = item.CON_ID;

    this.router.navigate(['viewers'],{
      state: {memInfo, conId},
    });
  }

  // 팔로잉 목록
  showFollowings() {
    const memInfo = this.uInfo;
    const mode = 2;
    const me = 1;

    this.router.navigate(['followers'], {
      state: { memInfo, mode, me },
    });
  }

  // 프로필 이미지 수정
  async modifyProfileImage(){
    try {
      const { base64String, format } = await this.photo.getPhotos();

      const alert = await this.helper.alert({
        header: '확인',
        subHeader: '프로필 이미지를 변경하시겠습니까?',
        buttons: [
          { text: '아니요', role: 'cancel' },
          { text: '예', role: 'change' },
        ],
      });
      alert.present();
      const { role } = await alert.onDidDismiss();
      if ('change' !== role) {
        return;
      }

      var imgWebPath = `data:image/${format};base64,${base64String}`;
      const body : ModifyProfileImgRequest = {
        MEM_ID: this.uInfo.MEM_ID,
        PROFILE_THUMNAIL: imgWebPath
      }
      await this.auth.modifyProfileImage(body);

      // 닉네임 변경후 재로그인처리하여 닉네임 업데이트
      const MEM_TOKEN = await this.storage.get('authorization');
      if (!MEM_TOKEN) {
        return false;
      }
      const { item: userInfo, RESULT } = await this.auth.requestUserByToken(
        MEM_TOKEN,
      );
      if (RESULT === 'FAIL') {
        return false;
      }

      this.auth.setUser(userInfo);

      this.helper.toast('프로필 이미지 변경을 완료했습니다.', 'middle');
    } catch (e) {
      if (e.name === 'permission') {
        this.helper.toast(e.message, 'middle');
      }
    }
  }

  formatNum(num) {
    if (num >= 1000000000) {
       return (num / 1000000000).toFixed(1).replace(/\.0$/, '') + 'G';
    }
    if (num >= 1000000) {
       return (num / 1000000).toFixed(1).replace(/\.0$/, '') + 'M';
    }
    if (num >= 10000) {
       return (num / 1000).toFixed(1).replace(/\.0$/, '') + 'K';
    }
    return num;
  }

  async onCheckHearts(event: Event) {
    event.stopPropagation();
    const user = this.auth.getUser();

    const url = `/api/heart/sender`;
    const data = {
      MEM_ID: user.MEM_ID
    }
    const result = await this.http.post<any>(url, data, {
      needToken: true,
    }).toPromise();
    const senders: any[] = result.HEART_SENDER;

    let message = `진한 하트: ${user.HEART_CHG}<br>진한 하트 액수: ${(user.HEART_CHG * 0.5).toLocaleString()}₩`;
    if (senders.length > 0) {
      message += '<br><br>하트 보낸 사용자<br>'
    }

    senders.forEach((d) => {
      message += `${d.MEM_NAME}님 ${d.CON_HEART_CHG}개<br>`;
    });

    const modal = await this.helper.alert({
      header: '하트 수',
      message,
      buttons: ['확인']
    });
    await modal.present();
    await modal.onDidDismiss();
  }

  async openSubscriptionManagement() {
    // 팝오버 닫기 (dismissOnSelect가 true지만 명시적으로 닫아주는 것이 안전할 수 있음)
    // await this.popoverCtrl.dismiss(); // PopoverController가 주입되어 있다면 사용

    const modal = await this.helper.callModal({
      component: SubscriptionListModalPage,
      // cssClass: 'my-custom-modal-class' 
    });
    return await modal.present();
  }
}
