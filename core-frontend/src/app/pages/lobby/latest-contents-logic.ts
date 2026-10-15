// 로비 Latest Contents 숏폼의 순수 로직 (2-33차).
// 컴포넌트에서 분리한 이유: 로비 쪽 컴포넌트는 Capacitor·HttpService 를 끌고 들어와 Karma 에서 통째로
// 불러오기 어렵다. 인덱스 계산·자동 이동 판정·스와이프 판정만 여기 두고 spec 은 이 모듈만 본다.

/** /api/super/lobby/latest-contents 응답 항목 */
export interface LatestContentItem {
  lcId: number;
  conId: number;
  prsId: string;
  starName: string;
  starImage: string | null;
  title: string | null;
  mediaUrl: string;
  thumbUrl: string | null;
  isFeatured: string;
}

/** 서버에 기록하는 이벤트 종류 */
export type LatestContentEventType = 'IMPRESSION' | 'PLAY' | 'CLICK' | 'PROFILE_CLICK';

/** 자동 이동 기본 간격(초). 어드민 설정이 범위를 벗어나면 이 값으로 돌아간다 */
export const LATEST_DEFAULT_INTERVAL_SEC = 4;
/** 어드민 설정 허용 범위(초) — 서버 SuperAdminService.LC_INTERVAL_MIN/MAX 와 같은 값 */
export const LATEST_INTERVAL_MIN_SEC = 3;
export const LATEST_INTERVAL_MAX_SEC = 10;
/** 사용자가 손으로 넘긴 뒤 자동 이동을 멈추는 시간 */
export const LATEST_MANUAL_PAUSE_MS = 6000;
/** 가로 스와이프로 인정하는 최소 이동 거리(px) — VS 캐러셀과 같은 값 */
export const LATEST_SWIPE_THRESHOLD_PX = 40;

/** 다음 인덱스. 마지막에서 첫 카드로 순환한다 */
export function nextIndex(current: number, length: number): number {
  if (!(length > 0)) return 0;
  return (clampIndex(current, length) + 1) % length;
}

/** 이전 인덱스. 첫 카드에서 마지막으로 순환한다 */
export function prevIndex(current: number, length: number): number {
  if (!(length > 0)) return 0;
  return (clampIndex(current, length) - 1 + length) % length;
}

/** 범위 밖·NaN 인덱스를 0..length-1 로 맞춘다 */
export function clampIndex(index: number, length: number): number {
  if (!(length > 0)) return 0;
  if (typeof index !== 'number' || isNaN(index) || index < 0) return 0;
  if (index >= length) return length - 1;
  return Math.floor(index);
}

/**
 * 터치 이동량으로 스와이프 방향을 정한다. 왼쪽으로 끌면(음수) 다음, 오른쪽으로 끌면 이전.
 * 임계값 미만은 탭·흔들림으로 보고 null
 */
export function swipeDirection(deltaX: number, threshold: number = LATEST_SWIPE_THRESHOLD_PX): 'next' | 'prev' | null {
  if (typeof deltaX !== 'number' || isNaN(deltaX)) return null;
  if (Math.abs(deltaX) < threshold) return null;
  return deltaX < 0 ? 'next' : 'prev';
}

/** 자동 이동을 지금 해도 되는지. 마지막 수동 조작 뒤 pauseMs 가 지나야 한다 */
export function canAutoAdvance(now: number, lastManualAt: number | null, pauseMs: number = LATEST_MANUAL_PAUSE_MS): boolean {
  if (lastManualAt === null || lastManualAt === undefined) return true;
  return now - lastManualAt >= pauseMs;
}

/** 서버가 준 간격(초)을 ms 로. 범위(3~10초) 밖이거나 숫자가 아니면 기본 4초 */
export function normalizeIntervalMs(intervalSec: unknown): number {
  const sec = typeof intervalSec === 'number' ? intervalSec : parseInt(String(intervalSec), 10);
  if (isNaN(sec) || sec < LATEST_INTERVAL_MIN_SEC || sec > LATEST_INTERVAL_MAX_SEC) {
    return LATEST_DEFAULT_INTERVAL_SEC * 1000;
  }
  return sec * 1000;
}

/** 세션 안에서 같은 이벤트를 두 번 보내지 않기 위한 키 */
export function eventKey(lcId: number, type: LatestContentEventType): string {
  return `${lcId}:${type}`;
}

/** /api/super/content/stats 응답의 stats (2-34차 플레이어 오른쪽 숫자) */
export interface ContentStats {
  viewCnt: number;
  likeCnt: number;
  commentCnt: number;
  saveCnt: number;
  shareCnt: number;
  liked: boolean;
  saved: boolean;
}

/** 서버 응답(문자열·Y/N 섞임)을 ContentStats 로. 숫자가 아니면 0, 음수도 0 */
export function toContentStats(raw: any): ContentStats {
  const num = (v: any) => {
    const n = typeof v === 'number' ? v : parseInt(String(v), 10);
    return isNaN(n) || n < 0 ? 0 : n;
  };
  const flag = (v: any) => v === true || v === 'Y' || v === 1 || v === '1';
  const r = raw || {};
  return {
    viewCnt: num(r.viewCnt),
    likeCnt: num(r.likeCnt),
    commentCnt: num(r.commentCnt),
    saveCnt: num(r.saveCnt),
    shareCnt: num(r.shareCnt),
    liked: flag(r.liked),
    saved: flag(r.saved),
  };
}

/**
 * 아이콘 아래 숫자 축약 — 999 까지 그대로, 1.2K · 12K · 1.2M. 소수 첫째 자리는 내림(올려서 부풀리지 않는다)
 */
export function compactNumber(value: unknown): string {
  const n = typeof value === 'number' ? value : parseInt(String(value), 10);
  if (isNaN(n) || n <= 0) return '0';
  const fmt = (v: number, unit: string) => {
    const floored = v >= 10 ? Math.floor(v) : Math.floor(v * 10) / 10;
    return `${floored}${unit}`;
  };
  if (n < 1000) return String(Math.floor(n));
  if (n < 1_000_000) return fmt(n / 1000, 'K');
  if (n < 1_000_000_000) return fmt(n / 1_000_000, 'M');
  return fmt(n / 1_000_000_000, 'B');
}
