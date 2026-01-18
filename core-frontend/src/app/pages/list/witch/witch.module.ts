import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { WitchPageRoutingModule } from './witch-routing.module';
import { RouterModule } from '@angular/router';
import { WitchPage } from './witch.page';
import { HeaderComponentModule } from 'src/app/components/header/header.module';
import { ContentListComponentModule } from 'src/app/components/content-list/content-list.module';
import { UserAvatarComponentModule } from 'src/app/components/user-avatar/user-avatar.module';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { ChatModalPage } from './chat/chat-modal.page';
import { ChatWriteModalPage } from './chat-write/chat-write-modal.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    WitchPageRoutingModule,
    HeaderComponentModule,
    ContentListComponentModule,
    RouterModule,
    UserAvatarComponentModule,
    ScrollingModule,
  ],
  declarations: [WitchPage,ChatModalPage],
})
export class WitchPageModule {}
