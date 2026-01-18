import { Component, Input } from '@angular/core';
import { ModalController, LoadingController } from '@ionic/angular';
import { StoreService } from 'src/app/services/store.service';
import { HelperService } from 'src/app/services/helper.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-subscribe-modal',
  templateUrl: './subscribe-modal.page.html',
  styleUrls: ['./subscribe-modal.page.scss'],
})
export class SubscribeModalPage {
  @Input() content: any;
  private readonly PRODUCT_ID = 'monthly_subscription_2.99';
  private purchaseSub: Subscription;

  constructor(
    private modalCtrl: ModalController,
    private store: StoreService,
    private helper: HelperService,
    private loadingCtrl: LoadingController
  ) { }

  async subscribe() {
    const loading = await this.loadingCtrl.create({ message: 'Requesting Store...' });
    await loading.present();

    try {
      // 1. 스토어 결제 요청 (작가 ID 함께 전달)
      await this.store.orderSubscription(this.PRODUCT_ID, this.content.PRS_ID);
      
      // 2. 결제 결과 대기 (StoreService의 purchaseState 구독)
      this.purchaseSub = this.store.purchased.subscribe(async (state) => {
        if (state && state.state === 'success') {
           // 결제 및 DB 등록 완료됨
           await loading.dismiss();
           this.helper.toast('구독이 완료되었습니다.');
           this.modalCtrl.dismiss({ subscribed: true });
           this.purchaseSub.unsubscribe();
        } else if (state && state.state === 'cancelled') {
           await loading.dismiss();
           this.helper.toast('구매가 취소되었습니다.');
           this.purchaseSub.unsubscribe();
        }
      });

    } catch (error) {
      await loading.dismiss();
      this.helper.toast('Store Error: ' + error.message);
      loading.dismiss();
      this.modalCtrl.dismiss();
      console.error(error);
    }
  }
  
  close() {
    if(this.purchaseSub) this.purchaseSub.unsubscribe();
    this.modalCtrl.dismiss();
  }
}