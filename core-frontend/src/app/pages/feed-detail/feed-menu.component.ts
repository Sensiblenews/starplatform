import { Component, Input } from '@angular/core';
import { PopoverController } from '@ionic/angular';

// 콘텐츠 상세(feed-detail) 헤더 점점점 메뉴 (2-33차 — 아이콘만 있고 메뉴가 없던 버그 수정).
// 스타 페이지 메뉴(StarMenuComponent)와 같은 구성에 Share 를 더했다. 선택한 액션은 부모가 처리한다
@Component({
  selector: 'app-feed-menu',
  template: `
    <ion-list lines="none" class="menu-list">
      <!-- 본인 글이면 수정·삭제 (2-35차) -->
      <ng-container *ngIf="canEdit">
        <ion-item button (click)="close('edit')">
          <ion-icon name="create-outline" slot="start"></ion-icon>
          <ion-label>Edit Post</ion-label>
        </ion-item>
        <ion-item button (click)="close('delete')">
          <ion-icon name="trash-outline" slot="start"></ion-icon>
          <ion-label>Delete Post</ion-label>
        </ion-item>
      </ng-container>

      <ion-item button (click)="close('search')">
        <ion-icon name="search-outline" slot="start"></ion-icon>
        <ion-label>Go to Search</ion-label>
      </ion-item>

      <ion-item button (click)="close('refresh')">
        <ion-icon name="refresh-outline" slot="start"></ion-icon>
        <ion-label>Refresh</ion-label>
      </ion-item>

      <ion-item button (click)="close('share')">
        <ion-icon name="share-social-outline" slot="start"></ion-icon>
        <ion-label>Share</ion-label>
      </ion-item>

      <ion-item button (click)="close('report')" lines="none">
        <ion-icon name="alert-circle-outline" color="danger" slot="start"></ion-icon>
        <ion-label color="danger">Report</ion-label>
      </ion-item>
    </ion-list>
  `,
  styles: [`
    .menu-list { padding: 0; }
    ion-item { --min-height: 48px; font-size: 14px; }
    ion-icon { font-size: 20px; margin-right: 8px; }
  `]
})
export class FeedMenuComponent {
  @Input() canEdit = false;

  constructor(private popoverCtrl: PopoverController) {}

  close(action: string) {
    this.popoverCtrl.dismiss({ action });
  }
}
