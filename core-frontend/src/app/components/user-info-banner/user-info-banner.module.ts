import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { IonicModule } from '@ionic/angular';

import { UserInfoBannerComponent } from './user-info-banner.component';
import { UserAvatarComponentModule } from 'src/app/components/user-avatar/user-avatar.module';
import { RouterModule } from '@angular/router';
import { TruncatePipe } from 'src/app/pipes/truncate.pipe';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule, UserAvatarComponentModule],
  declarations: [UserInfoBannerComponent, TruncatePipe],
  exports: [UserInfoBannerComponent],
})
export class UserInfoBannerComponentModule {}
