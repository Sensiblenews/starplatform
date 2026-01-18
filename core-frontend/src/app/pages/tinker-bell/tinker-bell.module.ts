import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { TinkerBellPageRoutingModule } from './tinker-bell-routing.module';

import { TinkerBellPage } from './tinker-bell.page';
import { HeaderComponentModule } from 'src/app/components/header/header.module';
import { ContentListComponentModule } from 'src/app/components/content-list/content-list.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    TinkerBellPageRoutingModule,
    HeaderComponentModule,
    ContentListComponentModule,
  ],
  declarations: [TinkerBellPage]
})
export class TinkerBellPageModule {}
