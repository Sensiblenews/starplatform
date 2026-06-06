import { Injectable } from '@angular/core';
import { Preferences } from '@capacitor/preferences'; // 또는 localStorage 사용

@Injectable({
  providedIn: 'root'
})
export class AdProtectionService {

  private readonly STORAGE_PREFIX = 'AD_CLICK_LOCK_';
  private readonly LOCK_DURATION_MS = 24 * 60 * 60 * 1000; // 24시간

  constructor() { }

  // [판단] 광고를 보여줘도 되는가? (True = 보여줌 / False = 숨김)
  async shouldShowAd(starId: string): Promise<boolean> {
    const key = this.STORAGE_PREFIX + starId;
    const { value } = await Preferences.get({ key });

    if (!value) return true; // 기록 없으면 보여줌

    const lastClicked = parseInt(value, 10);
    const now = Date.now();

    // 24시간이 지났으면 다시 보여줌 (true), 안 지났으면 숨김 (false)
    return (now - lastClicked) > this.LOCK_DURATION_MS;

    // return true; // [임시] 광고 보호 기능 비활성화 (테스트용)
  }

  // [기록] 광고 클릭 시 호출 (24시간 잠금 시작)
  async lockAd(starId: string): Promise<void> {
    const key = this.STORAGE_PREFIX + starId;
    const now = Date.now().toString();

    await Preferences.set({ key, value: now });
    console.log(`[AdProtection] Star ${starId} 광고 24시간 잠금 설정됨`);
  }
  
  // (옵션) 잠금 해제 (테스트용)
  async unlockAd(starId: string): Promise<void> {
    const key = this.STORAGE_PREFIX + starId;
    await Preferences.remove({ key });
  }
}