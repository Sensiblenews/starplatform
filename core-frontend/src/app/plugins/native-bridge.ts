import { registerPlugin } from '@capacitor/core';

export interface NativeBridgePlugin {
  addAdWrapper(): Promise<void>;
  updateAdPosition(options: { value: number; direction: string }): Promise<void>;
  setShow(options: { show: boolean; page: string }): Promise<void>;
  // 로비 광고 슬롯: 웹이 플레이스홀더의 뷰포트 기준 y좌표와 sticky 탭 바 하단(hideAbove)을 전달
  setSlotPosition(options: { y: number; hideAbove: number }): Promise<void>;
  setViewportHeight(options: { height: number }): Promise<void>;
  getInstallReferrer(): Promise<{ referrer: string }>;
}

const NativeBridge = registerPlugin<NativeBridgePlugin>('NativeBridge');

export default NativeBridge;
