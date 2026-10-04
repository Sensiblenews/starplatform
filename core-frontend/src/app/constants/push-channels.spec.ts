import { LEGACY_PUSH_CHANNELS, PUSH_CHANNEL_BROADCAST, PUSH_CHANNEL_DM, PUSH_CHANNEL_VISITOR } from './push-channels';

// 2-31차 후속: 소리 없이 굳은 옛 채널을 버리고 새 id 로 보낸다
describe('push channel ids', () => {

  it('새 채널 id 는 옛 채널과 겹치지 않는다', () => {
    for (const id of [PUSH_CHANNEL_VISITOR, PUSH_CHANNEL_DM, PUSH_CHANNEL_BROADCAST]) {
      expect(LEGACY_PUSH_CHANNELS).not.toContain(id);
    }
  });

  it('새 채널 id 는 서로 다르다', () => {
    expect(new Set([PUSH_CHANNEL_VISITOR, PUSH_CHANNEL_DM, PUSH_CHANNEL_BROADCAST]).size).toBe(3);
  });

  it('지울 옛 채널 목록에 예전 세 채널이 모두 있다', () => {
    expect(LEGACY_PUSH_CHANNELS).toEqual(jasmine.arrayContaining(['star_visitor_channel', 'dm_channel', '500']));
  });
});
