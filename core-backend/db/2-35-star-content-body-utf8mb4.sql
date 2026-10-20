-- =========================================================
-- 스타 게시물 본문 이모지·2만자 대응 (2-35차, 2026-10-20)
--
-- 배경: WH_STAR_CONTENT.CON_BODY 가 utf8mb3 TEXT 다.
--       1) utf8mb3 은 4바이트 문자(📰 🌍 🎬 등 대부분의 이모지)를 저장하지 못한다.
--          이모지를 붙여넣은 글은 저장 단계에서 실패한다 (클라이언트 보고 "이모지 복붙").
--       2) TEXT 는 65,535 바이트다. 2-35차 상한 20,000자는 이모지만으로 채우면
--          80,000 바이트라 넘친다. MEDIUMTEXT(16MB)로 올린다.
--
-- 범위: CON_BODY 컬럼만 바꾼다. 테이블 전체(CONVERT TO)를 바꾸지 않는 이유:
--       PRS_ID 는 WH_PRESS.PRS_ID 를 참조하는 외래키라 문자셋이 달라지면 FK 가 깨진다.
--
-- 적용: 운영 DB 에 수동 1회 실행. 앱·서버 배포 전후 어느 쪽이든 무방하다
--       (컬럼 이름·용도는 그대로라 기존 코드와 호환). 행 수가 적어(2026-10 기준 약 160건)
--       테이블 복사 방식 ALTER 여도 수 초 안에 끝난다. 그동안 글 작성만 잠깐 대기한다.
--
-- 확인: 실행 후 SHOW CREATE TABLE WH_STAR_CONTENT; 에서
--       `CON_BODY` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci 이면 완료.
-- =========================================================

ALTER TABLE WH_STAR_CONTENT
	MODIFY COLUMN CON_BODY MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '게시글 내용 (옵션)';
