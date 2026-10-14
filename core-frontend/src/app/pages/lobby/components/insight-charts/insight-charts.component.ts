import { Component, Input, OnChanges } from '@angular/core';
import { HttpService } from 'src/app/services/http.service';
import {
  BarRect,
  INSIGHT_METRICS,
  INSIGHT_PERIODS,
  InsightPeriod,
  InsightSeriesResponse,
  axisLabels,
  barRects,
  rankLine,
} from '../../insight-chart-logic';
import { compactNumber } from '../../latest-contents-logic';

/** 그래프 한 장을 그릴 준비가 끝난 값 */
interface ChartView {
  key: string;
  title: string;
  color: string;
  kind: 'line' | 'bar';
  summary: string;
  bars: BarRect[];
  line: { d: string; dots: Array<{ x: number; y: number }> };
  labels: Array<{ x: number; text: string }>;
  empty: boolean;
}

/**
 * 로비 별 메뉴 I(Insight) 탭 (2-34차 2-1) — 글로벌 랭킹·방문자·즐겨찾기·좋아요·게시물 그래프 5종을 한눈에.
 * 기간 칩: 매일·매주·매달·3개월·6개월·1년. 데이터는 /api/super/my-insight/series (스타 본인 토큰 필요).
 * 차트 라이브러리 없이 SVG 로 그린다 (좌표 계산은 insight-chart-logic.ts).
 */
@Component({
  selector: 'app-insight-charts',
  templateUrl: './insight-charts.component.html',
  styleUrls: ['./insight-charts.component.scss'],
})
export class InsightChartsComponent implements OnChanges {
  /** 로그인한 스타 ID (토큰은 localStorage 에서 읽는다 — 다른 스타 페이지 API 와 같은 방식) */
  @Input() starId = '';

  readonly periods = INSIGHT_PERIODS;
  /** SVG viewBox 크기 — CSS 로 폭에 맞춰 늘어난다 */
  readonly width = 300;
  readonly height = 90;

  period: InsightPeriod = 'WEEK';
  charts: ChartView[] = [];
  loading = false;
  error: string | null = null;

  private requestSeq = 0;

  constructor(private http: HttpService) { }

  ngOnChanges(): void {
    this.load();
  }

  selectPeriod(period: InsightPeriod): void {
    if (period === this.period && this.charts.length) return;
    this.period = period;
    this.load();
  }

  trackByKey(_: number, chart: ChartView): string {
    return chart.key;
  }

  private load(): void {
    if (!this.starId) return;
    const seq = ++this.requestSeq;
    this.loading = true;
    this.error = null;
    this.http.post<InsightSeriesResponse>('/api/super/my-insight/series', {
      starId: this.starId,
      starToken: localStorage.getItem('starToken') || '',
      period: this.period,
    }).subscribe({
      next: res => {
        if (seq !== this.requestSeq) return; // 기간을 빠르게 바꾼 경우 늦게 온 응답은 버린다
        this.loading = false;
        if (!res || res.result !== 'OK') {
          this.error = (res && res.msg) || 'Insight is not available right now.';
          return;
        }
        this.charts = this.toViews(res);
      },
      error: () => {
        if (seq !== this.requestSeq) return;
        this.loading = false;
        this.error = 'Insight is not available right now.';
      },
    });
  }

  private toViews(res: InsightSeriesResponse): ChartView[] {
    const n = (res.labels || []).length;
    const slot = n > 0 ? this.width / n : 0;
    const labels = axisLabels(res.labels || []).map(l => ({ x: l.index * slot + slot / 2, text: l.text }));
    return INSIGHT_METRICS.map(m => {
      const values = (res.series && res.series[m.key]) || [];
      const isRank = m.kind === 'line';
      const line = isRank ? rankLine(values, this.width, this.height) : { d: '', dots: [] };
      const bars = isRank ? [] : barRects(values, this.width, this.height);
      const total = res.totals ? res.totals[m.key] : 0;
      return {
        key: m.key,
        title: m.title,
        color: m.color,
        kind: m.kind,
        summary: isRank
          ? (res.rankNow ? `#${compactNumber(res.rankNow)} now` : 'Not ranked yet')
          : `${compactNumber(total)} this period`,
        bars,
        line,
        labels,
        empty: isRank ? line.dots.length === 0 : !bars.some(b => b.h > 0),
      };
    });
  }
}
