import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { NavController } from '@ionic/angular';
import { UserInfo } from 'src/app/types/Auth';
import type { ProfileInfo, FollowListRequest, FollowingListRequest, FollowInfo, FollowRequest, UnFollowRequest } from '../../types/Auth';
import { HttpService } from '../../services/http.service';
import { BehaviorSubject, Observable } from 'rxjs';
import { HelperService } from './../../services/helper.service';

@Component({
  selector: 'app-followers',
  templateUrl: './followers.page.html',
  styleUrls: ['./followers.page.scss'],
})
export class FollowersPage {
  public mode: number = null;
  public me: number = null;
  public memInfo: UserInfo = null;
  public profileInfo: ProfileInfo = null;
  public loaded = false;
  public dataList: BehaviorSubject<FollowInfo[]> = new BehaviorSubject([]);

  constructor(
    private navController: NavController,
    private router: Router,
    private helper: HelperService,
    private http: HttpService,
  ) {

  }

  ionViewDidEnter() {
    // const mode: number =
    //   this.router?.getCurrentNavigation()?.extras?.state?.mode || null;
    // const me: number =
    //   this.router?.getCurrentNavigation()?.extras?.state?.me || null;
    // const memInfo: UserInfo =
    //   this.router?.getCurrentNavigation()?.extras?.state?.memInfo || null;

    console.log(history);

    const mode: number = history.state.mode;
    const me: number = history.state.me;
    const memInfo: UserInfo = history.state.memInfo;

    this.me = me;
    this.mode =  mode;
    this.memInfo = memInfo;

    if(this.me === 2){
      const profileInfo: ProfileInfo = history.state.profileInfo;

      this.profileInfo = profileInfo;
    }

    // 팔로워리스트
    if(this.mode === 1){
      this.loadFollowersReq().subscribe((f)=>{
        this.dataList.next(f);
        this.loaded = true;
      });
    }
    // 팔로잉리스트
    else if(this.mode === 2){
      this.loadFollowingsReq().subscribe((f)=>{
        this.dataList.next(f);
        this.loaded = true;
      });
    }
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

  public get pageTitle(): string {
    if(this.mode === 1){
      return '팔로워 목록';
    }
    else if(this.mode === 2){
      return '팔로잉 목록';
    }
  }

  moveBack() {
    try {
      this.navController.back();
    } catch (e) {
      this.navController.navigateRoot('/');
    }
  }

  loadFollowersReq(){
    var req: FollowListRequest = null;
    if(this.me === 2){
      req = {
        MEM_ID: this.memInfo.MEM_ID,
        PROFILE_ID: this.profileInfo.MEM_ID,
      };
    }
    else if(this.me === 1){
      req = {
        MEM_ID: this.memInfo.MEM_ID,
        PROFILE_ID: this.memInfo.MEM_ID,
      };
    }

    const url = `/api/followers`;
    return this.http.post<object[]>(url, req, {
      needToken: true,
    });
  }

  loadFollowingsReq(){
    var req: FollowingListRequest = null;
    if(this.me === 2){
      req = {
        MEM_ID: this.memInfo.MEM_ID,
        PROFILE_ID: this.profileInfo.MEM_ID,
      };
    }
    else if(this.me === 1){
      req = {
        MEM_ID: this.memInfo.MEM_ID,
        PROFILE_ID: this.memInfo.MEM_ID,
      };
    }

    const url = `/api/follows`;
    return this.http.post<object[]>(url, req, {
      needToken: true,
    });
  }

  profilePage(item){
    const prsId = item.FOLLOW_ID;
    const memName = item.FOLLOW_NAME;

    console.log(item);

    this.router.navigate(['profile-page'], {
      state: { prsId, memName },
    });
  }

  follow(event,item){
    this.followUser(this.memInfo.MEM_ID, item.FOLLOW_ID).subscribe((o) => {
      // 팔로워리스트
      if(this.mode === 1){
        this.loadFollowersReq().subscribe((f)=>{
          this.dataList.next(f);
          this.loaded = true;
        });
      }
      // 팔로잉리스트
      else if(this.mode === 2){
        this.loadFollowingsReq().subscribe((f)=>{
          this.dataList.next(f);
          this.loaded = true;
        });
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


  async unFollow(event,item){
    // 팝업은 마이페이지 통해 접속할 경우만 띄움
    if(this.me === 1){
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
    }

    this.unFollowUser(this.memInfo.MEM_ID, item.FOLLOW_ID).subscribe((o) => {
      // 팔로워리스트
      if(this.mode === 1){
        this.loadFollowersReq().subscribe((f)=>{
          this.dataList.next(f);
          this.loaded = true;
        });
      }
      // 팔로잉리스트
      else if(this.mode === 2){
        this.loadFollowingsReq().subscribe((f)=>{
          this.dataList.next(f);
          this.loaded = true;
        });
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
}
