import { AuthService } from './auth.service';
import { Platform } from '@ionic/angular';
import { HttpService } from './http.service';
import { BehaviorSubject, Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import 'cordova-plugin-purchase';
import {
  PRODUCTS_MAP,
  PurchaseState,
  CustomIAPProduct,
} from '../constants/StoreItems';

@Injectable({
  providedIn: 'root',
})
export class StoreService {
  private products: BehaviorSubject<CustomIAPProduct[]> = new BehaviorSubject(
    [],
  );
  private purchaseState: BehaviorSubject<PurchaseState> = new BehaviorSubject(
    null,
  );

  private purchasedDttm: Date;

  private pendingTargetPrsId: string | null = null;

  constructor(
    private http: HttpService,
    private platform: Platform,
    private auth: AuthService,
  ) {}

  public get purchased(): Observable<PurchaseState> {
    return this.purchaseState.asObservable();
  }

  public get productLists(): Observable<CustomIAPProduct[]> {
    return this.products.asObservable();
  }

  async initialize(): Promise<void> {
    const subscriptionType = CdvPurchase.ProductType.PAID_SUBSCRIPTION;
    const purchaseType = CdvPurchase.ProductType.CONSUMABLE;
    const purchasePlatform = this.platform.is('ios')
    ? CdvPurchase.Platform.APPLE_APPSTORE
    : this.platform.is('android')
    ? CdvPurchase.Platform.GOOGLE_PLAY
    : CdvPurchase.Platform.TEST;

    const purchaseMap = [
      {
        id: 'kr.co.sensiblenews.witchhunting.inapp.babystar',
        type: purchaseType,
        platform: purchasePlatform
      },
      {
        id: 'kr.co.sensiblenews.witchhunting.inapp.casiopea',
        type: purchaseType,
        platform: purchasePlatform
      },
      {
        id: 'kr.co.sensiblenews.witchhunting.inapp.bigdipper',
        type: purchaseType,
        platform: purchasePlatform
      },
      {
        id: 'kr.co.sensiblenews.witchhunting.inapp.sirius',
        type: purchaseType,
        platform: purchasePlatform
      },
      {
        id: 'kr.co.sensiblenews.witchhunting.inapp.andromeda',
        type: purchaseType,
        platform: purchasePlatform
      },
      {
        id: 'monthly_subscription_2.99', // 실제 스토어 ID로 변경 필요
        type: subscriptionType,
        platform: purchasePlatform
      }
    ];

    CdvPurchase.store.verbosity = CdvPurchase.LogLevel.DEBUG;
    CdvPurchase.store.register(purchaseMap);

    const loadedProducts: any[] = [];
    CdvPurchase.store.when().productUpdated(product => {
      const displayInfo = PRODUCTS_MAP[product.id] || { name: product.title, price: product.pricing?.price };

      loadedProducts.push({
        id: product.id,
        display: PRODUCTS_MAP[product.id],
      });

      if (loadedProducts.length === purchaseMap.length) {
        this.products.next(loadedProducts);
      }
    });

    this.setupListeners();

    return CdvPurchase.store.initialize([
      CdvPurchase.Platform.GOOGLE_PLAY,
      CdvPurchase.Platform.TEST,
      {
        platform: CdvPurchase.Platform.APPLE_APPSTORE,
        options: {
          needAppReceipt: true,
        }
      }
    ])
    .then((value: CdvPurchase.IError[]): void => {
      console.log(value);
    });
  }

  registerProducts(productMap) {
    CdvPurchase.store.register(productMap);
    // Object.keys(productMap).forEach((productKey) => {
    // });
    // this.store.refresh();
  }

  purchase(p: CdvPurchase.Product) {
    const platform = this.platform.is('ios') ? CdvPurchase.Platform.APPLE_APPSTORE : CdvPurchase.Platform.GOOGLE_PLAY;

    CdvPurchase.store.get(p.id, platform)?.getOffer()?.order()
    .then(error=>{
      if(error){
        this.purchaseState.next({
          state: 'cancelled',
          id: p.id,
          chargeInfo: PRODUCTS_MAP[p.id],
        });
      }
      console.log(error);
    });
    // this.store.order(p);
  }

  async orderSubscription(productId: string, authorId: string): Promise<any> {
    this.pendingTargetPrsId = authorId;
    
    const product = CdvPurchase.store.get(productId);
    if (product) {
      // 상품이 존재하면 주문 시도
      const offer = product.getOffer();
      if(offer) {
        return offer.order();
      } else {
        throw new Error('Offer not found');
      }
    } else {
      throw new Error('Product not found');
    }
  }

  setupListeners() {
    CdvPurchase.store.when().approved((transaction) => {
      console.warn("purchase approved "+transaction);

      const productId = transaction.products[0].id;
      
      // [로직 분기] 구독 상품인지 별 상품인지 확인
      if (productId === 'monthly_subscription_2.99') {
          // A. 구독 처리 로직
          return this.handleSubscriptionApproval(transaction);
      } else {
          // B. 기존 별 충전 로직
          const which = this.platform.is('ios') ? 'ios' : this.platform.is('android') ? 'android' : 'web';
          
          // PRODUCTS_MAP에서 정보 가져올 때 방어 코드
          const productInfo = PRODUCTS_MAP[productId];
          const starCount = productInfo ? productInfo.starCount : 0;

          const verifyPurchase = {
            receipt: `${which}-receipt`, // 실제 검증 시엔 transaction.verificationData 등 사용 권장
            transactionId: transaction.transactionId,
            chargedStarNum: starCount,
            productId: productId,
          };
          return this.purchasedFeedBack(verifyPurchase, transaction);
      }
    });

    CdvPurchase.store.when().verified((receipt) => {
      console.log("purchase verified "+receipt);
      
      // UI 갱신 알림
      this.purchaseState.next({
        state: 'success',
        id: receipt.id,
        chargeInfo: PRODUCTS_MAP[receipt.id] || { name: 'Subscription', starCount: 0 },
      });
      return receipt.finish();
    });

    CdvPurchase.store.error((error) => {
      console.log(error);
    });
  }

  async handleSubscriptionApproval(transaction: CdvPurchase.Transaction) {
      if (!this.pendingTargetPrsId) {
          console.error("No target author ID found for subscription");
          transaction.finish(); // 에러 상황이지만 일단 종료처리 하여 무한루프 방지
          return;
      }

      const payload = {
          MEM_ID: this.auth.getUser().MEM_ID,
          PRS_ID: this.pendingTargetPrsId,
          TRAN_ID: transaction.transactionId,
          // 필요한 경우 영수증 데이터 추가
      };

      try {
          // 구독 등록 API 호출 (ContentService 대신 HttpService 직접 사용하거나 주입 필요)
          // 순환 참조 방지를 위해 여기서는 http 사용
          await this.http.post('/api/subscribe', payload, { needToken: true }).toPromise();
          
          transaction.verify(); // 검증 완료 처리 (-> verified 리스너로 이동)
      } catch (e) {
          console.error('Subscription registration failed', e);
          // 실패 시 처리는 비즈니스 로직에 따라 다름 (재시도 등)
      } finally {
          this.pendingTargetPrsId = null; // 초기화
      }
  }

  async purchasedFeedBack(verifyPurchase, p?: CdvPurchase.Transaction): Promise<void> {
    try {
      var currentDttm = new Date();

      // cdvpurchase 플러그인에서 결재시 결재완료 콜백이 반복적으로 오는 현상 있어서 로직처리
      if(!this.purchasedDttm || ((+currentDttm - +this.purchasedDttm) / 1000) > 10)
      {
        const MEM_STAR_CHG = await this.sendPurchaseResult(verifyPurchase);
        this.auth.updateUserProperty({ MEM_STAR_CHG });
        if (p) {
          p.verify();
        }
      }

      this.purchasedDttm = new Date();
    } catch (error) {
      console.log(error);
    }
  }

  async sendPurchaseResult(verifyPurchase): Promise<number> {
    return this.http
      .post<number>('/api/purchase', verifyPurchase, {
        needToken: true,
      })
      .toPromise();
  }

  async checkActiveSubscription() {
    // 1. 스토어 정보 갱신
    await CdvPurchase.store.update();

    // 2. 'monthly_subscription_2.99' 상품 가져오기
    const product = CdvPurchase.store.get('monthly_subscription_2.99');
    
    // 3. 사용자가 해당 상품을 소유 중인지 확인 (owned)
    if (product && product.owned) {
        console.log('User has active subscription');
        
        // 4. (중요) 백엔드에 알리기 "이 사람 아직 구독 중이니 만료일 늘려줘"
        // 여기서 transaction 정보를 보내서 검증 후 DB 업데이트
        const transaction = product.getOffer();
        if (transaction) {
            // 이전에 만든 handleSubscriptionApproval을 재활용하거나
            // 갱신 전용 API를 호출
            // this.handleSubscriptionRenewal(transaction); 
            // this.handleSubscriptionApproval(transaction);
        }
    }
  }

  async restorePurchases(): Promise<boolean> {
    try {
      // 1. 스토어에 구매 내역 갱신 요청 (로그인 요구할 수 있음)
      await CdvPurchase.store.update();
      
      // 2. 갱신 후, 구독 상품('monthly_subscription_2.99')을 소유 중인지 확인
      const productId = 'monthly_subscription_2.99';
      const product = CdvPurchase.store.get(productId);
      
      if (product && product.owned) {
        console.log('Valid subscription found during restore.');
        
        // 3. (중요) 유효한 구독이 있다면, 백엔드에 갱신 요청
        // 트랜잭션 정보를 가져와서 서버에 보냄
        const transaction = CdvPurchase.store.localTransactions.find(t => 
            // A. 이 트랜잭션에 내 상품 ID가 포함되어 있는지 확인
            t.products.some(p => p.id === productId) &&
            // B. 승인(Approved) 또는 완료(Finished)된 상태인지 확인
            (t.state === CdvPurchase.TransactionState.APPROVED || 
             t.state === CdvPurchase.TransactionState.FINISHED)
        );
        
        if (transaction) {
           // 기존에 만들었던 핸들러 재사용 (또는 별도 restore API 호출)
           await this.handleSubscriptionApproval(transaction);
        }
        
        return true; // 복원 성공 (유효한 구독 있음)
      } else {
        console.log('No active subscription found.');
        return false; // 복원 실패 (유효한 구독 없음)
      }

    } catch (e) {
      console.error('Restore failed', e);
      throw e;
    }
  }
}