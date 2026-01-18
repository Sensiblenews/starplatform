import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { NonriGooGooDanPageRoutingModule } from './nonri-goo-goo-dan-routing.module';

import { NonriGooGooDanPage } from './nonri-goo-goo-dan.page';
import { ContentListComponentModule } from 'src/app/components/content-list/content-list.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    NonriGooGooDanPageRoutingModule,
    ContentListComponentModule,
  ],
  declarations: [NonriGooGooDanPage],
})
export class NonriGooGooDanPageModule {}
