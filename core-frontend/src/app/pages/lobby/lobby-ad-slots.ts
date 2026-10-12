// 로비 네이티브 광고 슬롯 3개의 좌표·클리핑 경계 계산 (2-24차 단일 슬롯을 3슬롯으로 확장).
// LobbyPage에서 분리한 이유: 로비 페이지는 Capacitor 플러그인·네이티브 브리지를 import하므로
// Karma에서 통째로 불러오기 어렵다. 순수 함수만 여기에 두고 spec은 이 모듈만 본다.

/** 로비 슬롯 수 — 네이티브 컨테이너(ad_container_1~3 / tag 201~203)와 1:1 */
export const LOBBY_AD_SLOT_COUNT = 3;

/**
 * 슬롯별 클리핑 경계. 슬롯 1·2는 랭킹 탭 바보다 위에 있어 헤더 하단에서 잘리고,
 * 슬롯 3은 sticky 탭 바 아래에 있어 탭 바 하단에서 잘린다 (그렇지 않으면 오버레이가 탭 바를 덮는다)
 */
export const LOBBY_AD_SLOT_BELOW_TAB_BAR: ReadonlyArray<boolean> = [false, false, true];

/** 네이티브로 보내는 슬롯 한 개의 위치. 단위는 css px(= iOS pt, Android는 네이티브가 density를 곱한다) */
export interface LobbyAdSlotPosition {
  index: number;
  y: number;
  hideAbove: number;
}

/** 네이티브가 높이를 알려 주지 않을 때(iOS, 구 빌드)의 카드 높이(css px = dp/pt) */
export const LOBBY_AD_SLOT_DEFAULT_HEIGHT = 315;
/** 네이티브가 보낸 높이의 허용 범위 — 비정상 값으로 플레이스홀더가 화면을 삼키지 않게 한다 */
export const LOBBY_AD_SLOT_MIN_HEIGHT = 200;
export const LOBBY_AD_SLOT_MAX_HEIGHT = 420;

/** 모두 접힌 초기 상태 */
export function emptyAdSlotFilled(): boolean[] {
  return new Array(LOBBY_AD_SLOT_COUNT).fill(false);
}

/** 모두 기본 높이인 초기 상태 */
export function defaultAdSlotHeights(): number[] {
  return new Array(LOBBY_AD_SLOT_COUNT).fill(LOBBY_AD_SLOT_DEFAULT_HEIGHT);
}

/**
 * ad_loaded 이벤트의 detail.height(소재 비율로 측정한 카드 높이, Android 2-33차 7번)를 꺼낸다.
 * 없거나 0 이하·숫자 아님이면 기본 315, 범위 밖이면 상·하한으로 자른다
 */
export function slotHeightFromAdLoaded(detail: unknown): number {
  const height = detail && typeof detail === 'object' ? (detail as { height?: unknown }).height : undefined;
  if (typeof height !== 'number' || !Number.isFinite(height) || height <= 0) return LOBBY_AD_SLOT_DEFAULT_HEIGHT;
  return Math.max(LOBBY_AD_SLOT_MIN_HEIGHT, Math.min(LOBBY_AD_SLOT_MAX_HEIGHT, Math.round(height)));
}

/**
 * ad_loaded 이벤트의 detail에서 슬롯 번호를 꺼낸다.
 * 구 네이티브 빌드는 detail 없이 보내므로 그때는 첫 슬롯으로 간주한다. 범위 밖 값은 -1
 */
export function slotIndexFromAdLoaded(detail: unknown): number {
  if (detail === null || detail === undefined) return 0;
  const slot = (detail as { slot?: unknown }).slot;
  if (typeof slot !== 'number' || !Number.isInteger(slot)) return 0;
  return slot >= 0 && slot < LOBBY_AD_SLOT_COUNT ? slot : -1;
}

/**
 * 펼쳐진(광고가 채워진) 슬롯만 골라 네이티브에 보낼 좌표 목록을 만든다.
 * @param slotTops   슬롯별 플레이스홀더 상단의 뷰포트 기준 y. 접힌 슬롯은 null
 * @param headerBottom 헤더 하단 y
 * @param tabBarBottom sticky 탭 바 하단 y (탭 바가 아직 고정되지 않았으면 슬롯보다 위라 자연히 무해)
 */
export function buildAdSlotPositions(
  slotTops: ReadonlyArray<number | null>,
  headerBottom: number,
  tabBarBottom: number
): LobbyAdSlotPosition[] {
  const result: LobbyAdSlotPosition[] = [];
  slotTops.forEach((top, index) => {
    if (top === null || top === undefined || index >= LOBBY_AD_SLOT_COUNT) return;
    const hideAbove = LOBBY_AD_SLOT_BELOW_TAB_BAR[index]
      ? Math.max(headerBottom, tabBarBottom)
      : headerBottom;
    result.push({ index, y: top, hideAbove });
  });
  return result;
}
