import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
// ▼ ScrollingModule 추가
import { ScrollingModule } from '@angular/cdk/scrolling'; 

import { TinkerBellPageRoutingModule } from './tinker-bell-routing.module';
import { TinkerBellPage } from './tinker-bell.page';
import { HeaderComponentModule } from 'src/app/components/header/header.module';
import { ContentListComponentModule } from 'src/app/components/content-list/content-list.module';
import { ContentListCardComponentModule } from 'src/app/components/content-list-card/content-list-card.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    TinkerBellPageRoutingModule,
    HeaderComponentModule,
    ContentListComponentModule,
    ScrollingModule,
    ContentListCardComponentModule,
  ],
  declarations: [TinkerBellPage]
})
export class TinkerBellPageModule {}