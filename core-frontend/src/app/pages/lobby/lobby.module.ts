import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { LobbyPageRoutingModule } from './lobby-routing.module';
import { LobbyPage } from './lobby.page';
import { BoardModalComponent } from './modals/board-modal.component';
import { IpoModalComponent } from './modals/ipo-modal.component';
import { PurchaseModalComponent } from './modals/purchase-modal.component';
import { AdminWriteModalModule } from './modals/admin-write-modal.module';
import { AvailablePageModalComponent } from './modals/available-page-modal.component';
import { GeneralRankingModalComponent } from './modals/rankings/general-ranking-modal.component';
import { RevenueRankingModalComponent } from './modals/rankings/revenue-ranking-modal.component';
import { DailyRankingModalComponent } from './modals/rankings/daily-ranking-modal.component';
import { HallOfFameModalComponent } from './modals/rankings/hall-of-fame-modal.component';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    LobbyPageRoutingModule,
    AdminWriteModalModule
  ],
  declarations: [
    LobbyPage,
    IpoModalComponent,
    BoardModalComponent,
    PurchaseModalComponent,
    AvailablePageModalComponent,
    GeneralRankingModalComponent,
    RevenueRankingModalComponent,
    DailyRankingModalComponent,
    HallOfFameModalComponent,
  ],
})
export class LobbyPageModule {}