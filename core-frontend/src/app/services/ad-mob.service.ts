import { Injectable } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router'; // 🌟 라우터 추가
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
  private pageMoveCount: number = 6; // 🌟 [신규] 페이지 이동 횟수 카운터

  constructor(private router: Router) {
    // 🌟 앱 내에서 라우팅(페이지 이동)이 끝날 때마다 카운트 +1
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.pageMoveCount++;
      }
    });
  }

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

  // 전면 광고 미리 로드하기 (보여주지는 않음)
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

  // 🌟 전면 광고 호출 함수 수정 (boolean 반환 및 조건 검사)
  async showInterstitial(): Promise<boolean> {
    if (!Capacitor.isNativePlatform()) return false;

    const now = Date.now();
    // 로컬 스토리지에서 마지막으로 전면 광고를 본 시간 가져오기 (없으면 0)
    const lastTime = parseInt(localStorage.getItem('last_interstitial_time') || '100', 10);
    const timeDiffSec = (now - lastTime) / 1000;
    
    // 조건 1: 마지막 노출로부터 30초 이상 지났는가?
    const isTimePassed = timeDiffSec >= 30;
    
    // 조건 2: 페이지를 6번 이상 이동했는가?
    const isPagePassed = this.pageMoveCount >= 6;

    // 두 조건을 모두 만족할 때만 광고 노출 시도
    if (isTimePassed && isPagePassed) {
      try {
        // 1. 준비된 광고 보여주기
        await AdMob.showInterstitial();
        
        // 🌟 성공적으로 노출됐으므로 시간 갱신 및 페이지 이동 횟수 초기화
        localStorage.setItem('last_interstitial_time', now.toString());
        this.pageMoveCount = 0;
        console.log('✅ 전면 광고 노출 성공! 조건 초기화됨.');

        // 2. [중요] 광고를 보여줬으니, 다음 번을 위해 바로 다시 장전!
        this.loadInterstitial(); 

        return true; // 노출 성공했음을 반환 (로그 쏘기 위함)

      } catch (e) {
        console.log('광고가 아직 준비되지 않았거나 에러 발생. 다시 장전 시도.');
        // 혹시라도 없어서 못 보여줬다면, 다음을 위해 지금이라도 장전
        this.loadInterstitial();
        return false;
      }
    } else {
      // 조건 미달 시 콘솔에 남은 진행도 표시
      console.log(`⏳ 전면 광고 스킵 (남은 시간: ${Math.max(0, 30 - timeDiffSec).toFixed(0)}초, 이동 페이지: ${this.pageMoveCount}/6)`);
      return false; // 노출 스킵했음을 반환
    }
  }
}