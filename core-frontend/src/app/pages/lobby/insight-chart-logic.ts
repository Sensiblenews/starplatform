// 로비 별 메뉴 I(Insight) 탭 그래프의 순수 계산 (2-34차 2-1).
// 차트 라이브러리를 들이지 않고 SVG 로 그린다(사용자 확정) — 막대·꺾은선 좌표만 여기서 만들고 spec 은 이 모듈만 본다.

/** /api/super/my-insight/series 응답 */
export interface InsightSeriesResponse {
  result: string;
  period: string;
  labels: string[];
  series: { [metric: string]: Array<number | null> };
  totals: { [metric: string]: number };
  rankNow: number | null;
  msg?: string;
}

export type InsightPeriod = 'DAY' | 'WEEK' | 'MONTH' | 'M3' | 'M6' | 'Y1';

/** 기간 칩 — 요청서 "매일 매주 매달 3개월 6개월 1년" */
export const INSIGHT_PERIODS: ReadonlyArray<{ key: InsightPeriod; label: string }> = [
  { key: 'DAY', label: 'Daily' },
  { key: 'WEEK', label: 'Weekly' },
  { key: 'MONTH', label: 'Monthly' },
  { key: 'M3', label: '3M' },
  { key: 'M6', label: '6M' },
  { key: 'Y1', label: '1Y' },
];

/** 그래프 5종 — 요청서 순서(글로벌 랭킹·방문자·즐겨찾기·좋아요·게시물) */
export const INSIGHT_METRICS: ReadonlyArray<{ key: string; title: string; color: string; kind: 'line' | 'bar' }> = [
  { key: 'rank', title: 'Global Ranking', color: '#7c4dff', kind: 'line' },
  { key: 'visits', title: 'Visitors', color: '#ff4d6d', kind: 'bar' },
  { key: 'follows', title: 'Favorites', color: '#ffb703', kind: 'bar' },
  { key: 'likes', title: 'Likes', color: '#e5383b', kind: 'bar' },
  { key: 'posts', title: 'Posts', color: '#219ebc', kind: 'bar' },
];

export interface BarRect { x: number; y: number; w: number; h: number; }

/**
 * 막대 좌표. 0 이상 값만, 최댓값이 높이를 꽉 채운다. 값이 전부 0 이면 높이 0 막대(바닥선만).
 * 막대 사이 간격은 칸 폭의 25%
 */
export function barRects(values: ReadonlyArray<number | null>, width: number, height: number): BarRect[] {
  const n = values.length;
  if (n === 0) return [];
  const nums = values.map(v => (typeof v === 'number' && v > 0 ? v : 0));
  const max = Math.max(...nums);
  const slot = width / n;
  const w = Math.max(1, slot * 0.75);
  return nums.map((v, i) => {
    const h = max > 0 ? (v / max) * height : 0;
    return { x: i * slot + (slot - w) / 2, y: height - h, w, h };
  });
}

/**
 * 순위 꺾은선 — 순위는 작을수록 위(1등이 맨 위). null 칸은 선을 끊는다.
 * 반환: SVG path d 문자열 (M/L). 값이 하나뿐인 칸은 점으로 그리도록 dots 로도 돌려준다
 */
export function rankLine(values: ReadonlyArray<number | null>, width: number, height: number): { d: string; dots: Array<{ x: number; y: number }> } {
  const n = values.length;
  const present = values.filter((v): v is number => typeof v === 'number' && v > 0);
  if (n === 0 || present.length === 0) return { d: '', dots: [] };
  const best = Math.min(...present);
  const worst = Math.max(...present);
  const span = worst - best;
  const slot = width / n;
  const pad = 4;
  const yOf = (v: number) => (span === 0 ? height / 2 : pad + ((v - best) / span) * (height - pad * 2));
  let d = '';
  let drawing = false;
  const dots: Array<{ x: number; y: number }> = [];
  values.forEach((v, i) => {
    if (typeof v !== 'number' || v <= 0) {
      drawing = false;
      return;
    }
    const x = i * slot + slot / 2;
    const y = yOf(v);
    d += `${drawing ? 'L' : 'M'}${round(x)} ${round(y)} `;
    drawing = true;
    dots.push({ x: round(x), y: round(y) });
  });
  return { d: d.trim(), dots };
}

function round(v: number): number {
  return Math.round(v * 10) / 10;
}

/** 축 아래 라벨 — 첫·가운데·끝 세 개만 (칸이 많은 3M·1Y 에서도 겹치지 않게) */
export function axisLabels(labels: ReadonlyArray<string>): Array<{ index: number; text: string }> {
  const n = labels.length;
  if (n === 0) return [];
  const picks = n <= 2 ? Array.from({ length: n }, (_, i) => i) : [0, Math.floor((n - 1) / 2), n - 1];
  return picks.map(i => ({ index: i, text: labels[i] }));
}
