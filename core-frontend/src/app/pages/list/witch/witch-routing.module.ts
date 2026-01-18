import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { WitchPage } from './witch.page';

const routes: Routes = [
  {
    path: '',
    component: WitchPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class WitchPageRoutingModule {}
