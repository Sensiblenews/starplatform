import { Component } from '@angular/core';
import { NavParams } from '@ionic/angular';

@Component({
  selector: 'app-item-count-popover',
  templateUrl: './item-count-popover.component.html',
  styleUrls: ['./item-count-popover.component.scss'],
})
export class ItemCountPopoverComponent {
  public countInfo: { [key: string]: string } = null;
  constructor(private navParams: NavParams) {
    const cinfo = this.navParams.get('countInfo') || {
      type: 'heart',
      free: 0,
      charged: 0,
    };
    this.countInfo = cinfo;
  }
}
