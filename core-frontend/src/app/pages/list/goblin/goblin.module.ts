import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { GoblinPageRoutingModule } from './goblin-routing.module';

import { GoblinPage } from './goblin.page';
import { HeaderComponentModule } from 'src/app/components/header/header.module';
import { ContentListComponentModule } from 'src/app/components/content-list/content-list.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    GoblinPageRoutingModule,
    HeaderComponentModule,
    ContentListComponentModule,
  ],
  declarations: [GoblinPage],
})
export class GoblinPageModule {}
