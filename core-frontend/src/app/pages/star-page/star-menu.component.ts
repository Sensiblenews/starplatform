import { Component } from '@angular/core';
import { PopoverController } from '@ionic/angular';
import { Router } from '@angular/router';

@Component({
  selector: 'app-star-menu',
  template: `
    <ion-list lines="none" class="menu-list">
      <ion-item button (click)="close('search')">
        <ion-icon name="search-outline" slot="start"></ion-icon>
        <ion-label>검색 로비로 이동</ion-label>
      </ion-item>
      
      <ion-item button (click)="close('refresh')">
        <ion-icon name="refresh-outline" slot="start"></ion-icon>
        <ion-label>새로고침</ion-label>
      </ion-item>

      <ion-item button (click)="close('report')" lines="none">
        <ion-icon name="alert-circle-outline" color="danger" slot="start"></ion-icon>
        <ion-label color="danger">신고하기</ion-label>
      </ion-item>
    </ion-list>
  `,
  styles: [`
    .menu-list { padding: 0; }
    ion-item { --min-height: 48px; font-size: 14px; }
    ion-icon { font-size: 20px; margin-right: 8px; }
  `]
})
export class StarMenuComponent {
  constructor(
    private popoverCtrl: PopoverController,
    private router: Router
  ) {}

  close(action: string) {
    // 선택한 액션을 부모(StarPage)에게 전달하며 닫음
    this.popoverCtrl.dismiss({ action });
  }
}