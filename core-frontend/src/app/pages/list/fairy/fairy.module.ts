import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { FairyPageRoutingModule } from './fairy-routing.module';

import { FairyPage } from './fairy.page';
import { HeaderComponentModule } from 'src/app/components/header/header.module';
import { ContentListComponentModule } from 'src/app/components/content-list/content-list.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    FairyPageRoutingModule,
    HeaderComponentModule,
    ContentListComponentModule,
  ],
  declarations: [FairyPage],
})
export class FairyPageModule {}
