import { Injectable } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { LoginModalPage } from '../modals/login-modal/login-modal.page';

@Injectable({
  providedIn: 'root'
})
export class CallLoginViewService {

  constructor(
    public modalController: ModalController
  ) {}

  async callLoginView(){
    const modal = await this.modalController.create({
      component: LoginModalPage,
      cssClass: 'loginView'
    });

    return await modal.present();
  }
}
