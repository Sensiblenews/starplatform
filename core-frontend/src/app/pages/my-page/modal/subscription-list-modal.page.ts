import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { IonicModule, ModalController, Platform } from "@ionic/angular";
import { AuthService } from "src/app/services/auth.service";
import { HelperService } from "src/app/services/helper.service";
import { HttpService } from "src/app/services/http.service";

@Component({
  imports: [CommonModule, FormsModule, IonicModule],
  selector: 'app-subscription-list-modal',
  templateUrl: './subscription-list-modal.page.html',
  styleUrls: ['./subscription-list-modal.page.scss'],
})
export class SubscriptionListModalPage implements OnInit {
  subList: any[] = [];

  constructor(
    private modalCtrl: ModalController,
    private http: HttpService,
    private auth: AuthService,
    private platform: Platform,
    private helper: HelperService,
  ) { }

  ngOnInit() {
    this.loadSubscriptions();
  }

  loadSubscriptions() {
    const user = this.auth.getUser();
    this.http.post('/app/mySubscriptions', { MEM_ID: user.MEM_ID }, { needToken: true })
      .subscribe((res: any) => {
        if (res.success && Array.isArray(res.list)) {
          this.subList = res.list;
        } else {
          this.subList = [];
        }
        
        console.log('Final subList:', this.subList);
      });
  }

  async cancelSubscription(item: any) {
    const alert = await this.helper.alert({
      header: '구독 취소',
      message: '구독 취소는 스토어 설정 페이지에서 진행해야 합니다. 이동하시겠습니까?',
      buttons: [
        { text: '닫기', role: 'cancel' },
        {
          text: '이동',
          handler: () => {
            this.openStoreSubscription();
          }
        }
      ]
    });
    await alert.present();
  }

  openStoreSubscription() {
    let url = '';
    if (this.platform.is('ios')) {
      url = 'https://apps.apple.com/account/subscriptions';
    } else {
      url = 'https://play.google.com/store/account/subscriptions';
    }
    window.open(url, '_system');
  }

  close() {
    this.modalCtrl.dismiss();
  }
}