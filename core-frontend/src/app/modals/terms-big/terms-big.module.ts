import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { TermsBigModal } from './terms-big.component';

@NgModule({
  declarations: [TermsBigModal],
  imports: [
    CommonModule,
    FormsModule,
    IonicModule
  ]
})
export class TermsBigModalModule {}
