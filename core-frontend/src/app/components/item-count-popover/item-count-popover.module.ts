import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { ItemCountPopoverComponent } from './item-count-popover.component';

@NgModule({
  imports: [CommonModule, IonicModule],
  declarations: [ItemCountPopoverComponent],
  exports: [ItemCountPopoverComponent],
})
export class ItemCountPopoverComponentModule {}
