/**
 * Android 알림 채널 id (2-31차 후속). 서버 FirebaseService 의 CHANNEL_* 와 같은 값이어야 한다.
 *
 * Android 는 한번 만든 채널의 소리·진동을 앱이 바꾸지 못한다. star_visitor_channel 은 2026-06 에 소리 없이
 * 만들어졌고 소리는 2026-09 에 붙었으므로 그 사이 설치된 기기는 계속 무음이었다. id 를 바꿔 새 채널을 만든다.
 */
export const PUSH_CHANNEL_VISITOR = 'star_visitor_channel_v2';
export const PUSH_CHANNEL_DM = 'dm_channel_v2';
export const PUSH_CHANNEL_BROADCAST = 'broadcast_v2';

/** 소리 없이 굳은 옛 채널. 새 채널을 만든 뒤 설정 화면에서 지운다 */
export const LEGACY_PUSH_CHANNELS: readonly string[] = ['star_visitor_channel', 'dm_channel', '500'];
