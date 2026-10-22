import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { FeedDetailPageRoutingModule } from './feed-detail-routing.module';
import { FeedDetailPage } from './feed-detail.page';
import { FeedMenuComponent } from './feed-menu.component';
import { PinchZoomDirective } from 'src/app/components/pinch-zoom/pinch-zoom.directive';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    FeedDetailPageRoutingModule,
    // 상세 사진도 탭하기 전에 두 손가락으로 제자리 확대 (스타 페이지와 같은 디렉티브, 2026-10-23 4번)
    PinchZoomDirective
  ],
  declarations: [FeedDetailPage, FeedMenuComponent]
  // schemas: [CUSTOM_ELEMENTS_SCHEMA]  <-- 이 줄 삭제 (Swiper용 설정 제거)
})
export class FeedDetailPageModule {}