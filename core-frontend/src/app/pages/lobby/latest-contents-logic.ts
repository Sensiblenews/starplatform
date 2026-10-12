// 로비 Latest Contents 숏폼 캐러셀의 순수 로직 (2-33차).
// 컴포넌트에서 분리한 이유: 로비 쪽 컴포넌트는 Capacitor·HttpService 를 끌고 들어와 Karma 에서 통째로
// 불러오기 어렵다. 인덱스 계산·자동 이동 판정·src 지연 바인딩 규칙만 여기 두고 spec 은 이 모듈만 본다.

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
/** 활성 카드 양옆 몇 장까지 영상 src 를 붙여 둘지 */
export const LATEST_SRC_RADIUS = 1;
/** 캐러셀 높이 하한·상한(px) — 아주 작은 화면과 태블릿에서 반쪽 높이가 과해지는 것을 막는다 */
export const LATEST_HEIGHT_MIN_PX = 280;
export const LATEST_HEIGHT_MAX_PX = 520;

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

/** 가로 스크롤 위치로 가장 가까운 카드 인덱스를 구한다. step = 카드 폭 + 간격 */
export function indexFromScroll(scrollLeft: number, step: number, length: number): number {
  if (!(step > 0) || !(length > 0)) return 0;
  const raw = (typeof scrollLeft === 'number' && !isNaN(scrollLeft)) ? scrollLeft : 0;
  return clampIndex(Math.round(raw / step), length);
}

/**
 * 이 카드에 영상 src 를 붙여 둘지. 활성 카드와 원형 거리(마지막↔첫 카드도 이웃) radius 이내만 붙인다.
 * 나머지는 포스터만 보여 WebView 가 영상을 여러 개 디코딩하지 않게 한다
 */
export function shouldBindSrc(index: number, active: number, length: number, radius: number = LATEST_SRC_RADIUS): boolean {
  if (!(length > 0)) return false;
  if (index < 0 || index >= length) return false;
  const direct = Math.abs(index - active);
  const circular = Math.min(direct, length - direct);
  return circular <= radius;
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

/**
 * 캐러셀 높이 = 헤더를 뺀 콘텐츠 높이의 절반, 상·하한으로 자른다.
 * contentHeight 는 ion-content 의 clientHeight(fullscreen 이면 헤더 아래 영역까지 포함), offsetTop 은 헤더 높이
 */
export function halfContentHeight(
  contentHeight: number,
  offsetTop: number,
  min: number = LATEST_HEIGHT_MIN_PX,
  max: number = LATEST_HEIGHT_MAX_PX
): number {
  const h = typeof contentHeight === 'number' && !isNaN(contentHeight) ? contentHeight : 0;
  const top = typeof offsetTop === 'number' && !isNaN(offsetTop) && offsetTop > 0 ? offsetTop : 0;
  const half = Math.round((h - top) * 0.5);
  if (!(half > 0)) return min;
  return Math.max(min, Math.min(max, half));
}
