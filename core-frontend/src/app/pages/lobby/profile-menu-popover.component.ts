import { Component, Input, OnInit } from '@angular/core'; // OnInit 추가
import { PopoverController, IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // 🌟 [(ngModel)] 사용을 위해 필수!
import { HttpService } from '../../services/http.service'; // 경로에 맞게 수정해주세요

@Component({
  selector: 'app-profile-menu-popover',
  standalone: true,
  imports: [CommonModule, IonicModule, FormsModule], // 🌟 FormsModule 추가
  template: `
    <ion-list lines="none" class="m-0">
      <ion-item *ngIf="!isAdmin" button (click)="selectMenu('mypage')">
        <ion-icon name="person-circle-outline" slot="start" color="primary"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">My Page</ion-label>
      </ion-item>
      
      <ion-item *ngIf="!isAdmin" button (click)="selectMenu('messages')">
        <ion-icon name="chatbubble-ellipses-outline" slot="start" color="success"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">Messages</ion-label>
      </ion-item>
      
      <ion-item button (click)="selectMenu('logout')">
        <ion-icon name="log-out-outline" slot="start" style="color: #ff3b30; font-size: 20px;"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">Logout</ion-label>
      </ion-item>

      <!-- 🌟 구분선 및 푸시 알림 설정 (관리자가 아닐 때만 노출) -->
      <ng-container *ngIf="!isAdmin">
        <ion-item-divider style="min-height: 1px; background: #eee; margin: 4px 0;"></ion-item-divider>
        
        <ion-item lines="none">
          <ion-icon name="notifications-outline" slot="start" color="warning"></ion-icon>
          <ion-label class="fw-bold text-dark" style="font-size: 15px;">Visitor Push</ion-label>
          <!-- 🌟 토글 스위치 -->
          <ion-toggle slot="end" [(ngModel)]="isPushEnabled" (ionChange)="togglePush()"></ion-toggle>
        </ion-item>
      </ng-container>
    </ion-list>
  `
})
export class ProfileMenuPopoverComponent implements OnInit {
  @Input() isAdmin: boolean = false;
  
  // 🌟 푸시 알림 상태 변수 (기본값 ON)
  isPushEnabled: boolean = true;

  constructor(
    private popoverCtrl: PopoverController,
    private http: HttpService // 🌟 API 호출용 주입
  ) {}

  ngOnInit() {
    // 🌟 컴포넌트가 열릴 때, 로컬 스토리지에 저장된 설정값을 불러옴
    this.isPushEnabled = localStorage.getItem('pushEnabled') !== 'false';
  }

  selectMenu(action: string) {
    this.popoverCtrl.dismiss({ action });
  }

  // 🌟 토글 변경 시 실행되는 함수
  togglePush() {
    const starId = localStorage.getItem('starId');
    const pushYn = this.isPushEnabled ? 'Y' : 'N';
    
    // 1. 기기에 상태 저장 (다음에 열 때 기억하기 위해)
    localStorage.setItem('pushEnabled', this.isPushEnabled ? 'true' : 'false');

    // 2. 서버 DB에 상태 업데이트
    if (starId) {
      this.http.post('/api/super/star/push/toggle', {
        starId: starId,
        pushYn: pushYn
      }).subscribe({
        next: (res: any) => {
          if (res.result === 'OK') {
            console.log('Push setting updated successfully:', pushYn);
          }
        },
        error: (err) => console.error('Failed to update push setting', err)
      });
    }
  }
}