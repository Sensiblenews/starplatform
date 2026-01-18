import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { WriteModalPageRoutingModule } from './write-modal-routing.module';

import { WriteModalPage } from './write-modal.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    WriteModalPageRoutingModule
  ],
  declarations: [WriteModalPage]
})
export class WriteModalPageModule {}
