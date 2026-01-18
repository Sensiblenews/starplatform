import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule, Routes } from '@angular/router';
import { IsAuthedGuard } from './guards/is-authed.guard';

const routes: Routes = [
  { path: '', redirectTo: '/tabs/fairy', pathMatch: 'full' },
  {
    path: 'tabs',
    loadChildren: () =>
      import('./pages/tabs/tabs.module').then((m) => m.TabsPageModule),
  },
  {
    path: 'more',
    loadChildren: () =>
      import('./pages/more/more.module').then((m) => m.MorePageModule),
  },
  {
    path: 'login-modal',
    loadChildren: () =>
      import('./modals/login-modal/login-modal.module').then(
        (m) => m.LoginModalPageModule,
      ),
  },
  {
    path: 'tutorial/:tutorialId',
    loadChildren: () =>
      import('./pages/detail-page/detail-page.module').then(
        (m) => m.DetailPagePageModule,
      ),
  },
  {
    path: '',
    canActivate: [IsAuthedGuard],
    children: [
      {
        path: 'contents/:contentId',
        loadChildren: () =>
          import('./pages/detail-page/detail-page.module').then(
            (m) => m.DetailPagePageModule,
          ),
      },
      {
        path: 'search-results',
        loadChildren: () =>
          import('./pages/list/search-results/search-results.module').then(
            (m) => m.SearchResultsPageModule,
          ),
      },
      {
        path: 'profile-page',
        loadChildren: () =>
          import('./pages/profile-page/profile-page.module').then(
            (m) => m.ProfilePagePageModule,
          ),
      },
      {
        path: 'followers',
        loadChildren: () => import('./pages/followers/followers.module').then(
          (m) => m.FollowersPageModule,
        ),
      },
      {
        path: 'viewers',
        loadChildren: () => import('./pages/viewers/viewers.module').then(m => m.ViewersPageModule),
      },
      {
        path: 'peter-pan',
        loadChildren: () => import('./pages/peter-pan/peter-pan.module').then(m => m.PeterPanPageModule),
      },
      {
        path: 'tinker-bell',
        loadChildren: () => import('./pages/tinker-bell/tinker-bell.module').then(m => m.TinkerBellPageModule),
      },
      {
        path: 'monster',
        loadChildren: () => import('./pages/monster/monster.module').then(m => m.MonsterPageModule),
      },
    ],
  },
  {
    path: '**',
    pathMatch: 'full',
    redirectTo: '/tabs/fairy',
  },
  {
    path: 'tinker-bell',
    loadChildren: () => import('./tinker-bell/tinker-bell.module').then(m => m.TinkerBellPageModule),
  },

];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {
}
