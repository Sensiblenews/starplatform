-- =========================================================
-- ORG(단체) 직군 추가 — VS 카드 AUTO 시드 2장 삽입 (2026-08-13)
--
-- 배경: 클라이언트 요청으로 직군이 7개 → 8개로 확장됨
--       (GLOBAL / STAR / CELEB / BRAND / ORG / UNIV / CITY / MEDIA).
--       WH_PRESS.STAR_CATEGORY 컬럼은 VARCHAR(20)이라 스키마 변경은 없고,
--       VS 카드 순환용 AUTO 카드 2장(GLOBAL/ORG, DAILY/ORG)만 시드한다.
--
-- 적용: 운영 DB에 수동 1회 실행 (vs-battlefield.sql과 동일 절차).
--       두 번 실행하면 ORG 카드가 중복되므로 재실행 금지.
-- 순서: 탭 순서(BRAND 다음 ORG)에 맞춰 기존 PIN_ORDER를 밀고 삽입한다.
--       실행 전 기준: GLOBAL 그룹 1~7, DAILY 그룹 8~14 (vs-battlefield.sql 시드값)
-- =========================================================

-- 1. GLOBAL 그룹의 UNIV 이후 + DAILY 그룹 전체를 한 칸 뒤로 (5~14 → 6~15)
UPDATE WH_VS_CARD SET PIN_ORDER = PIN_ORDER + 1
 WHERE CARD_KIND = 'AUTO' AND PIN_ORDER >= 5;

-- 2. GLOBAL/ORG 카드 삽입 (BRAND=4 다음)
INSERT INTO WH_VS_CARD (CARD_KIND, RANK_TYPE, CATEGORY, PIN_ORDER)
VALUES ('AUTO', 'GLOBAL', 'ORG', 5);

-- 3. DAILY 그룹의 UNIV 이후를 한 칸 뒤로 (13~15 → 14~16)
UPDATE WH_VS_CARD SET PIN_ORDER = PIN_ORDER + 1
 WHERE CARD_KIND = 'AUTO' AND PIN_ORDER >= 13;

-- 4. DAILY/ORG 카드 삽입 (DAILY BRAND=12 다음)
INSERT INTO WH_VS_CARD (CARD_KIND, RANK_TYPE, CATEGORY, PIN_ORDER)
VALUES ('AUTO', 'DAILY', 'ORG', 13);

-- 검증: AUTO 카드 16장, 각 그룹 8장씩 (GLOBAL,STAR,CELEB,BRAND,ORG,UNIV,CITY,MEDIA 순)
-- SELECT RANK_TYPE, CATEGORY, PIN_ORDER FROM WH_VS_CARD
--  WHERE CARD_KIND = 'AUTO' ORDER BY PIN_ORDER;
--
-- 참고: ORG로 분류된 스타가 생기기 전까지 ORG 카드는 기존 로직대로
--       카드 순환에서 자동 제외된다 (TOP2 데이터 없는 카드 스킵).
