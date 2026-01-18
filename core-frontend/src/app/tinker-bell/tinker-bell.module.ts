import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { TinkerBellPageRoutingModule } from './tinker-bell-routing.module';

import { TinkerBellPage } from './tinker-bell.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    TinkerBellPageRoutingModule
  ],
  declarations: [TinkerBellPage]
})
export class TinkerBellPageModule {}
