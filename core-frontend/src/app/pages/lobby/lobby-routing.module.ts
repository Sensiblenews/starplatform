import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { LobbyPage } from './lobby.page';

const routes: Routes = [
  {
    // 경로가 빈 값('')인 이유는 AppRoutingModule에서 이미 'lobby'로 들어왔기 때문입니다.
    path: '',
    component: LobbyPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class LobbyPageRoutingModule {}