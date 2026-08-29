-- =========================================================
-- 스타 소개(Bio) + 웹 품질 게이트 (2-27차, 2026-08-28)
--
-- 배경: AdSense 재심사 대응. 스타 랜딩(/star/{id})에 스타별 고유
--       소개문을 노출해 "가치가 별로 없는 콘텐츠" 판정을 피한다.
--       입력은 어드민(/super/star/bio.do) 전용이며 앱에서는 수정하지 않는다.
--
--       WH_PRESS의 STATUS_MSG는 한 줄 상태메시지 용도이고 쓰기 경로가
--       전무해 재활용하지 않는다. 소개문은 신규 컬럼에 둔다.
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 "전"에 실행할 것.
--       미실행 상태로 배포하면 selectStarDetail이 PRS_BIO를 조회하다
--       SQL 에러가 나서 앱 스타 상세가 전면 장애가 된다.
--       기본값이 NULL이라 기존 동작(랜딩·앱)은 변하지 않는다.
-- =========================================================

-- 1. 스타 소개문 -------------------------------------------------
ALTER TABLE WH_PRESS
	ADD COLUMN PRS_BIO TEXT NULL COMMENT '스타 소개문 (웹 랜딩 About 섹션, 어드민 입력)';


-- 2. 웹 품질 게이트 보조 인덱스 ----------------------------------
-- 루트 허브·사이트맵·관련 스타 쿼리가 스타별 승인 게시물 보유 여부를
-- EXISTS (WH_STAR_CONTENT WHERE PRS_ID = ? AND MDR_STATUS = 'APPROVED')로
-- 확인한다. 기존 인덱스는 (MDR_STATUS, CON_ID)뿐이라 PRS_ID 선두 인덱스가
-- 없으면 스타 1.4만+ 규모에서 행마다 풀스캔이 된다.
CREATE INDEX IDX_WH_STAR_CONTENT_PRS_MDR ON WH_STAR_CONTENT (PRS_ID, MDR_STATUS);


-- =========================================================
-- 검증 쿼리
--
-- 컬럼 추가 확인
--   SHOW COLUMNS FROM WH_PRESS LIKE 'PRS_BIO';
--
-- 인덱스 확인
--   SHOW INDEX FROM WH_STAR_CONTENT WHERE Key_name = 'IDX_WH_STAR_CONTENT_PRS_MDR';
--
-- 롤백 (문제 발생 시)
--   ALTER TABLE WH_PRESS DROP COLUMN PRS_BIO;
--   DROP INDEX IDX_WH_STAR_CONTENT_PRS_MDR ON WH_STAR_CONTENT;
-- =========================================================
