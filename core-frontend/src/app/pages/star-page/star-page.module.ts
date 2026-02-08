import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { StarPagePageRoutingModule } from './star-page-routing.module';
import { StarPagePage } from './star-page.page';
import { IsPlayingPipe } from 'src/app/components/content-list/is-playing.pipe';
import { TruncatePipe } from 'src/app/pipes/truncate.pipe';
import { StarMenuComponent } from './star-menu.component';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    StarPagePageRoutingModule
  ],
  declarations: [StarPagePage, IsPlayingPipe, TruncatePipe, StarMenuComponent]
})
export class StarPagePageModule {}