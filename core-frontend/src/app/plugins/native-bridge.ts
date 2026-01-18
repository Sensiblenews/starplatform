import { registerPlugin } from '@capacitor/core';

export interface NativeBridgePlugin {
  addAdWrapper(): Promise<void>;
  updateAdPosition(options: { value: number; direction: string }): Promise<void>;
  setShow(options: { show: boolean; page: string }): Promise<void>;
  setViewportHeight(options: { height: number }): Promise<void>;
}

const NativeBridge = registerPlugin<NativeBridgePlugin>('NativeBridge');

export default NativeBridge;
