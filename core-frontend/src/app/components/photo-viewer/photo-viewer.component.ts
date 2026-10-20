import { Component, Input, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule, ModalController } from '@ionic/angular';
import { SwiperModule } from 'swiper/angular';
import SwiperCore, { Zoom } from 'swiper';
import { Swiper } from 'swiper/types';

SwiperCore.use([Zoom]);

/**
 * 사진 전체 화면 뷰어 (2-35차 — 손가락으로 확대·축소).
 *
 * 앱은 index.html 에서 페이지 확대를 막아 두었으므로(user-scalable=no) 사진만 따로 확대한다.
 * 이미 설치된 swiper 8 의 Zoom 모듈을 쓴다 — 두 손가락 확대, 두 번 탭 확대/원래대로, 확대 중 끌어서 이동.
 * 여러 장이면 좌우로 넘긴다.
 */
@Component({
  selector: 'app-photo-viewer',
  standalone: true,
  imports: [CommonModule, IonicModule, SwiperModule],
  encapsulation: ViewEncapsulation.None,
  template: `
    <ion-content class="photo-viewer-content" [fullscreen]="true">
      <button type="button" class="photo-viewer-close" (click)="close()" aria-label="Close">
        <ion-icon name="close"></ion-icon>
      </button>
      <div class="photo-viewer-count" *ngIf="urls.length > 1">{{ current + 1 }} / {{ urls.length }}</div>

      <swiper class="photo-viewer-swiper" [zoom]="zoomOptions" [initialSlide]="startIndex"
        (slideChange)="onSlideChange($event)">
        <ng-template swiperSlide *ngFor="let url of urls">
          <div class="swiper-zoom-container">
            <img [src]="url" alt="" />
          </div>
        </ng-template>
      </swiper>
    </ion-content>
  `,
  styles: [`
    .photo-viewer-content { --background: #000; }
    .photo-viewer-swiper { width: 100%; height: 100%; }
    .photo-viewer-swiper .swiper-slide { overflow: hidden; }
    /* swiper zoom 기본 스타일 (swiper/css/zoom 과 같은 내용) */
    .swiper-zoom-container { width: 100%; height: 100%; display: flex; justify-content: center; align-items: center; text-align: center; }
    .swiper-zoom-container > img { max-width: 100%; max-height: 100%; object-fit: contain; }
    .photo-viewer-close {
      position: absolute; z-index: 20;
      top: calc(12px + var(--ion-safe-area-top, 0px)); right: 12px;
      width: 40px; height: 40px; border: 0; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      background: rgba(255, 255, 255, 0.18); color: #fff; font-size: 24px;
    }
    .photo-viewer-count {
      position: absolute; z-index: 20;
      top: calc(20px + var(--ion-safe-area-top, 0px)); left: 0; right: 0;
      text-align: center; color: rgba(255, 255, 255, 0.85); font-size: 14px; pointer-events: none;
    }
  `]
})
export class PhotoViewerComponent {
  @Input() urls: string[] = [];
  @Input() startIndex = 0;

  current = 0;
  readonly zoomOptions = { maxRatio: 4, minRatio: 1, toggle: true };

  constructor(private modalCtrl: ModalController) {}

  ngOnInit() {
    this.current = this.startIndex || 0;
  }

  onSlideChange(event: any) {
    // swiper/angular 8 은 이벤트 인자로 [swiper] 배열을 넘긴다
    const swiper: Swiper | undefined = Array.isArray(event) ? event[0] : event;
    if (swiper) this.current = swiper.activeIndex;
  }

  close() {
    this.modalCtrl.dismiss();
  }
}
