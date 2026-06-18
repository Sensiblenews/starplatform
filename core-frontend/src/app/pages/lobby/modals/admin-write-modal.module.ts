import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { AdminWriteModalComponent } from './admin-write-modal.component';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule
  ],
  declarations: [
    AdminWriteModalComponent
  ],
  exports: [
    AdminWriteModalComponent
  ]
})
export class AdminWriteModalModule { }
