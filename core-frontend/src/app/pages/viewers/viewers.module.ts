import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { ViewersPageRoutingModule } from './viewers-routing.module';
import { ViewersPage } from './viewers.page';
import { UserAvatarComponentModule } from 'src/app/components/user-avatar/user-avatar.module';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    ViewersPageRoutingModule,
    RouterModule,
    UserAvatarComponentModule,
    ScrollingModule,
  ],
  declarations: [ViewersPage]
})
export class ViewersPageModule {}
