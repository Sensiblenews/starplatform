import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { StarPagePageRoutingModule } from './star-page-routing.module';
import { StarPagePage } from './star-page.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    StarPagePageRoutingModule
  ],
  declarations: [StarPagePage]
})
export class StarPagePageModule {}