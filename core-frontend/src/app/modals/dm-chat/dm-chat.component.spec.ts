import { DmChatComponent } from './dm-chat.component';
import { DmMessage } from 'src/app/services/dm.service';

// 첨부 크기 검사 (2-29차). base64 길이에서 원본 바이트를 계산해 서버 상한(이미지 10MB·영상 30MB)을 앱에서 먼저 거른다
describe('DmChatComponent 첨부 크기 계산', () => {

  it('패딩 없는 base64는 길이의 3/4가 바이트 수다', () => {
    // 'abcd' (4자) → 3바이트
    expect(DmChatComponent.byteLength('YWJj')).toBe(3);
  });

  it('패딩 = 하나는 1바이트, == 는 2바이트를 뺀다', () => {
    expect(DmChatComponent.byteLength('YWI=')).toBe(2);
    expect(DmChatComponent.byteLength('YQ==')).toBe(1);
  });

  it('data URI 접두사는 무시한다', () => {
    expect(DmChatComponent.byteLength('data:image/png;base64,YWJj')).toBe(3);
  });

  it('30MB 영상은 통과하고 그보다 크면 걸린다', () => {
    const limit = 30 * 1024 * 1024;
    const okLen = Math.ceil(limit / 3) * 4; // 정확히 30MB에 해당하는 base64 길이
    expect(DmChatComponent.byteLength('A'.repeat(okLen))).toBe(limit);
    expect(DmChatComponent.byteLength('A'.repeat(okLen + 4))).toBeGreaterThan(limit);
  });
});


// 폴링 병합 (2-29차). 서버가 응답마다 새 토큰 URL을 주더라도 첨부 src는 바뀌지 않아야
// 영상이 처음으로 되돌아가거나 화면이 깜빡이지 않는다 (실기기에서 확인된 문제)
describe('DmChatComponent 폴링 병합', () => {

  const msg = (msgId: number, fileUrl: string, readDate: number | null = null): DmMessage => ({
    msgId, senderId: 'a', contentType: 'VIDEO', text: null, fileUrl, thumbUrl: fileUrl + '&thumb',
    sendDate: 1, readDate, expireAt: 2,
  });

  it('같은 메시지는 첨부 URL과 객체를 그대로 유지한다', () => {
    const prev = [msg(1, '/f?t=AAA')];
    const merged = DmChatComponent.mergeMessages(prev, [msg(1, '/f?t=BBB')]);
    expect(merged[0]).toBe(prev[0]);
    expect(merged[0].fileUrl).toBe('/f?t=AAA');
  });

  it('읽음 상태가 바뀌면 객체는 새로 만들되 첨부 URL은 유지한다', () => {
    const merged = DmChatComponent.mergeMessages([msg(1, '/f?t=AAA')], [msg(1, '/f?t=BBB', 999)]);
    expect(merged[0].readDate).toBe(999);
    expect(merged[0].fileUrl).toBe('/f?t=AAA');
    expect(merged[0].thumbUrl).toBe('/f?t=AAA&thumb');
  });

  it('새 메시지는 그대로 들어오고 사라진 메시지는 빠진다', () => {
    const merged = DmChatComponent.mergeMessages([msg(1, '/f?t=AAA')], [msg(2, '/f?t=CCC')]);
    expect(merged.length).toBe(1);
    expect(merged[0].msgId).toBe(2);
    expect(merged[0].fileUrl).toBe('/f?t=CCC');
  });
});
