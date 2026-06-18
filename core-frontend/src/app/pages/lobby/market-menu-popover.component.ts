// market-menu-popover.component.ts
import { Component, Input } from '@angular/core';
import { PopoverController, IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { Browser } from '@capacitor/browser';

@Component({
  selector: 'app-market-menu-popover',
  standalone: true,
  imports: [CommonModule, IonicModule],
  template: `
    <ion-list lines="none" class="m-0">
      <ion-item button (click)="selectMenu('ipo')">
        <ion-icon name="document-text-outline" slot="start" color="primary"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">Create a Star Page</ion-label>
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

      <ion-item button (click)="openExternal('https://witch-hunting.com/witch/privacy')">
        <ion-icon name="shield-checkmark-outline" slot="start" color="medium"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">Privacy Policy</ion-label>
      </ion-item>
      <ion-item button (click)="openExternal('https://witch-hunting.com/witch/terms')">
        <ion-icon name="document-outline" slot="start" color="medium"></ion-icon>
        <ion-label class="fw-bold text-dark" style="font-size: 15px;">Terms of Service</ion-label>
      </ion-item>
    </ion-list>
  `
})
export class MarketMenuPopoverComponent {
  @Input() isLoggedIn: boolean = false;

  constructor(private popoverCtrl: PopoverController) { }

  selectMenu(action: string) {
    this.popoverCtrl.dismiss({ action });
  }

  async openExternal(url: string) {
    await Browser.open({ url });
    await this.popoverCtrl.dismiss();
  }
}