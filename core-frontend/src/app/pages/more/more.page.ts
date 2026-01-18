import { Subscription } from 'rxjs';
import { UserInfo } from 'src/app/types/Auth';
import { AuthService } from './../../services/auth.service';
import { HelperService } from './../../services/helper.service';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { NavController } from '@ionic/angular';
import { CallLoginViewService } from 'src/app/services/call-login-view.service';

@Component({
  selector: 'app-more',
  templateUrl: './more.page.html',
  styleUrls: ['./more.page.scss'],
})
export class MorePage implements OnInit, OnDestroy {
  public userInfo: UserInfo;
  public isAuthed: boolean;
  private userInfoSubscription: Subscription;
  private authInfoSubscription: Subscription;
  constructor(
    public navController: NavController,
    public loginView: CallLoginViewService,
    private auth: AuthService,
    private helper: HelperService,
  ) {}

  ngOnInit() {
    this.userInfoSubscription = this.auth
      .getUserAsObservable()
      .subscribe((user) => {
        this.userInfo = user;
      });

    this.authInfoSubscription = this.auth
      .isAuthedAsObservable()
      .subscribe((authed) => {
        this.isAuthed = authed;
      });
  }

  ngOnDestroy() {
    if (this.userInfoSubscription) {
      this.userInfoSubscription.unsubscribe();
      this.userInfoSubscription = null;
    }
    if (this.authInfoSubscription) {
      this.authInfoSubscription.unsubscribe();
      this.authInfoSubscription = null;
    }
  }

  checkUser() {
    return this.auth.showUserCount();
  }

  moveBack() {
    this.navController.back();
  }

  openLogic500Page() {
    this.helper.openURL({ url: 'http://naver.me/FosLrGZF' });
  }

  async requestId() {
    await this.helper.sendEmail({
      to: 'witchhunting777@gmail.com',
      subject: 'ID 신청',
      body: ``,
      isHtml: true,
    });
  }

  async shareApp(): Promise<void> {
    await this.helper.share(null);
  }
}
