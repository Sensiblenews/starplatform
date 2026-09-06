import { LiveNewsItem, LiveNewsTickerComponent } from './live-news-ticker.component';
import { VsCard } from '../vs-carousel/vs-carousel.component';

// 티커 속보 감지 로직 검증 (2-27차).
// 로비의 3초 VS 폴링 응답 두 번을 비교해 역전·추격·점수 변화를 찾아내는 순수 함수만 다룬다.
describe('LiveNewsTickerComponent 이벤트 감지', () => {

  const makeCard = (leftScore: number, rightScore: number, type: 'GLOBAL' | 'DAILY' | 'CUSTOM' = 'DAILY'): VsCard => ({
    vsId: 1,
    type,
    category: 'GLOBAL',
    left: { id: 'a', name: 'MSK', image: '', score: leftScore },
    right: { id: 'b', name: 'Emma', image: '', score: rightScore },
  });

  it('첫 응답(비교 대상 없음)에서는 이벤트가 없다', () => {
    const events = LiveNewsTickerComponent.detectEvents(new Map(), [makeCard(392, 391)]);
    expect(events.length).toBe(0);
  });

  it('점수가 그대로면 이벤트가 없다', () => {
    const prev = LiveNewsTickerComponent.buildSnapshots([makeCard(392, 391)]);
    const events = LiveNewsTickerComponent.detectEvents(prev, [makeCard(392, 391)]);
    expect(events.length).toBe(0);
  });

  it('DAILY 카드 역전은 NEW KING 속보(priority 1)가 된다', () => {
    const prev = LiveNewsTickerComponent.buildSnapshots([makeCard(392, 391)]);
    const events = LiveNewsTickerComponent.detectEvents(prev, [makeCard(392, 393)]);
    expect(events.length).toBe(1);
    expect(events[0].priority).toBe(1);
    expect(events[0].text).toContain('NEW KING');
    expect(events[0].text).toContain('Emma');
  });

  it('GLOBAL 카드 역전은 LEAD CHANGE 속보가 된다', () => {
    const prev = LiveNewsTickerComponent.buildSnapshots([makeCard(392, 391, 'GLOBAL')]);
    const events = LiveNewsTickerComponent.detectEvents(prev, [makeCard(392, 393, 'GLOBAL')]);
    expect(events[0].priority).toBe(1);
    expect(events[0].text).toContain('LEAD CHANGE');
  });

  it('격차가 좁혀지면 추격 속보(priority 2)가 된다', () => {
    const prev = LiveNewsTickerComponent.buildSnapshots([makeCard(395, 390)]);
    const events = LiveNewsTickerComponent.detectEvents(prev, [makeCard(395, 393)]);
    expect(events.length).toBe(1);
    expect(events[0].priority).toBe(2);
    expect(events[0].text).toContain('GAP 2');
  });

  it('격차가 벌어지는 점수 변화는 일반 속보(priority 3)가 된다', () => {
    const prev = LiveNewsTickerComponent.buildSnapshots([makeCard(395, 390)]);
    const events = LiveNewsTickerComponent.detectEvents(prev, [makeCard(398, 390)]);
    expect(events.length).toBe(1);
    expect(events[0].priority).toBe(3);
    expect(events[0].text).toContain('398');
  });

  it('대진(스타 조합)이 바뀐 카드는 비교하지 않는다', () => {
    const prev = LiveNewsTickerComponent.buildSnapshots([makeCard(392, 391)]);
    const next = makeCard(10, 500);
    next.right = { id: 'c', name: 'Daisy', image: '', score: 500 };
    const events = LiveNewsTickerComponent.detectEvents(prev, [next]);
    expect(events.length).toBe(0);
  });

  it('도전자가 없는 카드(right null)는 건너뛴다', () => {
    const solo = makeCard(392, 391);
    solo.right = null;
    expect(LiveNewsTickerComponent.detectEvents(new Map(), [solo]).length).toBe(0);
    expect(LiveNewsTickerComponent.buildSnapshots([solo]).size).toBe(0);
  });
});

// Normal Loop 큐 구성 검증 (2-29차).
// ① 어드민 문구 → ② 카테고리별 #1 → ③ VS 상황 순서와 건너뛰기·탭 타겟을 다룬다.
describe('LiveNewsTickerComponent Normal Loop 큐', () => {

  const card = (vsId: number, type: 'GLOBAL' | 'DAILY' | 'CUSTOM', category: string,
                left: { id: string, name: string, score: number } | null,
                right: { id: string, name: string, score: number } | null): VsCard => ({
    vsId,
    type,
    category,
    left: left ? { ...left, image: '' } : (null as any),
    right: right ? { ...right, image: '' } : null,
  });

  const news = (newsId: number, message: string, targetType: string, targetValue: string | null): LiveNewsItem =>
    ({ newsId, message, targetType, targetValue });

  it('어드민 문구 → 카테고리 #1 → VS 상황 순서로 이어진다', () => {
    const queue = LiveNewsTickerComponent.buildQueue(
      [news(1, 'GLOBAL CHALLENGE START!', 'NONE', null)],
      [card(1, 'GLOBAL', 'BRAND', { id: 's1', name: 'Samsung', score: 50 }, { id: 'a1', name: 'Apple', score: 47 })]
    );
    expect(queue.map(q => q.text)).toEqual([
      'GLOBAL CHALLENGE START!',
      '🏢 BRAND #1 · Samsung',
      '⚔️ LIVE VS · Samsung vs Apple · GAP 3',
    ]);
  });

  it('어드민 문구가 없으면 카테고리 #1부터 시작한다', () => {
    const queue = LiveNewsTickerComponent.buildQueue([], [
      card(1, 'GLOBAL', 'GLOBAL', { id: 't', name: 'Taylor', score: 9 }, null),
    ]);
    expect(queue[0].text).toBe('🌎 GLOBAL #1 · Taylor');
    expect(queue[1].text).toBe('👑 Taylor awaits a challenger');
  });

  it('데이터 없는 카테고리(카드 없음·1위 없음)는 건너뛴다', () => {
    const items = LiveNewsTickerComponent.buildCategoryLeaderItems([
      card(1, 'GLOBAL', 'CITY', null, null),
      card(2, 'GLOBAL', 'UNIV', { id: 'u', name: 'KNU', score: 3 }, null),
    ]);
    expect(items.length).toBe(1);
    expect(items[0].text).toBe('🎓 UNIV #1 · KNU');
  });

  it('Daily King 전체 카드는 DAILY KING #1 한 줄, 데일리 카테고리 카드·커스텀 카드는 ②에 넣지 않는다', () => {
    const items = LiveNewsTickerComponent.buildCategoryLeaderItems([
      card(1, 'DAILY', 'GLOBAL', { id: 'b', name: 'BTS', score: 100 }, { id: 'i', name: 'IU', score: 90 }),
      card(2, 'DAILY', 'STAR', { id: 'b', name: 'BTS', score: 100 }, null),
      card(3, 'CUSTOM', 'GLOBAL', { id: 'x', name: 'X', score: 1 }, { id: 'y', name: 'Y', score: 1 }),
    ]);
    expect(items.map(i => i.text)).toEqual(['👑 DAILY KING #1 · BTS']);
    expect(items[0].target).toEqual({ kind: 'STAR', starId: 'b' });
  });

  it('VS 상황 문장은 해당 카드를 타겟으로 갖는다', () => {
    const items = LiveNewsTickerComponent.buildVsStatusItems([
      card(7, 'CUSTOM', 'GLOBAL', { id: 'x', name: 'X', score: 5 }, { id: 'y', name: 'Y', score: 2 }),
    ]);
    expect(items[0].text).toBe('⚔️ LIVE VS · X vs Y · GAP 3');
    expect(items[0].target).toEqual({ kind: 'VS', vsId: 7 });
  });

  it('어드민 문구의 타겟은 종류별로 매핑되고 빈 값·모르는 종류는 NONE이 된다', () => {
    expect(LiveNewsTickerComponent.newsTarget(news(1, 'a', 'STAR', 'abc'))).toEqual({ kind: 'STAR', starId: 'abc' });
    expect(LiveNewsTickerComponent.newsTarget(news(1, 'a', 'VS', '12'))).toEqual({ kind: 'VS', vsId: 12 });
    expect(LiveNewsTickerComponent.newsTarget(news(1, 'a', 'URL', 'https://x.y'))).toEqual({ kind: 'URL', url: 'https://x.y' });
    expect(LiveNewsTickerComponent.newsTarget(news(1, 'a', 'NONE', null))).toEqual({ kind: 'NONE' });
    expect(LiveNewsTickerComponent.newsTarget(news(1, 'a', 'VS', 'abc'))).toEqual({ kind: 'NONE' });
    expect(LiveNewsTickerComponent.newsTarget(news(1, 'a', 'RANKING', 'x'))).toEqual({ kind: 'NONE' });
  });

  it('빈 문구는 큐에 넣지 않는다', () => {
    expect(LiveNewsTickerComponent.buildNewsItems([news(1, '   ', 'NONE', null)]).length).toBe(0);
  });

  it('속보 이벤트는 VS 카드 타겟을 갖는다', () => {
    const before = card(3, 'DAILY', 'GLOBAL', { id: 'a', name: 'A', score: 10 }, { id: 'b', name: 'B', score: 9 });
    const after = card(3, 'DAILY', 'GLOBAL', { id: 'a', name: 'A', score: 10 }, { id: 'b', name: 'B', score: 11 });
    const events = LiveNewsTickerComponent.detectEvents(LiveNewsTickerComponent.buildSnapshots([before]), [after]);
    expect(events[0].target).toEqual({ kind: 'VS', vsId: 3 });
  });
});
