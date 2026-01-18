import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { IsAuthedGuard } from 'src/app/guards/is-authed.guard';
import { TabsPage } from './tabs.page';

const routes: Routes = [
  {
    path: '',
    redirectTo: '/tabs/fairy',
    pathMatch: 'full',
  },
  {
    path: '',
    component: TabsPage,
    children: [
      {
        path: 'fairy',
        loadChildren: () =>
          import('../list/fairy/fairy.module').then((m) => m.FairyPageModule),
      },
      {
        path: 'peter-pan',
        loadChildren: () => import('../list/peter-pan/peter-pan.module').then((m) => m.PeterPanPageModule)
      },
      {
        path: 'tinker-bell',
        loadChildren: () => import('../list/tinker-bell/tinker-bell.module').then((m) => m.TinkerBellPageModule)
      },
      {
        path: 'witch',
        loadChildren: () =>
          import('../list/witch/witch.module').then((m) => m.WitchPageModule),
      },
      {
        path: 'goblin',
        loadChildren: () =>
          import('../list/goblin/goblin.module').then(
            (m) => m.GoblinPageModule,
          ),
      },
      {
        path: 'mypage',
        loadChildren: () =>
          import('../../pages/my-page/my-page.module').then(
            (m) => m.MyPagePageModule,
          ),
      },
    ],
  },
  {
    path: '**',
    pathMatch: 'full',
    redirectTo: '/',
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
})
export class TabsPageRoutingModule {}
