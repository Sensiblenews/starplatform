import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { PromotionZonePage } from './promotion-zone.page';

const routes: Routes = [
  {
    path: '',
    component: PromotionZonePage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PromotionZonePageRoutingModule {}
