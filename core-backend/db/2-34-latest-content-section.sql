-- =========================================================
-- 로비 편성 구역 추가 — 세로형 Latest Content · 가로형 Hot Content (2-34차 1-2, 2026-10-15)
--
-- 배경: 2-33차 숏폼 편성(WH_LATEST_CONTENT)을 그대로 재사용해 로비에 편성 구역 두 개를 더 만든다.
--       구역 이름과 테이블 이름이 엇갈리니 주의:
--         SHORTS = 로비 맨 위 "Star Shorts" 숏폼 (2-33차의 기존 행 전부)
--         LATEST = 첫 네이티브 광고와 Today's TOP 사이 세로형 "Latest Content"
--         HOT    = 그 아래 가로형 미니 숏폼 "Hot Content"
--       노출 개수·자동 이동 간격은 구역마다 따로 둔다. 설정 테이블은 ID 로 구분한다
--         (1=SHORTS 기존 행, 2=LATEST, 3=HOT — SuperAdminService.latestContentSettingId 와 같은 값).
--       이벤트 테이블은 LC_ID 가 이미 구역별 행이라 바꾸지 않는다.
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 "전"에 실행할 것.
--       미실행 상태로 배포하면 Star Shorts 를 포함한 세 구역 조회가 모두 SQL 에러로 실패한다
--       (앱은 조회 실패 시 섹션을 숨기므로 로비 자체는 열린다).
-- =========================================================

ALTER TABLE WH_LATEST_CONTENT
	ADD COLUMN SECTION VARCHAR(10) NOT NULL DEFAULT 'SHORTS' COMMENT 'SHORTS | LATEST | HOT' AFTER LC_ID;

-- 조회는 항상 구역 조건이 붙는다
DROP INDEX IDX_WH_LATEST_CONTENT_ACTIVE ON WH_LATEST_CONTENT;
CREATE INDEX IDX_WH_LATEST_CONTENT_ACTIVE ON WH_LATEST_CONTENT (SECTION, DEL_YN, USE_YN, DISPLAY_ORDER);

INSERT INTO WH_LATEST_CONTENT_SETTING (ID, DISPLAY_COUNT, INTERVAL_SEC) VALUES (2, 10, 4), (3, 10, 4);


-- =========================================================
-- 검증 쿼리
--   SELECT SECTION, COUNT(*) FROM WH_LATEST_CONTENT GROUP BY SECTION;   -- 기존 행은 전부 SHORTS
--   SELECT * FROM WH_LATEST_CONTENT_SETTING;                            -- ID 1·2·3
--
-- 롤백 (문제 발생 시 — 새 구역 편성 행은 함께 지워진다)
--   DELETE FROM WH_LATEST_CONTENT WHERE SECTION <> 'SHORTS';
--   DELETE FROM WH_LATEST_CONTENT_SETTING WHERE ID IN (2, 3);
--   DROP INDEX IDX_WH_LATEST_CONTENT_ACTIVE ON WH_LATEST_CONTENT;
--   CREATE INDEX IDX_WH_LATEST_CONTENT_ACTIVE ON WH_LATEST_CONTENT (DEL_YN, USE_YN, DISPLAY_ORDER);
--   ALTER TABLE WH_LATEST_CONTENT DROP COLUMN SECTION;
-- =========================================================
