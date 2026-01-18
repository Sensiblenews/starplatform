import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { FairyPage } from './fairy.page';

const routes: Routes = [
  {
    path: '',
    component: FairyPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class FairyPageRoutingModule {}
