import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { ProfilePagePageRoutingModule } from './profile-page-routing.module';

import { ProfilePagePage } from './profile-page.page';

import { UserAvatarComponentModule } from 'src/app/components/user-avatar/user-avatar.module';
import { ThousandModule } from 'src/app/pipes/thousand.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    ProfilePagePageRoutingModule,
    ThousandModule,
    UserAvatarComponentModule,
  ],
  declarations: [ProfilePagePage]
})
export class ProfilePagePageModule {}
