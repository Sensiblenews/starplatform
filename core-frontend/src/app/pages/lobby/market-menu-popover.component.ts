// market-menu-popover.component.ts
import { Component, Input, OnInit } from '@angular/core';
import { PopoverController, IonicModule, AlertController } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Capacitor } from '@capacitor/core';
import { PushNotifications } from '@capacitor/push-notifications';
import { HttpService } from '../../services/http.service';
import { DeepLinkService } from '../../services/deep-link.service';

@Component({
  selector: 'app-market-menu-popover',
  standalone: true,
  imports: [CommonModule, IonicModule, FormsModule],
  template: `
    <ion-list lines="none" class="m-0">
      <ion-item button (click)="selectMenu('ipo')">
        <ion-icon name="document-text-outline" slot="start" color="primary"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">Create a Star Page</ion-label>
      </ion-item>

      <ion-item button *ngIf="!isLoggedIn" (click)="selectMenu('creator_studio')">
        <ion-icon name="easel-outline" slot="start" color="success"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">Creator Studio</ion-label>
      </ion-item>

      <!-- 🌟 사람 아이콘 팝오버에서 이곳으로 이동한 항목들 (Create a Star Page 아래) -->
      <ion-item button *ngIf="isLoggedIn && !isAdmin" (click)="selectMenu('messages')">
        <ion-icon name="chatbubble-ellipses-outline" slot="start" color="success"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">Message</ion-label>
      </ion-item>

      <ion-item button *ngIf="isLoggedIn" (click)="selectMenu('logout')">
        <ion-icon name="log-out-outline" slot="start" style="color: #ff3b30; font-size: 20px;"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">Logout</ion-label>
      </ion-item>

      <ion-item lines="none" *ngIf="isLoggedIn && !isAdmin">
        <ion-icon name="notifications-outline" slot="start" color="warning"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">Visitor Push</ion-label>
        <ion-toggle slot="end" [(ngModel)]="isPushEnabled" (ionChange)="togglePush()"></ion-toggle>
      </ion-item>

      <!-- <ion-item button (click)="selectMenu('board')">
        <ion-icon name="list-outline" slot="start" color="primary"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">List Board</ion-label>
      </ion-item> -->

      <div style="height: 1px; background: #eee; margin: 4px 0;"></div>

      <ng-container *ngIf="!isLoggedIn">
        <!-- <ion-item button (click)="selectMenu('login_star')">
          <ion-icon name="person-circle-outline" slot="start" color="secondary"></ion-icon>
          <ion-label class="fw-bold text-dark" style="font-size: 15px;">Creator Login</ion-label>
        </ion-item> -->
        <!-- <ion-item button (click)="selectMenu('login_admin')">
          <ion-icon name="settings-outline" slot="start" color="medium"></ion-icon>
          <ion-label class="fw-bold text-dark" style="font-size: 15px;">Admin Login</ion-label>
        </ion-item> -->
      </ng-container>

      <ion-item button (click)="openExternal('https://witch-hunting.com/privacy')">
        <ion-icon name="shield-checkmark-outline" slot="start" color="medium"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">Privacy Policy</ion-label>
      </ion-item>
      <ion-item button (click)="openExternal('https://witch-hunting.com/terms')">
        <ion-icon name="document-outline" slot="start" color="medium"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">Terms of Service</ion-label>
      </ion-item>
    </ion-list>
  `
})
export class MarketMenuPopoverComponent implements OnInit {
  @Input() isLoggedIn: boolean = false;
  @Input() isAdmin: boolean = false;

  // 푸시 알림 상태 (기본값 ON)
  isPushEnabled: boolean = true;

  // 서버값 반영/프로그래밍 방식 토글 변경 중 ionChange 오발화 차단용
  private syncing: boolean = false;

  constructor(
    private popoverCtrl: PopoverController,
    private alertCtrl: AlertController,
    private http: HttpService,
    private deepLink: DeepLinkService
  ) { }

  ngOnInit() {
    // 스타 계정에서만 노출되는 항목이므로 그 외에는 조회하지 않는다
    if (!this.isLoggedIn || this.isAdmin) return;

    // 1. 로컬 스토리지 값으로 낙관적 초기화 (서버 응답 전 깜빡임 방지)
    this.isPushEnabled = localStorage.getItem('pushEnabled') !== 'false';

    // 2. 서버의 실제 PUSH_YN으로 덮어쓰기.
    //    로컬만 믿으면 재설치/기기 변경 후 DB는 'N'인데 UI는 ON으로 보여
    //    토글 이벤트가 발생하지 않아 다시 켤 수 없는 버그가 생긴다.
    const starId = localStorage.getItem('starId');
    if (starId) {
      this.http.get(`/api/super/star/push/status?starId=${encodeURIComponent(starId)}`).subscribe({
        next: (res: any) => {
          if (res && res.result === 'OK') {
            const enabled = res.pushYn === 'Y';
            if (enabled !== this.isPushEnabled) {
              this.syncing = true;
              this.isPushEnabled = enabled;
              setTimeout(() => this.syncing = false, 0);
            }
            localStorage.setItem('pushEnabled', enabled ? 'true' : 'false');
          }
        },
        error: (err) => console.error('Failed to load push setting', err)
      });
    }
  }

  selectMenu(action: string) {
    this.popoverCtrl.dismiss({ action });
  }

  async openExternal(url: string) {
    await this.deepLink.openExternal(url);
    await this.popoverCtrl.dismiss();
  }

  // 토글 변경 시 실행
  async togglePush() {
    if (this.syncing) return;

    // ON으로 켤 때는 OS 알림 권한부터 확인/요청한다.
    // 첫 설치 시 권한을 거부한 유저의 복구 경로가 여기다.
    if (this.isPushEnabled && Capacitor.isNativePlatform()) {
      try {
        let permStatus = await PushNotifications.checkPermissions();
        if (permStatus.receive === 'prompt') {
          permStatus = await PushNotifications.requestPermissions();
        }

        if (permStatus.receive === 'granted') {
          await PushNotifications.register();
          // registration 이벤트 미발생 대비: 캐싱된 토큰을 직접 업로드
          setTimeout(() => this.sendCachedToken(), 1500);
        } else {
          // denied: OS가 재요청 다이얼로그를 띄우지 않으므로 설정 이동을 안내하고 실상태(OFF)로 되돌린다
          await this.showPermissionAlert();
          this.revertToggle(false);
          this.savePushSetting('N');
          return;
        }
      } catch (e) {
        console.error('Push permission check failed', e);
      }
    }

    this.savePushSetting(this.isPushEnabled ? 'Y' : 'N');
  }

  // 서버 DB(PUSH_YN)와 로컬 스토리지에 설정 저장
  private savePushSetting(pushYn: string) {
    localStorage.setItem('pushEnabled', pushYn === 'Y' ? 'true' : 'false');

    const starId = localStorage.getItem('starId');
    if (!starId) return;

    this.http.post('/api/super/star/push/toggle', {
      starId: starId,
      pushYn: pushYn
    }).subscribe({
      next: (res: any) => {
        if (res.result === 'OK') {
          console.log('Push setting updated successfully:', pushYn);
        }
      },
      error: (err) => {
        console.error('Failed to update push setting', err);
        // 저장 실패 시 토글을 원복해 UI와 서버 상태 불일치를 막는다
        this.revertToggle(pushYn !== 'Y');
        localStorage.setItem('pushEnabled', pushYn === 'Y' ? 'false' : 'true');
      }
    });
  }

  // ionChange 재발화 없이 토글 값 변경
  private revertToggle(value: boolean) {
    this.syncing = true;
    this.isPushEnabled = value;
    setTimeout(() => this.syncing = false, 0);
  }

  // registration 이벤트가 다시 안 오는 경우를 대비해 캐싱된 FCM 토큰을 직접 서버에 전송
  private sendCachedToken() {
    const starId = localStorage.getItem('starId');
    const cachedToken = localStorage.getItem('fcmToken');
    if (starId && cachedToken) {
      this.http.post('/api/super/star/push/token', {
        starId: starId,
        fcmToken: cachedToken
      }).subscribe({
        next: () => console.log('FCM token re-sent to server'),
        error: (err) => console.error('FCM token send failed', err)
      });
    }
  }

  private async showPermissionAlert() {
    const alert = await this.alertCtrl.create({
      header: 'Notifications Disabled',
      message: 'Push notifications are turned off for this app in your device settings. Please enable them in Settings > Notifications, then turn this switch on again.',
      buttons: ['OK']
    });
    await alert.present();
  }
}
