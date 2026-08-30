import { LiveNewsTickerComponent } from './live-news-ticker.component';
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
