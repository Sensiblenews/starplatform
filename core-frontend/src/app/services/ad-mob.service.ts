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

    AdMob.addListener(BannerAdPluginEvents.SizeChanged, (info: AdMobBannerSize) => {
      // const appMargin = info.height;
      // const app: HTMLElement = document.querySelector('ion-router-outlet');

      // if(appMargin === 0){
      //   app.style.marginBottom = '0px';
      //   return;
      // }

      // if(appMargin > 0){
      //   const body = document.querySelector('body');
      //   const bodyStyles = window.getComputedStyle(body);
      //   const safeAreaBottom = bodyStyles.getPropertyValue("--ion-safe-area-bottom");
      //   app.style.marginBottom = `calc(${safeAreaBottom} + ${appMargin}px)`;
      // }

      // if (height > 0) {
      //   requestAnimationFrame(() => {
      //     const ionApp: HTMLElement = document.querySelector('ion-app');
      //     ionApp.style.marginBottom = `${height}px`;
      //   });
      // } else {
      //   requestAnimationFrame(() => {
      //     const ionApp: HTMLElement = document.querySelector('ion-app');
      //     ionApp.style.removeProperty('margin-bottom');
      //   });
      // }
    });
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

  async showInterstitial(): Promise<void> {
    if (!Capacitor.isNativePlatform()) {
      return;
    }

    const adId =
      Capacitor.getPlatform() === 'ios'
        ? INTERSTITIAL_AD_ID_IOS
        : INTERSTITIAL_AD_ID;

    const resp = await AdMob.prepareInterstitial({
      adId,
    });

    if (resp.adUnitId) {
      await AdMob.showInterstitial();
    }
  }
}
