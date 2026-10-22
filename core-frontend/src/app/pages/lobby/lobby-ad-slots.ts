// 로비 네이티브 광고 슬롯 6개의 좌표·클리핑 경계 계산 (2-24차 단일 슬롯 → 3슬롯 → 2-34차 6슬롯).
// LobbyPage에서 분리한 이유: 로비 페이지는 Capacitor 플러그인·네이티브 브리지를 import하므로
// Karma에서 통째로 불러오기 어렵다. 순수 함수만 여기에 두고 spec은 이 모듈만 본다.

/**
 * 로비 슬롯 수 — 네이티브 컨테이너(ad_container_1~6 / tag 201~206)와 1:1.
 * 슬롯 번호는 화면 순서가 아니라 템플릿의 data-slot 값이다 (세로형 목록이 짧으면 그 안의 슬롯이 아예 없을 수 있다):
 *   0 = My Global Ranking 위(2-34 3차에 아래→위) · 1 = Trending 아래 · 2 = TOP 100 아래 (2-24차부터 그대로)
 *   3·4·5 = 세로형 Latest Content 안 (2-34차, LOBBY_FEED_AD_SLOTS)
 */
export const LOBBY_AD_SLOT_COUNT = 6;

/** 세로형 Latest Content 안의 광고 슬롯 번호 — 콘텐츠 3·6·9번째 뒤(LATEST_FEED_AD_AFTER, 마지막 콘텐츠 뒤 제외)에 순서대로 대응 */
export const LOBBY_FEED_AD_SLOTS: ReadonlyArray<number> = [3, 4, 5];

/**
 * 슬롯별 클리핑 경계. 슬롯 0·1과 세로형 안의 3~5는 랭킹 탭 바보다 위에 있어 헤더 하단에서 잘리고,
 * 슬롯 2는 sticky 탭 바 아래에 있어 탭 바 하단에서 잘린다 (그렇지 않으면 오버레이가 탭 바를 덮는다)
 */
export const LOBBY_AD_SLOT_BELOW_TAB_BAR: ReadonlyArray<boolean> = [false, false, true, false, false, false];

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
 * @param suppressed 잠시 숨길 슬롯 번호 — 목록에서 빠지므로 네이티브가 화면 밖에 둔다
 *                   (TOP 100 목록이 스켈레톤으로 바뀌는 동안 아래 슬롯이 옛 자리에 남아 겹치던 문제)
 */
export function buildAdSlotPositions(
  slotTops: ReadonlyArray<number | null>,
  headerBottom: number,
  tabBarBottom: number,
  suppressed?: ReadonlySet<number>
): LobbyAdSlotPosition[] {
  const result: LobbyAdSlotPosition[] = [];
  slotTops.forEach((top, index) => {
    if (top === null || top === undefined || index >= LOBBY_AD_SLOT_COUNT) return;
    if (suppressed && suppressed.has(index)) return;
    const hideAbove = LOBBY_AD_SLOT_BELOW_TAB_BAR[index]
      ? Math.max(headerBottom, tabBarBottom)
      : headerBottom;
    result.push({ index, y: top, hideAbove });
  });
  return result;
}

// ==========================================
// 빠른 스크롤 중 광고 페이드 (2026-10-23 클라이언트 10번)
// 네이티브 광고는 웹뷰 위에 따로 얹힌 뷰라 좌표가 브리지를 한 번 건너 1~3프레임 늦게 적용된다.
// 빠르게 넘길 때 그 지연이 "광고와 주변이 미세하게 떨리는" 모습으로 보이므로, 속도가 임계치를 넘으면
// 광고를 투명하게 숨기고 느려지거나 멈추면 제자리에서 다시 나타낸다. 위치 전송 자체는 계속한다.
// ==========================================

/** 이 속도(css px/ms)를 넘으면 페이드 아웃. 체감으로 정한 초기값 — 실기기 확인 뒤 조정 */
export const LOBBY_AD_FADE_ON_VELOCITY = 1.5;
/** 이 속도 아래로 떨어지면 페이드 인 (켜짐·꺼짐 사이 여유를 둬 경계에서 깜빡이지 않게) */
export const LOBBY_AD_FADE_OFF_VELOCITY = 0.5;

/**
 * 스크롤 속도로 다음 페이드 상태를 정한다.
 * @param velocityY ion-content ScrollDetail.velocityY (css px/ms, 부호는 방향)
 * @param faded 현재 페이드 상태
 */
export function nextAdFadeState(velocityY: number | undefined | null, faded: boolean): boolean {
  if (typeof velocityY !== 'number' || !Number.isFinite(velocityY)) return faded;
  const speed = Math.abs(velocityY);
  if (!faded) return speed > LOBBY_AD_FADE_ON_VELOCITY;
  return speed >= LOBBY_AD_FADE_OFF_VELOCITY;
}
