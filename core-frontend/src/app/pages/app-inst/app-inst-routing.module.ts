import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AppInstPage } from './app-inst.page';

const routes: Routes = [
  {
    path: '',
    component: AppInstPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AppInstPageRoutingModule {}
