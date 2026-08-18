-- =========================================================
-- VS 카드 노출 여부 컬럼 추가 (2-25차, 2026-08-19)
--
-- 배경: 클라이언트 요청으로 기본 매치 16장과 커스텀 매치를
--       "삭제하지 않고 잠시 감추는" 기능이 필요해졌다.
--       기존 USE_YN은 커스텀 카드의 소프트 삭제에 이미 쓰이고 있어
--       재사용하면 '숨김'과 '삭제'를 구분할 수 없다. 별도 컬럼으로 분리한다.
--
--       USE_YN     = 존재 여부 (커스텀 삭제 시 'N', AUTO는 항상 'Y')
--       VISIBLE_YN = 노출 여부 (어드민 비노출/재노출 토글 대상)
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 전에 실행할 것.
--       기본값이 'Y'라 기존 카드의 노출 상태는 변하지 않는다.
-- =========================================================

ALTER TABLE WH_VS_CARD
	ADD COLUMN VISIBLE_YN CHAR(1) NOT NULL DEFAULT 'Y' AFTER USE_YN;

-- 검증: 전체 카드가 VISIBLE_YN='Y'인지 확인
-- SELECT VS_ID, CARD_KIND, RANK_TYPE, CATEGORY, PIN_ORDER, USE_YN, VISIBLE_YN
--   FROM WH_VS_CARD ORDER BY PIN_ORDER;
