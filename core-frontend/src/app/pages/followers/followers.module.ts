import { RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { FollowersPageRoutingModule } from './followers-routing.module';
import { FollowersPage } from './followers.page';
import { UserAvatarComponentModule } from 'src/app/components/user-avatar/user-avatar.module';
import { ScrollingModule } from '@angular/cdk/scrolling';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    FollowersPageRoutingModule,
    RouterModule,
    UserAvatarComponentModule,
    ScrollingModule,
  ],
  declarations: [FollowersPage]
})
export class FollowersPageModule {}
