import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { ChargeStarPageRoutingModule } from './charge-star-routing.module';

import { ChargeStarPage } from './charge-star.page';
import { UserInfoBannerComponentModule } from './../../components/user-info-banner/user-info-banner.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    ChargeStarPageRoutingModule,
    UserInfoBannerComponentModule,
  ],
  declarations: [ChargeStarPage],
})
export class ChargeStarPageModule {}
