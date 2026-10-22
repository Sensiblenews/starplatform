import { Injectable } from '@angular/core';
import { ModalController, AlertController } from '@ionic/angular';
import { AdminWriteModalComponent, ComposeGuard } from '../pages/lobby/modals/admin-write-modal.component';

/**
 * 작성판이 떠 있는 동안 뒤 화면을 멈추기 위한 연결 고리 (2-35차).
 * 작성 중에 로비 숏폼이 계속 돌고 네이티브 광고(웹뷰 위에 뜨는 네이티브 뷰)가 작성판 위로 겹쳐 잘못 눌렸다.
 * 부르는 쪽이 열릴 때 광고 숨김·영상 정지를, 닫힐 때 복구를 넘긴다
 */
export interface ComposeHooks {
  onPresent?: () => void;
  onDismiss?: () => void;
}

@Injectable({
  providedIn: 'root'
})
export class WriteModalService {
  constructor(
    private modalCtrl: ModalController,
    private alertCtrl: AlertController
  ) {}

  async openWriteModal(isStar: boolean, starId: string, adminLevel: string, onSuccess: () => void, hooks?: ComposeHooks) {
    const data = await this.present({ isStar, starId, adminLevel }, hooks);
    if (data && data.success) {
      // 동영상 글은 서버 압축이 끝난 뒤 게시물이 만들어진다 (2-34차) — 끝나면 FeedUploadService 가 따로 알린다.
      // 안내는 완료 사실 한 문장만 (2-35차 클라이언트 요청)
      const alert = await this.alertCtrl.create({
        header: 'Notice',
        message: data.processing
          ? 'Your video has been uploaded.'
          : 'Your post has been successfully created!',
        buttons: ['OK']
      });
      await alert.present();
      onSuccess();
    }
  }

  /** 게시물 수정 (2-35차) — 같은 작성판을 수정 모드로 연다. 스타 본인 글만 해당 */
  async openEditModal(starId: string, conId: string | number, onSuccess: () => void, hooks?: ComposeHooks) {
    const data = await this.present({ isStar: true, starId, adminLevel: '', editConId: conId }, hooks);
    if (data && data.success) {
      const alert = await this.alertCtrl.create({
        header: 'Notice',
        message: data.pending
          ? 'Your changes have been saved. The new photo will be visible after review.'
          : 'Your changes have been saved.',
        buttons: ['OK']
      });
      await alert.present();
      onSuccess();
    }
  }

  private async present(props: any, hooks?: ComposeHooks): Promise<any> {
    // 작성 중인 내용이 있으면 X·뒤로가기·스와이프로 닫을 때 확인한다. 저장 성공은 'confirmed' 로 닫는다
    const guard: ComposeGuard = { isDirty: () => false };
    const modal = await this.modalCtrl.create({
      component: AdminWriteModalComponent,
      componentProps: { ...props, guard },
      cssClass: 'compose-modal',
      canDismiss: async (_data?: any, role?: string) => {
        if (role === 'confirmed' || !guard.isDirty()) return true;
        return this.confirmDiscard();
      }
    });

    if (hooks && hooks.onPresent) hooks.onPresent();
    try {
      await modal.present();
      const { data } = await modal.onDidDismiss();
      return data;
    } finally {
      if (hooks && hooks.onDismiss) hooks.onDismiss();
    }
  }

  private async confirmDiscard(): Promise<boolean> {
    const alert = await this.alertCtrl.create({
      header: 'Discard changes?',
      message: 'What you have written will not be saved.',
      buttons: [
        { text: 'Keep Editing', role: 'cancel' },
        { text: 'Discard', role: 'destructive' }
      ]
    });
    await alert.present();
    const { role } = await alert.onDidDismiss();
    return role === 'destructive';
  }
}
