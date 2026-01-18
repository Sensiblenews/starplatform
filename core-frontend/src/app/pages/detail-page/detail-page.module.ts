import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { DetailPagePageRoutingModule } from './detail-page-routing.module';

import { DetailPagePage } from './detail-page.page';
import { UserAvatarComponentModule } from 'src/app/components/user-avatar/user-avatar.module';
import { SafeUrlPipe } from 'src/app/pipes/safe-url.pipe';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    DetailPagePageRoutingModule,
    UserAvatarComponentModule,
  ],
  declarations: [DetailPagePage, SafeUrlPipe],
})
export class DetailPagePageModule {}
