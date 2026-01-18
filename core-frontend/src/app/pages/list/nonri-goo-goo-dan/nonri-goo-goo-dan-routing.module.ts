import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { NonriGooGooDanPage } from './nonri-goo-goo-dan.page';

const routes: Routes = [
  {
    path: '',
    component: NonriGooGooDanPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class NonriGooGooDanPageRoutingModule {}
