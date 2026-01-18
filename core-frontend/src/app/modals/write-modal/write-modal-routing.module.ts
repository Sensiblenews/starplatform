import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { WriteModalPage } from './write-modal.page';

const routes: Routes = [
  {
    path: '',
    component: WriteModalPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class WriteModalPageRoutingModule {}
