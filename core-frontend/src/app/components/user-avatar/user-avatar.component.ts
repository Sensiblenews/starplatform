import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { NavParams } from '@ionic/angular';

@Component({
  selector: 'app-user-avatar',
  templateUrl: './user-avatar.component.html',
  styleUrls: ['./user-avatar.component.scss'],
  providers: [NavParams],
})
export class UserAvatarComponent implements OnInit, OnChanges {
  public profileAvatar: string;
  private defaultAvatar: string = '../../../assets/img/defaultImg/avatar.svg';

  @Input() profileImg: string = this.defaultAvatar;

  constructor(private navParams: NavParams) {}

  ngOnInit() {
    // this.profileAvatar = this.profileImg;
  }

  ionViewWillEnter() {
    this.profileAvatar = this.navParams.get('profileImg');
  }
  
  ngOnChanges() {
    console.log('ngonchanges');

    if(this.profileImg === undefined){
      this.profileImg = null;
    }

    this.profileAvatar = this.profileImg;
    if(!this.profileImg){
      this.profileAvatar = this.defaultAvatar;
    }
  }

  fallBack() {
    this.profileAvatar = this.defaultAvatar;
  }
}
