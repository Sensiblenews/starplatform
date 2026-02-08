제시해주신 8가지 항목을 **현재 구축된 슈퍼앱 프로젝트(WITCH Hunting)**의 기술 스택(Spring Legacy + MyBatis + JSP + MySQL + Ionic/Angular)과 비즈니스 로직(SM/LC/ST 권한 체계)에 맞춰 구체화한 「프로젝트 최종 배포 레드라인(Redline)」 문서입니다.

이 문서는 개발팀(혹은 본인)이 배포 직전 절대 넘어서는 안 되는 기술적/관리적 경계선을 의미합니다.

🛑 WITCH Super App - Project Redline (Ver 1.0)
문서 효력: 즉시 발효 대상: WITCH Hunting 서버 및 앱 개발 프로젝트 전체

1. 저장소 및 형상 관리 (Repository & SCM)
[원칙] Backend/Frontend 저장소의 물리적/논리적 분리 확정

Backend (Server): WITCH-Admin-Legacy (Java Spring + JSP + MyBatis)

규칙: /super/* (어드민), /api/* (앱 통신), /star/* (스타 전용) 로직이 모두 포함됨.

Frontend (App): WITCH-App-Ionic (Angular/Ionic)

규칙: 오직 UI/UX와 API 호출만 담당. 비즈니스 로직 포함 금지.

[금지] .metadata, target, build 폴더 커밋 금지 (.gitignore 필수 점검).

2. 런타임 버전 고정 (Version Pinning)
[원칙] 개발 환경과 배포 환경의 버전 불일치로 인한 오류 원천 차단

Java: JDK 1.8 (또는 11) - 현재 레거시 코드 호환성 유지

Was: Tomcat 8.5.x 또는 9.0.x - 서블릿 스펙 준수

DB: MySQL 8.0.x - utf8mb3 vs utf8mb4 Collation 이슈 방지 목적

[금지] 서버에서 임의로 apt-get upgrade 등을 통해 메이저 버전을 올리는 행위 금지.

3. 환경 설정 분리 (Environment Isolation)
[원칙] 소스 코드 수정 없이 환경(Local/Prod) 전환 가능

현황: Constants.java 내의 _FILE_SAVE_PATH 경로가 주석으로 토글되고 있음.

조치: OS 환경 변수나 외부 프로퍼티(globals.properties)를 통해 경로를 주입받도록 수정하거나, 배포 스크립트에서 자동 처리.

[금지] 배포할 때마다 Java 코드를 주석 처리/해제하여 컴파일하는 행위 금지. (Human Error의 주범)

4. 신규 기능 동결 (Feature Freeze)
[원칙] 현재 시점 이후, 데이터 스키마를 변경하는 기능 추가 전면 중단

허용 범위: CSS 수정, JSP 오타 수정, API 응답 메시지 수정, 치명적 버그 픽스.

금지 범위: 테이블 추가, 컬럼 추가, 새로운 권한(Role) 생성, 결제 모듈 연동 등.

[예외] 앱 안정성을 위한 '강제 업데이트' 기능이나 '공지사항 팝업'은 예외적으로 허용.

5. 광고·수익 로직 무결성 (Ad Integrity)
[원칙] WH_AD_LOG 테이블은 성역(Sanctuary)으로 간주

이 테이블은 향후 정산(돈)과 직결되므로, 조회(SELECT)와 적재(INSERT) 외의 작업은 원칙적으로 금지.

[금지] WH_AD_LOG 테이블에 대한 UPDATE, DELETE 쿼리 실행 절대 금지. 오적립된 데이터라도 '보정 데이터(Minus Log)'를 추가하는 방식으로 처리해야 함.

6. 금액/통계 계산의 서버 위임 (Server-Side Authority)
[원칙] 클라이언트(앱/JSP)는 계산기를 두드리지 않는다.

총 노출 수, 월간 활성 유저(MAU), 국가별 분포 등 모든 숫자는 DB 쿼리(XML)와 Java Service가 계산해서 내려준 값만 찍어야 함.

이유: JS로 계산하면 위변조가 쉽고, 브라우저마다 오차가 발생할 수 있음.

[확인] SuperAdminService.java의 getDashboardStats 메소드가 유일한 진실(Single Source of Truth)이어야 함.

7. 로그 보존 및 감사 (Log Retention)
[원칙] "누가(Who), 언제(When), 무엇을(What)" 했는지 기록 보존

관리자(SM/LC)의 '스타 상태 변경(정지/해제)', '계정 생성' 행위는 반드시 서버 로그(catalina.out) 또는 별도 DB 로그로 남아야 함.

[금지] DB 용량 확보를 이유로 최근 3개월 이내의 WH_AD_LOG나 WH_PRESS 데이터를 임의 삭제하는 행위.

8. CI/CD 자동화 (Automation)
[원칙] "내 컴퓨터에선 되는데요" 방지

로컬 이클립스에서 우클릭 > Export WAR 하는 방식을 지양.

GitHub Actions를 통해 코드가 푸시되면 자동으로 빌드되고, 테스트(단위 테스트가 있다면)를 통과한 후 WAR 파일이 생성되도록 설정.

최소한 Maven Build가 로컬 의존성 없이 독립적으로 성공해야 함.