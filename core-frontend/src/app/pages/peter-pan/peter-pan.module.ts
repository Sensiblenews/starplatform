import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { PeterPanPageRoutingModule } from './peter-pan-routing.module';

import { PeterPanPage } from './peter-pan.page';

import { HeaderComponentModule } from 'src/app/components/header/header.module';
import { ContentListComponentModule } from 'src/app/components/content-list/content-list.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    PeterPanPageRoutingModule,
    HeaderComponentModule,
    ContentListComponentModule,
  ],
  declarations: [PeterPanPage]
})
export class PeterPanPageModule {}
