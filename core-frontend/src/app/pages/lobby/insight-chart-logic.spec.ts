import { INSIGHT_METRICS, INSIGHT_PERIODS, axisLabels, barRects, rankLine } from './insight-chart-logic';

describe('insight-chart-logic (2-34차 별 메뉴 I 탭)', () => {
  it('그래프는 요청서 순서 5종, 기간은 6개', () => {
    expect(INSIGHT_METRICS.map(m => m.key)).toEqual(['rank', 'visits', 'follows', 'likes', 'posts']);
    expect(INSIGHT_PERIODS.map(p => p.key)).toEqual(['DAY', 'WEEK', 'MONTH', 'M3', 'M6', 'Y1']);
  });

  describe('barRects', () => {
    it('최댓값이 높이를 채우고 나머지는 비례', () => {
      const r = barRects([0, 5, 10], 300, 100);
      expect(r.length).toBe(3);
      expect(r[2].h).toBe(100);
      expect(r[2].y).toBe(0);
      expect(r[1].h).toBe(50);
      expect(r[0].h).toBe(0);
    });

    it('전부 0·null·음수면 높이 0', () => {
      expect(barRects([0, null, -3], 90, 50).every(b => b.h === 0)).toBe(true);
    });

    it('빈 값은 빈 배열', () => {
      expect(barRects([], 100, 100)).toEqual([]);
    });
  });

  describe('rankLine', () => {
    it('1등이 위(작은 y), null 칸에서 선이 끊긴다', () => {
      const line = rankLine([10, 5, null, 20], 400, 100);
      expect(line.dots.length).toBe(3);
      expect(line.dots[1].y).toBeLessThan(line.dots[0].y); // 5위가 10위보다 위
      expect(line.dots[2].y).toBeGreaterThan(line.dots[0].y); // 20위가 가장 아래
      expect((line.d.match(/M/g) || []).length).toBe(2); // 끊긴 뒤 새로 시작
    });

    it('순위가 하나도 없으면 빈 선', () => {
      expect(rankLine([null, null], 100, 100)).toEqual({ d: '', dots: [] });
    });

    it('순위가 모두 같으면 가운데', () => {
      const line = rankLine([3, 3], 100, 80);
      expect(line.dots.every(p => p.y === 40)).toBe(true);
    });
  });

  it('axisLabels — 첫·가운데·끝', () => {
    expect(axisLabels(['a', 'b', 'c', 'd', 'e'])).toEqual([{ index: 0, text: 'a' }, { index: 2, text: 'c' }, { index: 4, text: 'e' }]);
    expect(axisLabels(['a'])).toEqual([{ index: 0, text: 'a' }]);
    expect(axisLabels([])).toEqual([]);
  });
});
