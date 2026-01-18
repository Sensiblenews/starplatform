import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common'; // [필수] *ngFor, *ngIf 사용용
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';   // [필수] ion-list, ion-item 사용용

import { SubscriptionListModalPage } from './subscription-list-modal.page';

@NgModule({
  imports: [
    CommonModule, // <-- 이게 없으면 리스트가 절대 안 나옵니다!
    FormsModule,
    IonicModule,
    // SubscriptionListModalPageRoutingModule (라우팅 안 쓰면 생략 가능)
  ],
  declarations: [SubscriptionListModalPage],
  exports: [SubscriptionListModalPage] // 모달로 쓸 때는 exports에 넣어주는 게 안전함
})
export class SubscriptionListModalPageModule {}