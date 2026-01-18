import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { EventBannerComponent } from './event-banner.component';
@NgModule({
  imports: [CommonModule, FormsModule, IonicModule],
  declarations: [EventBannerComponent],
  exports: [EventBannerComponent],
})
export class EventBannerComponentModule {}
