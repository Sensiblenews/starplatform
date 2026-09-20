import { shouldRevealTopMeta, TOP_META_REVEAL_PX } from './lobby-reveal';

// Today's TOP 순위·조회수 줄 펼침 판정 (2-29차)
describe('shouldRevealTopMeta', () => {

  it('스크롤 전(0)에는 펼치지 않는다', () => {
    expect(shouldRevealTopMeta(0)).toBeFalse();
  });

  it('임계값과 같으면 펼치지 않는다 (러버밴드·미세 흔들림 방지)', () => {
    expect(shouldRevealTopMeta(TOP_META_REVEAL_PX)).toBeFalse();
  });

  it('임계값을 넘으면 펼친다', () => {
    expect(shouldRevealTopMeta(TOP_META_REVEAL_PX + 1)).toBeTrue();
  });

  it('임계값을 따로 주면 그 값을 쓴다', () => {
    expect(shouldRevealTopMeta(10, 5)).toBeTrue();
    expect(shouldRevealTopMeta(5, 5)).toBeFalse();
  });

  it('숫자가 아닌 값은 펼치지 않는다', () => {
    expect(shouldRevealTopMeta(undefined as any)).toBeFalse();
    expect(shouldRevealTopMeta(NaN)).toBeFalse();
  });
});
