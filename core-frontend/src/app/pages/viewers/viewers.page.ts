import { Component } from '@angular/core';
import { UserInfo } from 'src/app/types/Auth';
import { BehaviorSubject, Observable } from 'rxjs';
import type { ViewersRequest, ViewersResponse } from '../../types/Content';
import { HelperService } from './../../services/helper.service';
import { HttpService } from '../../services/http.service';
import { NavController } from '@ionic/angular';
import { Router } from '@angular/router';
import type { ProfileInfo, FollowListRequest, FollowingListRequest, FollowInfo, FollowRequest, UnFollowRequest } from '../../types/Auth';

@Component({
  selector: 'app-viewers',
  templateUrl: './viewers.page.html',
  styleUrls: ['./viewers.page.scss'],
})
export class ViewersPage {
  public memInfo: UserInfo = null;
  public conId: number = null;
  public loaded = false;
  public dataList: BehaviorSubject<ViewersResponse[]> = new BehaviorSubject([]);

  constructor(
    private navController: NavController,
    private router: Router,
    private helper: HelperService,
    private http: HttpService,
  ) { }

  public get pageTitle(): string {
    return '열람인';
  }

  moveBack() {
    try {
      this.navController.back();
    } catch (e) {
      this.navController.navigateRoot('/');
    }
  }
  
  public get itemSize(): number {
    return 80;
  }

  public get itemCount(): number {
    return 10;
  }

  ionViewDidEnter() {
    const memInfo: UserInfo = history.state.memInfo;
    const conId: number = history.state.conId;

    this.memInfo = memInfo;
    this.conId = conId;

    this.loadViewers().subscribe((f)=>{
      this.dataList.next(f);
      this.loaded = true;
    });
  }

  public get viewerList(): Observable<ViewersResponse[]>{
    return this.dataList.asObservable();
  }

  profilePage(item){
    const prsId = item.VIEWER_ID;
    const memName = item.VIEWER_NAME;

    console.log(item);

    this.router.navigate(['profile-page'], {
      state: { prsId, memName },
    });
  }

  loadViewers(){
    var req: ViewersRequest = {
      CON_ID: this.conId,
      // MEM_ID: this.memInfo.MEM_ID,
    };

    const url = `/api/viewers`;
    return this.http.post<object[]>(url, req, {
      needToken: true,
    });
  }

  follow(event,item){
    console.log('follow');

    this.followUser(this.memInfo.MEM_ID, item.VIEWER_ID).subscribe((o) => {
      this.loadViewers().subscribe((f)=>{
        this.dataList.next(f);
        this.loaded = true;
      });
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

    this.unFollowUser(this.memInfo.MEM_ID, item.VIEWER_ID).subscribe((o) => {
      this.loadViewers().subscribe((f)=>{
        this.dataList.next(f);
        this.loaded = true;
      });
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
