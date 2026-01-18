import { Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ModalController, Platform, ToastController } from '@ionic/angular';
import { Observable, of, Subscription } from 'rxjs';
import { HttpService } from 'src/app/services/http.service';
import { AuthService } from '../../../../services/auth.service';
import { FilePicker, PickedFile, PickFilesResult } from '@capawesome/capacitor-file-picker';
import { Filesystem } from '@capacitor/filesystem';
import { environment } from 'src/environments/environment';
import { ImageViewerComponent } from '../image-viewer/image-viewer.component';
import { DomSanitizer } from '@angular/platform-browser';
import { Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';
import { HelperService } from 'src/app/services/helper.service';
import { CheckMessageService } from 'src/app/services/check-message.service';

export interface ChatMessage {
  message: string;
  sendDate: string;
  receiveDate: string;
  memPicture: string;
  isMe: boolean;
}

export interface PendingFile {
  file: PickedFile;
  previewUrl: string;
}

@Component({
  selector: 'chat-write-modal',
  templateUrl: './chat-write-modal.page.html',
  styleUrls: ['./chat-write-modal.page.scss'],
})
export class ChatWriteModalPage implements OnInit, OnDestroy {
  @ViewChild('scrollBottom') private scrollBottom!: ElementRef;
  chatList$!: Observable<ChatMessage[]>;

  commentInput: string = '';
  defaultPicture: string = '../../../../../assets/img/defaultImg/avatar.svg';
  public pendingFile: PendingFile | null = null;
  backButtonSubscription: any | null = null;

  @Input() followInfo: any;

  constructor(
    public modalController: ModalController,
    private auth: AuthService,
    private http: HttpService,
    private toastController: ToastController,
    private platform: Platform,
    private helper: HelperService,
    private checkMessageService: CheckMessageService,
  ) {}

  ngOnInit(): void {
    this.chatList$ = of([]);
  }

  ngOnDestroy(): void {
    if (this.backButtonSubscription) {
      this.backButtonSubscription.unsubscribe();
    }
  }

  ionViewDidEnter() {
    this.getChatList();
    this.readMessage();


    this.backButtonSubscription = this.platform.backButton.subscribeWithPriority(10000, () => {
      if (document.fullscreenElement) {
        document.exitFullscreen();
      } else {
        this.closeModal();
      }
    });
  }

  ionViewWillLeave() {
    if (this.backButtonSubscription) {
      this.backButtonSubscription.unsubscribe();
    }
  }

  async getChatList() {
    const myId = this.auth.getUser().MEM_ID;
    const followId = this.followInfo.FOLLOW_ID;
    const chatParameter = {
      SEND_MEM_ID: myId,
      RECEIVE_MEM_ID: followId,
    };

    const url = `/api/messageList`;

    const p = this.http.post<object[]>(url, chatParameter, {
      needToken: true,
    });

    p.subscribe({
      next: (response) => {
        const d = response.map((msg: any) => ({
          message: msg.CONTENT,
          sendDate: msg.SEND_DATE,
          receiveDate: msg.RECEIVE_DATE,
          memPicture: msg.MEM_PICTURE,
          isMe: msg.SEND_MEM_ID === myId
        }));
        this.chatList$ = of(d);

        setTimeout(() => this.scrollToBottom(), 100);
      },
      error: (err) => {
        console.error('Error fetching chat messages:', err);
      }
    });
  }

  closeModal() {
    this.checkMessageService.triggerCheck();
    this.modalController.dismiss();
  }

  readMessage() {
    const url = `/api/readMessage`;
    const parameters = {
      SEND_MEM_ID: this.auth.getUser().MEM_ID,
      RECEIVE_MEM_ID: this.followInfo.FOLLOW_ID,
    };

    this.http.post<any>(url, parameters, { needToken: true }).subscribe({
      next: () => {
        console.log("메시지 읽음 처리 성공");
      },
      error: (err) => {
        console.error('메시지 읽음 처리 실패: ', err);
      }
    });
  }

  async openFilePicker() {
    try {
      const platform = Capacitor.getPlatform();
      let result: PickFilesResult
      if (platform === 'ios') {
        result = await FilePicker.pickImages({ limit: 1, readData: true });
      } else {
        result = await FilePicker.pickFiles({ limit: 1, readData: true });
      }


      if (result.files && result.files.length > 0) {
        const file = result.files[0];
        
        const previewUrl = file.path 
          ? Capacitor.convertFileSrc(file.path) 
          : ((file as any).webPath || '');
        
        this.pendingFile = { file, previewUrl };
      }
    } catch (error) {
      console.error('파일 선택 오류:', error);
      await this.presentToast("파일을 선택하는 중 오류가 발생했습니다.");
    }
  }

  removePendingFile() {
    this.pendingFile = null;
  }

  async registChat() {
    if (this.pendingFile) {
      await this.uploadAndSendFile();
    } else if (this.commentInput.trim()) {
      await this.sendTextMessage(this.commentInput);
    } else {
      return;
    }
  }

  private async uploadAndSendFile() {
    if (!this.pendingFile) return;

    const file = this.pendingFile.file;
    const fileName = this.getUniqueFileName(file.name);
    const mimeType = file.mimeType;

    // FilePicker에서 이미 데이터를 읽었으므로 file.data를 사용합니다.
    const base64Data = file.data; 

    if (!base64Data) {
      await this.presentToast("파일 데이터를 읽을 수 없습니다.");
      return;
    }

    let fileCategory: 'image' | 'video' | 'file';
    if (mimeType.startsWith('image/')) fileCategory = 'image';
    else if (mimeType.startsWith('video/')) fileCategory = 'video';
    else fileCategory = 'file';

    await this.presentToast("업로드 중...");

    // 1. FormData 대신 일반 JSON 객체를 생성합니다.
    const payload = {
      fileName: fileName,
      base64: `data:${mimeType};base64,${base64Data}`,
      CONTENT: `${fileCategory}::${fileName}`,
      RECEIVE_MEM_ID: this.followInfo.FOLLOW_ID,
      SEND_MEM_ID: this.auth.getUser().MEM_ID.toString(),
    };

    // 2. HttpService의 postFile이 아닌, 일반 post 메소드를 사용합니다.
    const url = `/api/uploadChatFile`;
    this.http.post(url, payload, {
      needToken: true,
      contentType: 'json' // ContentType을 'json'으로 지정합니다.
    }).subscribe({
      next: async () => {
        await this.presentToast("업로드 완료");
        this.pendingFile = null;
        this.getChatList();
      },
      error: async (err) => {
        console.error('파일 업로드 실패:', err);
        await this.presentToast("업로드 실패");
      }
    });
  }

  private async sendTextMessage(message: string) {
    const sendObject = {
      CONTENT: message,
      RECEIVE_MEM_ID: this.followInfo.FOLLOW_ID,
      SEND_MEM_ID: this.auth.getUser().MEM_ID
    };

    const url = `/api/addMessage`;
    this.http.post<any>(url, sendObject, { needToken: true, contentType: 'json' })
      .subscribe({
        next: () => {
          this.commentInput = '';
          this.getChatList();
        },
        error: (err) => {
          console.error('메시지 전송 실패: ', err);
          this.presentToast("메시지 전송에 실패했습니다.");
        }
      });
  }

  scrollToBottom() {
    try {
      this.scrollBottom.nativeElement.scrollIntoView({ behavior: 'smooth' });
    } catch (err) {
      console.error('Scroll error:', err);
    }
  }

  getUniqueFileName(originalName: string): string {
    const now = new Date();
    const pad = (n: number) => n.toString().padStart(2, '0');
    const yyyy = now.getFullYear();
    const MM = pad(now.getMonth() + 1);
    const dd = pad(now.getDate());
    const hh = pad(now.getHours());
    const mm = pad(now.getMinutes());
    const ss = pad(now.getSeconds());
    const timestamp = `${yyyy}${MM}${dd}${hh}${mm}${ss}`;
    const extension = originalName.split('.').pop();
    return `${timestamp}.${extension}`;
  }

  getMessageType(message: string): 'image' | 'video' | 'file' | 'text' {
    if (message.startsWith('image::')) return 'image';
    if (message.startsWith('video::')) return 'video';
    if (message.startsWith('file::')) return 'file';
    return 'text';
  }

  getFileUrl(message: string): string {
    const fileName = message.split('::')[1];
    return environment.apiBaseURL + `/chatfiles/${fileName}`;
  }

  getVideoPoster(message: string): string {
    let fileName = message.split('::')[1];
    fileName = fileName.replace(/\.[^/.]+$/, ""); // 확장자 제거
    return environment.apiBaseURL + `/chatfiles/thumbnail/${fileName}.jpg`;
  }

  async openImageViewer(imageUrl: string) {
    const modal = await this.modalController.create({
      component: ImageViewerComponent,
      componentProps: { imageUrl },
      cssClass: 'image-viewer-modal'
    });
    return await modal.present();
  }

  async presentToast(message: string) {
    const toast = await this.toastController.create({
      message: message,
      duration: 2000,
      position: 'bottom',
      color: 'dark',
    });
    await toast.present();
  }

  async enterFullscreen(url: string) {
    this.helper.openURL({ url });
  }
}