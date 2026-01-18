import { Component, Input } from '@angular/core';
import { ModalController, Platform } from '@ionic/angular';
import { InAppBrowser } from '@awesome-cordova-plugins/in-app-browser/ngx';
import { FollowInfo, UserInfo } from '../../../../types/Auth';
import { HttpService } from '../../../../services/http.service';
import { HelperService } from '../../../../services/helper.service';
import { BehaviorSubject, Observable } from 'rxjs';
import { AuthService } from '../../../../services/auth.service';
import { ChatService } from '../../../../services/chat.service';

@Component({
  selector: 'chat-modal',
  templateUrl: './chat-modal.page.html',
  styleUrls: ['./chat-modal.page.scss'],
})
export class ChatModalPage {
  public available: boolean;
  constructor(
    public modalCtrl: ModalController,
    private platform: Platform,
    private http : HttpService,
    private auth : AuthService,
    private helper: HelperService,
    private chatService:ChatService
  ) {
    this.platform.ready().then(() => {
      // this.available = this.platform.is('ios');
      this.available = true;
    });
  }

  public dataList: BehaviorSubject<any[]> = new BehaviorSubject([]);

  public loaded = false;

  public picture = "";

  public memInfo: UserInfo = null;

  @Input() followId!: Number; // follow 아이디
  @Input() followPicture!: string; // follow 이미지
  @Input() followName!: string; // follow 이름

  public get itemSize(): number {
    return 80;
  }

  public get itemCount(): number {
    return 10;
  }


  public get messageList(): Observable<any[]>{
    return this.dataList.asObservable();
  }

  ionViewDidEnter() {
    this.memInfo = this.auth.getUser();

    this.picture = this.followPicture;

    this.chatList(this.memInfo.MEM_ID, this.followId).subscribe((o) => {
      this.dataList.next(o);
      this.readChatData();
      this.loaded = true;
    });

  }

  chatList(memId, followId){
    const chatParameter: any = {
      RECEIVE_MEM_ID: memId,
      SEND_MEM_ID: followId,
    };

    const url = `/api/messageList`;
    // TODO: 나중에 제거
    const p = this.http.post<object[]>(url, chatParameter, {
      needToken: true,
    });
    console.log(p)
    return p;
  }

  /** 데이터 읽음 처리 */
  readChatData() {
    const readObject: any = {
      RECEIVE_MEM_ID : this.memInfo.MEM_ID
    };

    this.chatService.readChat(readObject).toPromise();
  }

  modalClose(result = false) {
    this.modalCtrl.dismiss(result);
  }
}
