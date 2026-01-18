import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { MyPagePageRoutingModule } from './my-page-routing.module';

import { MyPagePage } from './my-page.page';

import { HeaderComponentModule } from '../../components/header/header.module';
import { UserAvatarComponentModule } from 'src/app/components/user-avatar/user-avatar.module';
import { ThousandModule } from 'src/app/pipes/thousand.module';
import { TruncatePipe } from '../../pipes/truncate.pipe';
import { SubscriptionListModalPageModule } from './modal/subscription-list-modal.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    MyPagePageRoutingModule,
    HeaderComponentModule,
    ThousandModule,
    UserAvatarComponentModule,
    SubscriptionListModalPageModule
  ],
  declarations: [MyPagePage, TruncatePipe],
})
export class MyPagePageModule {}
