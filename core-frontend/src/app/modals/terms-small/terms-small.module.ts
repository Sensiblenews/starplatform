import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { TermsSmallModal } from './terms-small.component';

@NgModule({
  declarations: [TermsSmallModal],
  imports: [
    CommonModule,
    FormsModule,
    IonicModule
  ]
})
export class TermsSmallModalModule {}
