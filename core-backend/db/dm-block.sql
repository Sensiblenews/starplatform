-- =========================================================
-- 1:1 메신저 사용자 차단 (2-28차, 2026-09-11)
--
-- 배경: 요청서의 "신고 · 차단 · 신고내용 보존 · Admin 제재" 중 차단 부분.
--       Apple 심사 가이드라인 1.2는 신고 기능과 함께 "차단한 사용자 목록 관리 및
--       해제 기능"을 점검하므로 목록·해제 화면까지 있어야 한다.
--
-- 기존 WH_COMMENT_BLOCK을 재사용할 수 없다: 그쪽은 DEVICE_ID(익명 게스트) 기준이고
--       메신저는 PRS_ID(스타 계정) 기준이다. 같은 이유로 WH_DM_REPORT도 따로 만들었다.
--
-- 차단 방향: 저장은 단방향(누가 누구를)이지만 효과는 양방향이다.
--       A가 B를 차단하면 둘 중 누구도 상대에게 메시지를 전달할 수 없다.
--
-- 차단당한 쪽에게는 알리지 않는다 (보복·마찰 방지).
--       요청서가 제안한 "전송은 성공한 척하고 저장은 안 하는" 방식(Silent Drop)은
--       이 앱에서 쓸 수 없다. 대화가 서버 조회 결과로만 그려지므로 저장하지 않으면
--       보낸 사람 화면에서도 3초 뒤 폴링 때 말풍선이 사라져 오히려 고장처럼 보인다.
--
--       대신 차단 관계면 findPeer가 null을 돌려주고, 호출부가 이를
--       "This star is not available."로 옮긴다. 이 문구는 상대가 계정을 내렸거나
--       스타가 아닐 때 나오는 것과 완전히 같다 — 차단당한 쪽은 차단인지 탈퇴인지
--       구분할 수 없고, 보낸 메시지가 사라지는 이상 동작도 없다.
--
-- 조회 규칙 (superapp.xml) — 세 곳 모두 양방향으로 가린다.
--       한쪽만 가리면 남은 쪽에 미읽음 뱃지와 마지막 메시지 미리보기가 남는데
--       정작 방을 열면 내용이 안 보여 고장처럼 보인다.
--       메시지가 5분이면 사라지는 앱이라 방이 없어지는 것 자체는 폭파와 구분되지 않는다.
--       selectDmRooms       : 차단 관계인 상대는 목록에서 제외
--       selectDmMessages    : 차단 관계면 대화 내용 전체를 제외
--       selectDmUnreadCount : 같은 기준으로 미읽음에서 제외
--
-- 적용: 운영 DB에 수동 1회 실행. 애플리케이션 배포 "전"에 실행할 것.
--       같은 차수의 db/dm-report.sql과 함께 적용한다.
-- =========================================================

CREATE TABLE WH_DM_BLOCK (
	BLK_ID          BIGINT      NOT NULL AUTO_INCREMENT,
	BLOCKER_PRS_ID  VARCHAR(50) NOT NULL COMMENT '차단한 스타 (WH_PRESS.PRS_ID)',
	BLOCKED_PRS_ID  VARCHAR(50) NOT NULL COMMENT '차단당한 스타',
	CREATED_DATE    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (BLK_ID),
	UNIQUE KEY UK_WH_DM_BLOCK_PAIR (BLOCKER_PRS_ID, BLOCKED_PRS_ID)
) DEFAULT CHARSET=utf8mb4;

-- 역방향 조회: "나를 차단한 사람"을 찾을 때 쓴다 (차단 효과가 양방향이라 필수)
CREATE INDEX IDX_WH_DM_BLOCK_BLOCKED ON WH_DM_BLOCK (BLOCKED_PRS_ID);


-- =========================================================
-- 검증 쿼리
--   SHOW CREATE TABLE WH_DM_BLOCK;
--
--   -- 차단 건수
--   SELECT COUNT(*) FROM WH_DM_BLOCK;
--
--   -- 서로 차단한 쌍 (정상적인 상태다. 해제는 각자 해야 한다)
--   SELECT A.BLOCKER_PRS_ID, A.BLOCKED_PRS_ID
--     FROM WH_DM_BLOCK A
--     JOIN WH_DM_BLOCK B
--       ON A.BLOCKER_PRS_ID = B.BLOCKED_PRS_ID
--      AND A.BLOCKED_PRS_ID = B.BLOCKER_PRS_ID;
--
--   -- 가장 많이 차단당한 스타 (신고와 교차 확인하면 상습범이 보인다)
--   SELECT BLOCKED_PRS_ID, COUNT(*) AS CNT
--     FROM WH_DM_BLOCK GROUP BY BLOCKED_PRS_ID ORDER BY CNT DESC LIMIT 20;
--
-- 롤백 (문제 발생 시)
--   DROP TABLE WH_DM_BLOCK;
--   조회 쿼리의 차단 조건은 테이블이 없으면 에러가 나므로
--   롤백 시에는 애플리케이션도 함께 되돌려야 한다.
-- =========================================================
