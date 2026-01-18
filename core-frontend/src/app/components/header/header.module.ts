import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { HeaderComponent } from './header.component';
import { EventBannerComponentModule } from '../event-banner/event-banner.module';
@NgModule({
  imports: [CommonModule, FormsModule, IonicModule, EventBannerComponentModule],
  declarations: [HeaderComponent],
  exports: [HeaderComponent],
})
export class HeaderComponentModule {}
