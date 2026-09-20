import { SimpleChange } from '@angular/core';
import { MyRankData, MyRankingCardComponent } from './my-ranking-card.component';

// 로비 My Global Ranking 카드의 순수 함수·플래시 규칙 검증 (2-29차). TestBed 없이 인스턴스만 만든다.
describe('MyRankingCardComponent 순위 변동 계산', () => {

  it('직전 순위가 없으면(첫 로드) 표시하지 않는다', () => {
    expect(MyRankingCardComponent.computeDelta(null, 15)).toEqual({ dir: 'none', amount: 0 });
  });

  it('현재 순위가 없으면(순위표 밖) 표시하지 않는다', () => {
    expect(MyRankingCardComponent.computeDelta(15, null)).toEqual({ dir: 'none', amount: 0 });
  });

  it('순위가 같으면 유지다', () => {
    expect(MyRankingCardComponent.computeDelta(15, 15)).toEqual({ dir: 'same', amount: 0 });
  });

  it('순위 숫자가 작아지면 상승이고 차이만 담는다', () => {
    expect(MyRankingCardComponent.computeDelta(21, 15)).toEqual({ dir: 'up', amount: 6 });
  });

  it('순위 숫자가 커지면 하락이다', () => {
    expect(MyRankingCardComponent.computeDelta(15, 20)).toEqual({ dir: 'down', amount: 5 });
  });
});

describe('MyRankingCardComponent 저장값·포맷', () => {

  it('저장 키는 starId별로 나뉜다', () => {
    expect(MyRankingCardComponent.lastSeenKey('star_1')).toBe('myRank:lastSeen:star_1');
  });

  it('저장된 순위 문자열을 숫자로 읽는다', () => {
    expect(MyRankingCardComponent.parseStoredRank('15')).toBe(15);
  });

  it('빈 값·비숫자·1 미만은 저장값 없음으로 본다', () => {
    expect(MyRankingCardComponent.parseStoredRank(null)).toBeNull();
    expect(MyRankingCardComponent.parseStoredRank('')).toBeNull();
    expect(MyRankingCardComponent.parseStoredRank('abc')).toBeNull();
    expect(MyRankingCardComponent.parseStoredRank('0')).toBeNull();
  });

  it('지표는 로비와 같은 규칙으로 줄여 쓴다', () => {
    expect(MyRankingCardComponent.formatCompact(999)).toBe('999');
    expect(MyRankingCardComponent.formatCompact(12300)).toBe('12.3K');
    expect(MyRankingCardComponent.formatCompact(3920000)).toBe('3.9M');
    expect(MyRankingCardComponent.formatCompact(undefined)).toBe('0');
  });

  it('전체 대상 수·pts는 천 단위 콤마만 붙인다', () => {
    expect(MyRankingCardComponent.formatWithCommas(8642)).toBe('8,642');
    expect(MyRankingCardComponent.formatWithCommas(null)).toBe('0');
  });
});

describe('MyRankingCardComponent 표시·플래시', () => {

  const data = (globalRank: number | null): MyRankData => ({
    globalRank, totalStars: 1789, score: 8642, viewCount: 1245, likeCnt: 320, followerCnt: 86, name: 'me', image: '',
  });

  beforeEach(() => jasmine.clock().install());
  afterEach(() => jasmine.clock().uninstall());

  it('순위표 밖(globalRank null)이면 순위를 지어내지 않고 —를 보인다', () => {
    const c = new MyRankingCardComponent();
    c.data = data(null);
    expect(c.rankLabel).toBe('—');
    c.data = data(15);
    expect(c.rankLabel).toBe('#15');
  });

  it('스켈레톤은 데이터가 없는 첫 로드에만 보인다', () => {
    const c = new MyRankingCardComponent();
    c.loading = true;
    expect(c.showSkeleton).toBeTrue();
    c.data = data(15);
    expect(c.showSkeleton).toBeFalse();
  });

  it('delta가 상승으로 바뀌면 1.5초 플래시한다', () => {
    const c = new MyRankingCardComponent();
    c.delta = { dir: 'up', amount: 6 };
    c.ngOnChanges({ delta: new SimpleChange(null, c.delta, true) });
    expect(c.flash).toBeTrue();
    jasmine.clock().tick(MyRankingCardComponent.FLASH_MS - 1);
    expect(c.flash).toBeTrue();
    jasmine.clock().tick(1);
    expect(c.flash).toBeFalse();
  });

  it('유지·없음으로 바뀌면 플래시하지 않는다', () => {
    const c = new MyRankingCardComponent();
    c.delta = { dir: 'same', amount: 0 };
    c.ngOnChanges({ delta: new SimpleChange(null, c.delta, true) });
    expect(c.flash).toBeFalse();
  });

  it('다른 입력만 바뀌면 플래시하지 않는다', () => {
    const c = new MyRankingCardComponent();
    c.delta = { dir: 'up', amount: 6 };
    c.ngOnChanges({ data: new SimpleChange(null, data(15), true) });
    expect(c.flash).toBeFalse();
  });

  it('파괴 시 타이머를 정리한다', () => {
    const c = new MyRankingCardComponent();
    c.startFlash();
    c.ngOnDestroy();
    jasmine.clock().tick(MyRankingCardComponent.FLASH_MS);
    // 타이머가 정리되어 flash가 그대로 남는다 (콜백 미실행)
    expect(c.flash).toBeTrue();
  });
});
