import { NavController } from '@ionic/angular';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { HttpService } from '../../services/http.service';
import type { ProfileInfo, ProfileRequest, FollowRequest, UnFollowRequest } from '../../types/Auth';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { StorageService } from './../../services/storage.service';

@Component({
  selector: 'app-profile-page',
  templateUrl: './profile-page.page.html',
  styleUrls: ['./profile-page.page.scss'],
})
export class ProfilePagePage {
  private prsId: string = null;
  private memName: string = null;
  private profileSubject: BehaviorSubject<ProfileInfo> = new BehaviorSubject(null);
  public profile: ProfileInfo = null;
  public loadedProfile: boolean;
  public loaded = false;
  public contentList = [];
  public contentCnt = null;
  public loadingMore = false;
  public page = 1;
  public pageSize = 10;
  public profilePicture = '';
  public isPress = false;

  constructor(
    private storage: StorageService,
    private navController: NavController,
    private router: Router,
    private http: HttpService,
    private auth: AuthService,
  ) { 
  }

  ionViewDidEnter() {

    const prsId: string = history.state.prsId;
    const memName: string = history.state.memName;
    const isNormalUser: string = history.state.isNormalUser;
    if (parseInt(isNormalUser) === 0) {
      this.isPress = true;
    }

    if (!prsId || !memName) {
      this.moveBack();
    }
    this.prsId = prsId;
    this.memName = memName;

    this.getProfileInfo().subscribe((profile) => {
      this.profileSubject.next(profile);
    });

    this.profileSubject.asObservable().subscribe((p)=>{
      this.profile = p;
      this.loadedProfile = true;

    });

    this.getProfileContents().subscribe((a: any) => {
      this.contentList = a.contents;
      this.contentCnt = a.contentsCount
      this.loaded = true;
    });
  }

  public get pageTitle(): string {
    return this.memName;
  }

  moveBack() {
    try {
      this.navController.back();
    } catch (e) {
      this.navController.navigateRoot('/');
    }
  }

  getProfileInfo(): Observable<ProfileInfo> {
     const profileRequest: ProfileRequest = {
      PROFILE_ID: this.prsId,
      MEM_ID: this.auth.getUserProperty('MEM_ID') as string
    };

    const url = `/app/profile`;
    return this.http.post<ProfileInfo>(url, profileRequest, {
      needToken: true
    });
  }

  getProfileContents(): Observable<Comment[]>{
    const profileRequest: ProfileRequest = {
      PROFILE_ID: this.prsId,
      MEM_ID: this.auth.getUserProperty('MEM_ID') as string,
      PAGE: this.page
    };

    const url = `/api/profileContents`;
    return this.http.post<Comment[]>(url, profileRequest, {
      needToken: true,
    });
  }

  goDetail(contentType: number) {
    this.router.navigate(['/contents', contentType]);
  }

  follow(){
    this.followUser().subscribe((o) => {
      this.getProfileInfo().subscribe((profile) => {
        this.profileSubject.next(profile);
      });    
    });
  }

  followUser(){
    const followRequest: FollowRequest = {
      MEM_ID: this.auth.getUserProperty('MEM_ID') as string,
      FOLLOW_ID: this.profile?.MEM_ID,
    };

    const url = `/api/followUser`;
    return this.http.post<object[]>(url, followRequest, {
      needToken: true,
    });
  }

  async unfollow(){
    this.unFollowUser().subscribe((o) => {
      this.getProfileInfo().subscribe((profile) => {
        this.profileSubject.next(profile);
      });    
    });
  }

  unFollowUser(){
    const unFollowRequest: UnFollowRequest = {
      MEM_ID: this.auth.getUserProperty('MEM_ID') as string,
      FOLLOW_ID: this.profile?.MEM_ID,
    };

    const url = `/api/unFollowUser`;
    return this.http.post<object[]>(url, unFollowRequest, {
      needToken: true,
    });
  }

  // 팔로워 목록
  showFollowers() {
    const memInfo = this.auth.getUser();
    const profileInfo = this.profile;
    const mode = 1;
    const me = 2;

    this.router.navigate(['followers'], {
      state: { memInfo, profileInfo, mode, me },
    });
  }

  // 팔로잉 목록
  showFollowings() {
    const memInfo = this.auth.getUser();
    const profileInfo = this.profile;
    const mode = 2;
    const me = 2;

    this.router.navigate(['followers'], {
      state: { memInfo, profileInfo, mode, me },
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


  loadMore() {
    this.loadingMore = true;
    this.page++;
    console.log("loading more");

    const profileRequest: ProfileRequest = {
      PROFILE_ID: this.prsId,
      MEM_ID: this.auth.getUserProperty('MEM_ID') as string,
      PAGE: this.page
    };

    this.http.post<{contents: any[], contentsCount: number}>('/api/profileContents', profileRequest, {
      needToken: true,
    }).subscribe(res => {
      this.contentList = [...this.contentList, ...res.contents];
      this.loadingMore = false;
    });
  }

  onScroll(event: any) {
    const target = event.target;
    console.log("pikmin");

    const threshold = 150;
    const position = target.scrollTop + target.offsetHeight;
    const height = target.scrollHeight

    if (position > height - threshold && !this.loadingMore) {
      this.loadMore();
    }
  }

}
