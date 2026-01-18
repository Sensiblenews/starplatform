import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { DetailPagePage } from './detail-page.page';
import { IsAuthedGuard } from 'src/app/guards/is-authed.guard';

const routes: Routes = [
  {
    path: '',
    component: DetailPagePage
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class DetailPagePageRoutingModule {}
