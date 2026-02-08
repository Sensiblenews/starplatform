import { Injectable } from '@angular/core';
import {
  AdMob,
  AdMobBannerSize,
  AdmobConsentStatus,
  BannerAdOptions,
  BannerAdPluginEvents,
  BannerAdPosition,
  BannerAdSize,
} from '@capacitor-community/admob';
import { Capacitor } from '@capacitor/core';
import {
  BANNER_AD_ID,
  BANNER_AD_ID_IOS,
  INTERSTITIAL_AD_ID,
  INTERSTITIAL_AD_ID_IOS,
} from './../constants/Keys/AdMob';

@Injectable({
  providedIn: 'root',
})
export class AdMobService {
  constructor() {}

  async initialize(): Promise<void> {
    AdMob.initialize({
      initializeForTesting: false,
    });

    const [trackingInfo, consentInfo] = await Promise.all([
      AdMob.trackingAuthorizationStatus(),
      AdMob.requestConsentInfo(),
    ]);

    if (trackingInfo.status === 'notDetermined') {
      await AdMob.requestTrackingAuthorization();
    }

    const authorizationStatus = await AdMob.trackingAuthorizationStatus();
    if (
      authorizationStatus.status === 'authorized' &&
      consentInfo.isConsentFormAvailable &&
      consentInfo.status === AdmobConsentStatus.REQUIRED
    ) {
      await AdMob.showConsentForm();
    }

    await this.loadInterstitial();
  }

  async showBanner(): Promise<void> {
    const adId =
      Capacitor.getPlatform() === 'ios' ? BANNER_AD_ID_IOS : BANNER_AD_ID;
    const options: BannerAdOptions = {
      adId,
      adSize: BannerAdSize.ADAPTIVE_BANNER,
      position: BannerAdPosition.BOTTOM_CENTER,
      margin: 0,
    };
    AdMob.showBanner(options);
  }

  // [신규] 전면 광고 미리 로드하기 (보여주지는 않음)
  async loadInterstitial(): Promise<void> {
    if (!Capacitor.isNativePlatform()) return;

    const adId = Capacitor.getPlatform() === 'ios' ? INTERSTITIAL_AD_ID_IOS : INTERSTITIAL_AD_ID;

    try {
      // prepareInterstitial은 광고를 로드만 하고 메모리에 올려둡니다.
      await AdMob.prepareInterstitial({ adId });
      console.log('전면 광고 로드 완료');
    } catch (e) {
      console.error('전면 광고 로드 실패', e);
    }
  }

  async showInterstitial(): Promise<void> {
    if (!Capacitor.isNativePlatform()) return;

    try {
      // 1. 준비된 광고 보여주기
      await AdMob.showInterstitial();
      
      // 2. [중요] 광고를 보여줬으니, 다음 번을 위해 바로 다시 장전!
      this.loadInterstitial(); 
    } catch (e) {
      console.log('광고가 아직 준비되지 않았거나 에러 발생. 다시 장전 시도.');
      // 혹시라도 없어서 못 보여줬다면, 다음을 위해 지금이라도 장전
      this.loadInterstitial();
    }
  }
}
