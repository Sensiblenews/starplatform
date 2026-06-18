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
      const alert = await this.alertCtrl.create({
        header: 'Notice',
        message: 'Your post has been successfully created!',
        buttons: ['OK']
      });
      await alert.present();
      onSuccess();
    }
  }
}
