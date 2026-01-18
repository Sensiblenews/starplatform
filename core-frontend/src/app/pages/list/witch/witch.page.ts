import { HelperService } from './../../../services/helper.service';
import { AuthService } from './../../../services/auth.service';
import { Component } from '@angular/core';
import { ContentService } from 'src/app/services/content.service';
import { WriteModalPage } from 'src/app/modals/write-modal/write-modal.page';
import { AdMobService } from 'src/app/services/ad-mob.service';
import { HttpService } from './../../../services/http.service';
import type { FollowSearchInfo, FollowInfo, FollowRequest, UnFollowRequest } from './../../../types/Auth';
import { BehaviorSubject, Observable } from 'rxjs';
import { ViewChild } from '@angular/core';
import { IonSearchbar } from '@ionic/angular';
import { UserInfo } from 'src/app/types/Auth';
import { Router } from '@angular/router';
import { ChatService } from '../../../services/chat.service';
import { ChatWriteModalPage } from './chat-write/chat-write-modal.page';

@Component({
  selector: 'app-witch',
  templateUrl: './witch.page.html',
  styleUrls: ['./witch.page.scss'],
})
export class WitchPage {
  @ViewChild('searchBar') searchBar: IonSearchbar;

  public pageTitle = '마녀';
  public menus: any[] = [];
  public menuIndex: number;
  public memInfo: UserInfo = null;
  public dataList: BehaviorSubject<FollowInfo[]> = new BehaviorSubject([]);
  public loaded = false;

  constructor(
    private auth: AuthService,
    private contentService: ContentService,
    private helper: HelperService,
    private http: HttpService,
    private router: Router,
    private admob: AdMobService,
    private chatService : ChatService
  ) {
    this.menus = ['팔로워', '팔로잉', '일반'].map((val)=>({
      title: val,
      selected: false
    }));
  }

  ionViewDidEnter(){
    this.memInfo = this.auth.getUser();

    setTimeout(()=>{
      this.selectMenu(0);
    },500);
  }

  public get isAuthed(): boolean {
    return this.auth.isAuthed();
  }

  public get itemSize(): number {
    return 80;
  }

  public get itemCount(): number {
    return 10;
  }

  public get followerList(): Observable<FollowInfo[]>{
    return this.dataList.asObservable();
  }

  searchByKeyword(event: KeyboardEvent): boolean {
    if(event.key === 'Enter'){
      const target = event.target as HTMLIonSearchbarElement;
      const keyword = String(target.value);

      switch(this.menuIndex){
        case 0:
          this.loadFollowers(keyword);
          break;
        case 1:
          this.loadFollowings(keyword);
          break;
        case 2:
          this.loadGeneral(keyword);
          break;
      }

      return true;
    }
    return false;
  }

  getFollowCount(){
    const url = `/api/witchCount`;
    const req = {};

    this.http.post<object[]>(url, req, {
      needToken: true,
    }).subscribe((f)=>{
      this.updateFollowCount(f);
    });
  }

  updateFollowCount(count){
    this.menus[0].title = '팔로워 ' + this.formatNum(count.FOLLOWER_COUNT);
    this.menus[1].title = '팔로잉 ' + this.formatNum(count.FOLLOWING_COUNT);
  }

  async selectMenu(i){
    this.menuIndex = i;
    this.loaded = false;
    this.searchBar.value = null;

    this.menus.map((item)=>(item.selected = false));
    this.menus[i].selected = true;

    switch(i){
      case 0:
        this.loadFollowers('');
        break;
      case 1:
        this.loadFollowings('');
        break;
      case 2:
        this.loadGeneral('');
        break;
    }

    await this.admob.showInterstitial();
  }

  loadFollowers(keyword){
    this.getFollowCount();

    const req: FollowSearchInfo = {
      KEYWORD: keyword,
      PROFILE_ID: this.auth.getUser().MEM_ID,
    };
    const url = `/api/followersSearch`;
    return this.http.post<object[]>(url, req, {
      needToken: true,
    }).subscribe((f)=>{
      this.dataList.next(f);
      this.loaded = true;
    });
  }

  loadFollowings(keyword){
    this.getFollowCount();

    const req: FollowSearchInfo = {
      KEYWORD: keyword,
      PROFILE_ID: this.auth.getUser().MEM_ID,
    };
    const url = `/api/followsSearch`;
    return this.http.post<object[]>(url, req, {
      needToken: true,
    }).subscribe((f)=>{
      this.dataList.next(f);
      this.loaded = true;
    });
  }

  loadGeneral(keyword){
    this.getFollowCount();

    const req: FollowSearchInfo = {
      KEYWORD: keyword,
    };
    const url = `/api/goblinPrsList`;
    return this.http.post<object[]>(url, req, {
      needToken: true,
    }).subscribe((f)=>{
      this.dataList.next(f);
      this.loaded = true;
    });

  }

  follow(event,item){
    this.followUser(this.memInfo.MEM_ID, item.FOLLOW_ID).subscribe((o) => {
      switch(this.menuIndex){
        case 0:
          this.loadFollowers(this.searchBar.value);
          break;
        case 1:
          this.loadFollowings(this.searchBar.value);
          break;
        case 2:
          this.loadGeneral(this.searchBar.value);
          break;
      }
    });
  }

  followUser(memId, followId){
    const followRequest: FollowRequest = {
      MEM_ID: memId,
      FOLLOW_ID: followId,
    };

    const url = `/api/followUser`;
    return this.http.post<object[]>(url, followRequest, {
      needToken: true,
    });
  }

  /**
   * 쪽지 모달 창 열기
   */
  async chatOpen(item:any) {
    console.log(item);
    await this.doChatWrite(item);

    // 닫히면 다시 로드
    if (this.menus[0].selected) {
      this.loadFollowers(this.searchBar.value);
    } else if (this.menus[1].selected) {
      this.loadFollowings(this.searchBar.value);
    } else if (this.menus[2].selected) {
      this.loadGeneral(this.searchBar.value);
    }

    // const confirmModal = await this.helper.alert({
    //   header: '',
    //   subHeader: '쪽지 선택',
    //   buttons: [
    //     { text: '보내기', role: 'send' },
    //     { text: '보기', role: 'view' },
    //   ],
    // });

    // confirmModal.present();

    // const { role } = await confirmModal.onDidDismiss();

    // if(role === 'send') {
    //   await this.doChatWrite(item);
    // } else {
    //   // 변수명을 반대로 지음 [ CHAT_COUNT : 신규 채팅 건 수 / NEW_CHAT_COUNT : 전체 채팅 건 수 ]
    //   if(item.CHAT_COUNT === 0){
    //     const alert = await this.helper.alert({
    //       header: '',
    //       subHeader: '발송된 쪽지가 없습니다.',
    //       buttons: [
    //         { text: '예', role: 'delete' },
    //       ],
    //     });

    //     alert.present();

    //     return false;
    //   }

    //   let parentFunc = undefined;
    //   switch (this.menuIndex) {
    //     case 0:
    //       parentFunc = () => this.loadFollowers(''); // 함수 참조 전달
    //       break;
    //     case 1:
    //       parentFunc = () => this.loadFollowings('');
    //       break;
    //     case 2:
    //       parentFunc = () => this.loadGeneral('');
    //       break;
    //   }

    //   await this.chatService.openChatModal(item.FOLLOW_ID,item.FOLLOW_PICTURE,item.FOLLOW_NAME,parentFunc);
    // }
  }

  async doChatWrite(item:any) {
    const modal = await this.helper.callModal({
      component: ChatWriteModalPage,
      componentProps : {followInfo : item}
    });
    await modal.present();
    return await modal.onDidDismiss();
  }

  async unFollow(event,item){
    // 팝업은 마이페이지 통해 접속할 경우만 띄움
    const alert = await this.helper.alert({
      header: '',
      subHeader: '목록에서 삭제하시겠습니까?',
      buttons: [
        { text: '아니요', role: 'cancel' },
        { text: '예', role: 'delete' },
      ],
    });
    alert.present();
    const { role } = await alert.onDidDismiss();
    if ('delete' !== role) {
      return;
    }

    this.unFollowUser(this.memInfo.MEM_ID, item.FOLLOW_ID).subscribe((o) => {
      switch(this.menuIndex){
        case 0:
          this.loadFollowers(this.searchBar.value);
          break;
        case 1:
          this.loadFollowings(this.searchBar.value);
          break;
        case 2:
          this.loadGeneral(this.searchBar.value);
          break;
      }
    });
  }

  unFollowUser(memId, followId){
    const unFollowRequest: UnFollowRequest = {
      MEM_ID: memId,
      FOLLOW_ID: followId,
    };

    const url = `/api/unFollowUser`;
    return this.http.post<object[]>(url, unFollowRequest, {
      needToken: true,
    });
  }

  profilePage(item){
    const prsId = item.FOLLOW_ID;
    const memName = item.FOLLOW_NAME;
    const pfp = item.FOLLOW_PICTURE;

    this.router.navigate(['profile-page'], {
      state: { prsId, memName },
    });
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

  async doWrite() {
    const modal = await this.helper.callModal({
      component: WriteModalPage,
    });

    return await modal.present();
  }
}
