import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { IonicModule } from '@ionic/angular';

import { UserAvatarComponent } from './user-avatar.component';

@NgModule({
  imports: [CommonModule, IonicModule],
  declarations: [UserAvatarComponent],
  exports: [UserAvatarComponent],
})
export class UserAvatarComponentModule {}
