import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { PeterPanPage } from './peter-pan.page';

const routes: Routes = [
  {
    path: '',
    component: PeterPanPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PeterPanPageRoutingModule {}
