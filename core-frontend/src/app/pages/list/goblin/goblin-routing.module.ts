import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { GoblinPage } from './goblin.page';

const routes: Routes = [
  {
    path: '',
    component: GoblinPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class GoblinPageRoutingModule {}
