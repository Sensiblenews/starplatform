import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';

import { EmailLoginModalComponent } from './email-login-modal.component';

@NgModule({
  declarations: [EmailLoginModalComponent],
  imports: [
    CommonModule,
    FormsModule,
    IonicModule
  ]
})
export class EmailLoginModalModule {}
