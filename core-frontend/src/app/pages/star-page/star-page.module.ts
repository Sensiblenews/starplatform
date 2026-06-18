import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { StarPagePageRoutingModule } from './star-page-routing.module';
import { StarPagePage } from './star-page.page';
import { IsPlayingPipe } from 'src/app/components/content-list/is-playing.pipe';
import { TruncatePipe } from 'src/app/pipes/truncate.pipe';
import { StarMenuComponent } from './star-menu.component';
import { CommentModalComponent } from './modals/comment-modal.component';
import { MyInsightModalComponent } from './modals/my-insight-modal.component';
import { AdminWriteModalModule } from '../lobby/modals/admin-write-modal.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    StarPagePageRoutingModule,
    AdminWriteModalModule
  ],
  declarations: [StarPagePage, IsPlayingPipe, TruncatePipe, StarMenuComponent, CommentModalComponent, MyInsightModalComponent]
})
export class StarPagePageModule {}