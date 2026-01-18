import { Content } from 'src/app/types/Content';
import { Component, Input, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';

@Component({
  selector: 'app-popup-modal',
  templateUrl: './popup-modal.page.html',
  styleUrls: ['./popup-modal.page.scss'],
})
export class PopupModalPage {
  @Input() popupContent: Content = null;
  public blockForWeek: boolean;
  constructor(private modalCtrl: ModalController) {}

  dismiss() {
    this.modalCtrl.dismiss(this.blockForWeek);
  }
}
