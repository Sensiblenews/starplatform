import { HttpService } from './http.service';
import { HelperService } from './helper.service';
import { StorageService } from './storage.service';
import { ChatModalPage } from '../pages/list/witch/chat/chat-modal.page';
import { Injectable } from '@angular/core';
import { FollowInfo, UserInfo } from '../types/Auth';
import { BehaviorSubject, Observable } from 'rxjs';
import { Content, type ContentResponse } from '../types/Content';

@Injectable({
  providedIn: 'root',
})
export class ChatService {
  constructor(private http: HttpService,
              private helper: HelperService,
              private storage: StorageService,) { }


  /** 쪽지 모달 창 열기*/
  async openChatModal(followId:number,followPicture,followName, parentFunc: () => void): Promise<boolean> {
    const chatModal = await this.helper.callModal({
      component: ChatModalPage,
      backdropDismiss: true,
      showBackdrop: true,
      cssClass: 'chatView',
      componentProps: { followId,followPicture,followName }, // 전달할 데이터
    });

    chatModal.onDidDismiss().then((result) => {
      parentFunc(); // 부모 함수 실행
    });

    await chatModal.present();
    const { data: authed } = await chatModal.onWillDismiss();
    return authed ?? false;
  }

  /** 쪽지 등록*/
  postChat(chatInfo: any): Observable<ContentResponse> {
    const url = `/api/addMessage`;
    return this.http.post<ContentResponse>(url, chatInfo, {
      needToken: true,
    });
  }

  /** 쪽지 읽음 처리*/
  readChat(chatInfo: any): Observable<ContentResponse> {
    const url = `/api/readMessage`;
    return this.http.post<ContentResponse>(url, chatInfo, {
      needToken: true,
    });
  }

  // {CONTENT: 'juj', RECEIVE_MEM_ID: '2025031300001', SEND_MEM_ID: '2025060300001'}
}
