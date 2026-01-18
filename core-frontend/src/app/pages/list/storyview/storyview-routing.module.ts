import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { StoryviewPage } from './storyview.page';

const routes: Routes = [
  {
    path: '',
    component: StoryviewPage,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class StoryviewPageRoutingModule {}
