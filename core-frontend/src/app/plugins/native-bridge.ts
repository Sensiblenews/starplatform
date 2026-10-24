import { registerPlugin } from '@capacitor/core';

export interface NativeBridgePlugin {
  addAdWrapper(): Promise<void>;
  updateAdPosition(options: { value: number; direction: string }): Promise<void>;
  setShow(options: { show: boolean; page: string }): Promise<void>;
  // 로비 광고 슬롯(6개): 웹이 펼쳐진 슬롯마다 플레이스홀더의 뷰포트 기준 y좌표와
  // 클리핑 경계(hideAbove — 헤더 하단 또는 sticky 탭 바 하단)를 한 번에 전달한다.
  // 목록에 없는 슬롯은 네이티브가 숨긴다. faded 가 true 면 빠른 스크롤 중이라 광고를 투명하게 둔다(위치는 계속 갱신).
  // 구 네이티브 빌드는 faded 를 모르고 무시한다.
  // scrollTop 은 좌표를 잰 순간의 ion-content scrollTop — iOS 는 이후 실제 스크롤 위치와의 차이만큼 보정한다(Android·구 빌드는 무시)
  setSlotPositions(options: { slots: { index: number; y: number; hideAbove: number }[]; faded?: boolean; scrollTop?: number }): Promise<void>;
  setViewportHeight(options: { height: number }): Promise<void>;
  getInstallReferrer(): Promise<{ referrer: string }>;
}

const NativeBridge = registerPlugin<NativeBridgePlugin>('NativeBridge');

export default NativeBridge;
