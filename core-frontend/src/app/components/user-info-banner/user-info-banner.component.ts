import { UserInfo } from 'src/app/types/Auth';
import { Component, Input, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-user-info-banner',
  templateUrl: './user-info-banner.component.html',
  styleUrls: ['./user-info-banner.component.scss'],
})
export class UserInfoBannerComponent {
  @Input() userInfo: UserInfo;
  @Input() isAuthed: boolean;
  @Output() OnCheckCount: EventEmitter<any> = new EventEmitter();
  constructor() {}

  checkUserCount() {
    return this.OnCheckCount.emit(true);
  }
}
