import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { MonsterPageRoutingModule } from './monster-routing.module';

import { MonsterPage } from './monster.page';
import { HeaderComponentModule } from 'src/app/components/header/header.module';
import { ContentListComponentModule } from 'src/app/components/content-list/content-list.module';
import { ContentListCardComponentModule } from 'src/app/components/content-list-card/content-list-card.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    MonsterPageRoutingModule,
    HeaderComponentModule,
    ContentListComponentModule,
    ContentListCardComponentModule,
  ],
  declarations: [MonsterPage],
})
export class MonsterPageModule {}
