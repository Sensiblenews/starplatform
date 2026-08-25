import { Injectable } from '@angular/core';
import { Device } from '@capacitor/device';

/**
 * 기기 식별자 캐시.
 *
 * Device.getId()는 Capacitor 네이티브 브리지를 왕복하므로 Android에서 수십~수백 ms가 든다.
 * 페이지마다 진입 직후 await하면 그 시간만큼 첫 API 요청이 밀린다.
 * 기기 ID는 앱 실행 중 바뀌지 않으므로 한 번만 조회해 두고 이후에는 동기적으로 꺼내 쓴다(2-26차).
 */
@Injectable({ providedIn: 'root' })
export class DeviceIdService {

  private cachedId: string = '';
  // 동시에 여러 화면이 요청해도 브리지 호출은 1회만 나가도록 진행 중인 Promise를 공유한다
  private pending: Promise<string> | null = null;

  /** 이미 조회해 둔 값. 아직 없으면 빈 문자열 */
  get(): string {
    return this.cachedId;
  }

  /** 캐시가 있으면 즉시, 없으면 브리지를 한 번 호출해 채운 뒤 반환한다 */
  resolve(): Promise<string> {
    if (this.cachedId) {
      return Promise.resolve(this.cachedId);
    }
    if (!this.pending) {
      this.pending = Device.getId()
        .then(info => {
          this.cachedId = (info && info.identifier) || '';
          return this.cachedId;
        })
        .catch(() => '')
        .then(id => {
          this.pending = null;
          return id;
        });
    }
    return this.pending;
  }

  /** 앱 기동 시 미리 채워두기 위한 호출. 실패해도 무시한다 */
  preload(): void {
    this.resolve();
  }
}
