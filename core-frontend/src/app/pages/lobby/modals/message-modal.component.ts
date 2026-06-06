import { CommonModule } from "@angular/common";
import { Component, Input, OnInit, OnDestroy, ViewChild } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { IonicModule, ModalController, IonContent } from "@ionic/angular";
import { HttpService } from "src/app/services/http.service";

@Component({
  selector: 'app-message-modal',
  standalone: true,
  imports: [CommonModule, IonicModule, FormsModule],
  templateUrl: './message-modal.component.html',
  styleUrls: ['./message-modal.component.scss']
})
export class MessageModalComponent implements OnInit, OnDestroy {
  messages: any[] = [];
  newMessage: string = '';
  
  @Input() pageId: string;
  @Input() isStar: boolean = false; // 로비에서 넘어오는 값
  @Input() isAdmin: boolean = false; // 로비에서 넘어오는 값

  @ViewChild('content', { static: false }) content: IonContent; // 🌟 스크롤 제어용
  
  private pollingIntervalId: any;
  senderType: string = 'USER';

  constructor(private http: HttpService, private modalCtrl: ModalController) {}

  ngOnInit() { 
    // 🌟 현재 사용자가 운영자인지 크리에이터인지 판별
    this.senderType = this.isAdmin ? 'ADMIN' : 'USER';
    
    this.loadMessages(true); 
    this.startPolling();
  }

  ngOnDestroy() {
    this.stopPolling();
  }

  // 🌟 스크롤 옵션을 받도록 수정
  loadMessages(scrollToBottom: boolean = false) {
    this.http.post('/api/super/message/list', { 
      pageId: this.pageId, 
      readerType: this.senderType 
    }).subscribe((res: any) => {
      this.messages = res.list || [];
      
      // 🌟 데이터 로드 후 화면 맨 아래로 내리기
      if (scrollToBottom) {
        setTimeout(() => {
          this.content?.scrollToBottom(300);
        }, 100);
      }
    });
  }

  // 🌟 실시간 폴링 (3초 간격)
  startPolling() {
    this.pollingIntervalId = setInterval(() => {
      this.loadMessages(false); // 폴링 때는 스크롤을 강제로 내리지 않음 (읽는 중 방해 방지)
    }, 3000);
  }

  stopPolling() {
    if (this.pollingIntervalId) {
      clearInterval(this.pollingIntervalId);
    }
  }

  send() {
    if(!this.newMessage.trim()) return;
    
    this.http.post('/api/super/message/send', { 
      pageId: this.pageId, 
      senderType: this.senderType, // 🌟 동적 적용
      message: this.newMessage 
    }).subscribe(() => {
      this.newMessage = '';
      this.loadMessages(true); // 🌟 내가 보낸 직후엔 스크롤 맨 아래로!
    });
  }

  dismiss() { 
    this.modalCtrl.dismiss(); 
  }
}