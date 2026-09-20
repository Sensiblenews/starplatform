// Today's TOP 순위·조회수 줄 펼침 판정 (2-29차 첫 화면 공간 확보).
// LobbyPage에서 분리한 이유: 로비 페이지는 Capacitor 플러그인·네이티브 브리지를 import하므로
// Karma에서 통째로 불러오기 어렵다. 순수 함수만 여기에 두고 spec은 이 모듈만 본다.

/** 세로 스크롤이 이 값(px)을 넘으면 1회 펼친다 — 러버밴드·미세 흔들림은 걸러낸다 */
export const TOP_META_REVEAL_PX = 24;

/** 의도된 스크롤인지 판정 */
export function shouldRevealTopMeta(scrollTop: number, threshold: number = TOP_META_REVEAL_PX): boolean {
  return typeof scrollTop === 'number' && scrollTop > threshold;
}
