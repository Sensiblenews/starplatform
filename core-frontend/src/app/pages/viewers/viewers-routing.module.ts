import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { ViewersPage } from './viewers.page';

const routes: Routes = [
  {
    path: '',
    component: ViewersPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ViewersPageRoutingModule {}
