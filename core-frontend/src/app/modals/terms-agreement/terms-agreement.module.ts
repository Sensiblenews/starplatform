import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { TermsAgreementPageRoutingModule } from './terms-agreement-routing.module';

import { TermsAgreementPage } from './terms-agreement.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    TermsAgreementPageRoutingModule
  ],
  declarations: [TermsAgreementPage]
})
export class TermsAgreementPageModule {}
