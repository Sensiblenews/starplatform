import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { StarPagePage } from './star-page.page';

const routes: Routes = [
  {
    // AppRoutingModule에서 'star/:starId'로 진입하므로 여기서는 빈 값으로 둡니다.
    path: '',
    component: StarPagePage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class StarPagePageRoutingModule {}