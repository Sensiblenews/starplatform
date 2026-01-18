import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { PromotionZonePageRoutingModule } from './promotion-zone-routing.module';

import { PromotionZonePage } from './promotion-zone.page';
import { ContentListComponentModule } from './../../components/content-list/content-list.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    PromotionZonePageRoutingModule,
    ContentListComponentModule,
  ],
  declarations: [PromotionZonePage],
})
export class PromotionZonePageModule {}
