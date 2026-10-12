import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { FeedDetailPageRoutingModule } from './feed-detail-routing.module';
import { FeedDetailPage } from './feed-detail.page';
import { FeedMenuComponent } from './feed-menu.component';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    FeedDetailPageRoutingModule
  ],
  declarations: [FeedDetailPage, FeedMenuComponent]
  // schemas: [CUSTOM_ELEMENTS_SCHEMA]  <-- 이 줄 삭제 (Swiper용 설정 제거)
})
export class FeedDetailPageModule {}