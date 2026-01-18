import { RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { IonicModule } from '@ionic/angular';
import { ContentListCardComponent } from './content-list-card.component';
import { UserAvatarComponentModule } from 'src/app/components/user-avatar/user-avatar.module';
import { VirtualScrollerModule } from 'ngx-virtual-scroller';
import { SubscribeModalPage } from './modal/subscribe-modal.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    ScrollingModule,
    RouterModule,
    UserAvatarComponentModule,
    VirtualScrollerModule
  ],
  declarations: [ContentListCardComponent, SubscribeModalPage],
  exports: [ContentListCardComponent],
})
export class ContentListCardComponentModule {}
