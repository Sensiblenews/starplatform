import { registerPlugin } from '@capacitor/core';

export interface NativeBridgePlugin {
  addAdWrapper(): Promise<void>;
  updateAdPosition(options: { value: number; direction: string }): Promise<void>;
  setShow(options: { show: boolean; page: string }): Promise<void>;
  // 로비 광고 슬롯(3개): 웹이 펼쳐진 슬롯마다 플레이스홀더의 뷰포트 기준 y좌표와
  // 클리핑 경계(hideAbove — 헤더 하단 또는 sticky 탭 바 하단)를 한 번에 전달한다.
  // 목록에 없는 슬롯은 네이티브가 숨긴다
  setSlotPositions(options: { slots: { index: number; y: number; hideAbove: number }[] }): Promise<void>;
  setViewportHeight(options: { height: number }): Promise<void>;
  getInstallReferrer(): Promise<{ referrer: string }>;
}

const NativeBridge = registerPlugin<NativeBridgePlugin>('NativeBridge');

export default NativeBridge;
