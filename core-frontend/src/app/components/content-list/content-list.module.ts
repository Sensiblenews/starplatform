import { RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { IonicModule } from '@ionic/angular';
import { ContentListComponent } from './content-list.component';
import { UserAvatarComponentModule } from 'src/app/components/user-avatar/user-avatar.module';
import { IsPlayingPipe } from './is-playing.pipe';
import { TruncatePipe } from 'src/app/pipes/truncate.pipe';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    ScrollingModule,
    RouterModule,
    UserAvatarComponentModule,
  ],
  declarations: [ContentListComponent, IsPlayingPipe, TruncatePipe],
  exports: [ContentListComponent],
})
export class ContentListComponentModule {}
