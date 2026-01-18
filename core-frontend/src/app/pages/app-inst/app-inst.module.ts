import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { AppInstPageRoutingModule } from './app-inst-routing.module';

import { AppInstPage } from './app-inst.page';
import { SwiperModule } from 'swiper/angular';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    AppInstPageRoutingModule,
    SwiperModule,
  ],
  declarations: [AppInstPage],
})
export class AppInstPageModule {}
