import { Injectable } from '@angular/core';
import { ModalController, AlertController } from '@ionic/angular';
import { AdminWriteModalComponent } from '../pages/lobby/modals/admin-write-modal.component';

@Injectable({
  providedIn: 'root'
})
export class WriteModalService {
  constructor(
    private modalCtrl: ModalController,
    private alertCtrl: AlertController
  ) {}

  async openWriteModal(isStar: boolean, starId: string, adminLevel: string, onSuccess: () => void) {
    const modal = await this.modalCtrl.create({
      component: AdminWriteModalComponent,
      componentProps: {
        isStar,
        starId,
        adminLevel
      }
    });

    await modal.present();

    const { data } = await modal.onDidDismiss();
    if (data && data.success) {
      // 동영상 글은 서버 압축이 끝난 뒤 게시물이 만들어진다 (2-34차) — 끝나면 FeedUploadService 가 따로 알린다
      const alert = await this.alertCtrl.create({
        header: 'Notice',
        message: data.processing
          ? 'Your video has been uploaded. We are preparing it now and will let you know when it is ready.'
          : 'Your post has been successfully created!',
        buttons: ['OK']
      });
      await alert.present();
      onSuccess();
    }
  }
}
