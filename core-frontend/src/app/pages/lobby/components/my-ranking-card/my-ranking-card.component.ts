import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';

// GET /api/super/ranking/my-rank 응답을 카드가 쓰는 형태로 정리한 것
export interface MyRankData {
  // 순위표 밖 페이지(IS_STAR='N' 등)는 null — 카드는 순위를 지어내지 않고 '—'를 보인다
  globalRank: number | null;
  totalStars: number;
  // Global Score = 조회 ×1 + 좋아요 ×3 + 즐겨찾기 ×5 (서버 계산, VS 카드 pts와 같은 단위)
  score: number;
  viewCount: number;
  likeCnt: number;
  followerCnt: number;
  name: string;
  image: string;
}

export type RankDeltaDir = 'up' | 'down' | 'same' | 'none';

// 순위 변동. up/down일 때만 화살표·숫자를 보인다 (클라이언트: "6위 상승" 아닌 숫자만)
export interface RankDelta {
  dir: RankDeltaDir;
  amount: number;
}

/**
 * 로비 My Global Ranking 카드 (2-29차).
 *
 * 로그인(스타) 사용자에게는 내 순위 / 전체 대상 수 / 변동 / Global Score / 지표 3종과 버튼 두 개,
 * 비로그인·관리자에게는 같은 셸 안에 페이지 만들기 유도 문구와 버튼 하나를 보인다.
 * 데이터 요청·저장은 로비 페이지가 맡고, 이 컴포넌트는 표시와 변동 플래시만 담당한다.
 */
@Component({
  selector: 'app-my-ranking-card',
  templateUrl: './my-ranking-card.component.html',
  styleUrls: ['./my-ranking-card.component.scss'],
})
export class MyRankingCardComponent implements OnChanges, OnDestroy {
  // 기기에 저장하는 "마지막으로 변동을 보여준 순위"의 localStorage 키 접두어 (starId별)
  static readonly LAST_SEEN_PREFIX = 'myRank:lastSeen:';
  // 테두리 강조·숫자 pop 지속 시간 (클라이언트: 1~2초 후 원래 상태로)
  static readonly FLASH_MS = 1500;

  @Input() loggedIn = false;
  @Input() data: MyRankData | null = null;
  @Input() loading = false;
  @Input() delta: RankDelta = { dir: 'none', amount: 0 };
  @Input() avatar = 'assets/img/defaultImg/avatar.svg';

  @Output() myPage = new EventEmitter<void>();
  @Output() topRankings = new EventEmitter<void>();
  @Output() createPage = new EventEmitter<void>();

  // true인 동안 카드 테두리 강조 + 순위 숫자 pop. FLASH_MS 뒤 자동 해제
  flash = false;
  private flashTimer: any = null;

  // ===== 순수 함수 (spec 대상) =====

  static lastSeenKey(starId: string): string {
    return MyRankingCardComponent.LAST_SEEN_PREFIX + starId;
  }

  /** localStorage 문자열 → 순위. 빈 값·비숫자·1 미만은 "저장값 없음"으로 본다 */
  static parseStoredRank(raw: string | null | undefined): number | null {
    if (raw == null || raw === '') return null;
    const n = Number(raw);
    if (!isFinite(n) || n < 1) return null;
    return Math.floor(n);
  }

  /**
   * 직전 순위와 현재 순위의 변동. 순위는 작을수록 좋으므로 curr < prev 가 상승이다.
   * 어느 한쪽이라도 없으면(첫 로드·순위표 밖) none — 표시하지 않는다.
   */
  static computeDelta(prev: number | null, curr: number | null): RankDelta {
    if (prev == null || curr == null) return { dir: 'none', amount: 0 };
    if (curr === prev) return { dir: 'same', amount: 0 };
    if (curr < prev) return { dir: 'up', amount: prev - curr };
    return { dir: 'down', amount: curr - prev };
  }

  /** 999 / 12.3K / 3.9M — 로비 formatNumber와 같은 규칙 */
  static formatCompact(n: number | null | undefined): string {
    const v = Number(n) || 0;
    if (v >= 1000000) return (v / 1000000).toFixed(1) + 'M';
    if (v >= 1000) return (v / 1000).toFixed(1) + 'K';
    return String(v);
  }

  /** 8,642 — 전체 대상 수·pts는 줄이지 않고 천 단위 콤마만 */
  static formatWithCommas(n: number | null | undefined): string {
    const v = Number(n) || 0;
    return Math.floor(v).toLocaleString('en-US');
  }

  // ===== 템플릿용 =====

  get rankLabel(): string {
    return this.data && this.data.globalRank != null ? '#' + this.data.globalRank : '—';
  }

  get showSkeleton(): boolean {
    // 첫 로드만 스켈레톤. 이후 30초 갱신은 기존 값을 그대로 두고 조용히 바꾼다
    return this.loading && !this.data;
  }

  formatCompact(n: number | null | undefined): string {
    return MyRankingCardComponent.formatCompact(n);
  }

  formatWithCommas(n: number | null | undefined): string {
    return MyRankingCardComponent.formatWithCommas(n);
  }

  ngOnChanges(changes: SimpleChanges): void {
    // 로비가 up/down을 감지했을 때만 새 delta 객체를 넘기므로, 참조가 바뀐 그 순간 1회 플래시한다
    const change = changes['delta'];
    if (change && this.delta && (this.delta.dir === 'up' || this.delta.dir === 'down')) {
      this.startFlash();
    }
  }

  startFlash(): void {
    this.clearFlashTimer();
    this.flash = true;
    this.flashTimer = setTimeout(() => {
      this.flash = false;
      this.flashTimer = null;
    }, MyRankingCardComponent.FLASH_MS);
  }

  ngOnDestroy(): void {
    this.clearFlashTimer();
  }

  onAvatarError(event: any): void {
    if (event && event.target) event.target.src = 'assets/img/defaultImg/avatar.svg';
  }

  private clearFlashTimer(): void {
    if (this.flashTimer) {
      clearTimeout(this.flashTimer);
      this.flashTimer = null;
    }
  }
}
