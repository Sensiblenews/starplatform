import { IsAuthedGuard } from 'src/app/guards/is-authed.guard';
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { MorePage } from './more.page';

const routes: Routes = [
  {
    path: '',
    component: MorePage,
  },
  {
    path: '',
    canActivate: [IsAuthedGuard],
    children: [
      {
        path: 'promotion-zone',
        loadChildren: () =>
          import('../../pages/promotion-zone/promotion-zone.module').then(
            (m) => m.PromotionZonePageModule,
          ),
      },
      {
        path: 'app-inst',
        loadChildren: () =>
          import('../../pages/app-inst/app-inst.module').then(
            (m) => m.AppInstPageModule,
          ),
      },
      {
        path: 'story',
        loadChildren: () =>
          import('../../pages/story/story.module').then(
            (m) => m.StoryPageModule,
          ),
      },
      {
        path: 'story/:viewId',
        loadChildren: () =>
          import('../../pages/list/storyview/storyview.module').then(
            (m) => m.StoryviewPageModule,
          ),
      },
      {
        path: 'faq',
        loadChildren: () =>
          import('../../pages/faq/faq.module').then((m) => m.FaqPageModule),
      },
      {
        path: 'charge-star',
        loadChildren: () =>
          import('../../pages/charge-star/charge-star.module').then(
            (m) => m.ChargeStarPageModule,
          ),
      },
      {
        path: 'nonri-goo-goo-dan',
        loadChildren: () =>
          import(
            '../../pages/list/nonri-goo-goo-dan/nonri-goo-goo-dan.module'
          ).then((m) => m.NonriGooGooDanPageModule),
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class MorePageRoutingModule {}
