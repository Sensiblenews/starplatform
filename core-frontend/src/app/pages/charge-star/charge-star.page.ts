import { Platform } from '@ionic/angular';
import { StorageService } from './../../services/storage.service';
import { UserInfo } from 'src/app/types/Auth';
import { HelperService } from './../../services/helper.service';
import { Observable, Subscription } from 'rxjs';
import { StoreService } from './../../services/store.service';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavController } from '@ionic/angular';
import { AuthService } from 'src/app/services/auth.service';
import { CustomIAPProduct } from '../../constants/StoreItems';
import 'cordova-plugin-purchase';

@Component({
  selector: 'app-charge-star',
  templateUrl: './charge-star.page.html',
  styleUrls: ['./charge-star.page.scss'],
})
export class ChargeStarPage implements OnInit, OnDestroy {
  public userInfo: UserInfo;
  public isAuthed: boolean;
  private userInfoSubscription: Subscription;
  private authInfoSubscription: Subscription;
  constructor(
    public navController: NavController,
    private auth: AuthService,
    private store: StoreService,
    private storage: StorageService,
    private helper: HelperService,
    private platform: Platform,
  ) {}

  public get storeProducts(): Observable<CustomIAPProduct[]> {
    return this.store.productLists;
  }

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

  async buyInAppPurchase(product): Promise<void> {
    const confirmModal = await this.helper.alert({
      header: product.display.name,
      subHeader: product.price,
      message: product.description,
      buttons: [
        { text: '취소', role: 'cancel' },
        { text: '결제', role: 'purchase' },
      ],
    });
    confirmModal.present();
    const { role } = await confirmModal.onDidDismiss();
    if (role === 'purchase') {
      try {
        await this.helper.loading();
        
        this.store.purchase(product);
      } catch (error) {
        this.helper.toast(
          `결제에 실패하였습니다. - ${error.message}`,
          'middle',
        );
      } finally {
        await this.helper.loadEnd();
      }
    }
  }

  async dailyCharge(): Promise<void> {
    try {
      const freeReceipt = {
        receipt: 'free',
        transactionId: 'free',
        productId: 'free',
        chargedStarNum: 10,
      };
      const freeChargedDate = await this.storage.get('freeChargedDate');
      const [today] = new Date().toISOString().split('T');
      if (!freeChargedDate || freeChargedDate !== today) {
        const MEM_STAR = await this.store.sendPurchaseResult(freeReceipt);
        this.auth.updateUserProperty({ MEM_STAR });
        const received = await this.helper.alert({
          header: '충전 완료!',
          subHeader: `쑤~우~웅 별 10개가 떨어졌네요. 총 ${MEM_STAR}개의 무료별을 보유 중입니다.`,
        });
        received.present();
        await this.storage.set('freeChargedDate', today);
        return;
      }
      const limited = await this.helper.alert({
        header: '내일 다시 눌러주세요^^',
        subHeader: `하루 한 번만 충전할 수 있습니다.`,
      });
      limited.present();
    } catch (error) {
      console.log(error);
    }
  }
}
