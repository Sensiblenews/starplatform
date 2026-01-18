import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { StoryviewPageRoutingModule } from './storyview-routing.module';

import { StoryviewPage } from './storyview.page';
import { HeaderComponentModule } from 'src/app/components/header/header.module';
import { ContentListComponentModule } from 'src/app/components/content-list/content-list.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    StoryviewPageRoutingModule,
    HeaderComponentModule,
    ContentListComponentModule,
  ],
  declarations: [StoryviewPage]
})
export class StoryviewPageModule {}
