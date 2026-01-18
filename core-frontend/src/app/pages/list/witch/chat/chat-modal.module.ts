import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';
import { ChatModalPage } from './chat-modal.page';
import { ChatModalPageRoutingModule } from './chat-modal-routing.module';
import { CdkFixedSizeVirtualScroll, CdkVirtualForOf, CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { UserAvatarComponentModule } from '../../../../components/user-avatar/user-avatar.module';
import { ChatWriteModalPage } from '../chat-write/chat-write-modal.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    ChatModalPageRoutingModule,
    CdkFixedSizeVirtualScroll,
    CdkVirtualForOf,
    CdkVirtualScrollViewport,
    UserAvatarComponentModule,
  ],
  declarations: [ChatWriteModalPage]
})
export class ChatModalPageModule {}
