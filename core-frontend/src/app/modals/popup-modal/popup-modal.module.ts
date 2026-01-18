import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { PopupModalPageRoutingModule } from './popup-modal-routing.module';

import { PopupModalPage } from './popup-modal.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    PopupModalPageRoutingModule
  ],
  declarations: [PopupModalPage]
})
export class PopupModalPageModule {}
