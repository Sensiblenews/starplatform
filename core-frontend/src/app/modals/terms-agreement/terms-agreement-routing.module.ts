import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { TermsAgreementPage } from './terms-agreement.page';

const routes: Routes = [
  {
    path: '',
    component: TermsAgreementPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class TermsAgreementPageRoutingModule {}
