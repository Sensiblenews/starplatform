# 06 식별자·링크 검증표 (47단계) — Config 에서 생성

`factory/platforms/*.json` 의 값. `TODO_` 는 총운영사 자료 대기. 실제 일치 확인(DNS·단말 딥링크)은 도메인·단말이 있어야 하므로 **IN PROGRESS**.

| 호기 | 도메인 | API | 딥링크 호스트 | 커스텀 스킴 | Android 패키지 | iOS 번들 | AASA appID | AdMob 모드 |
|---|---|---|---|---|---|---|---|---|
| master | witch-hunting.com | https://witch-hunting.com | witch-hunting.com | witchhunting | kr.co.sensiblenews.witchHuntingVU2D7F2P7E | kr.co.sensiblenews.witchHunting | VU2D7F2P7E.kr.co.sensiblenews.witchHunting | production |
| 7001 | TODO_DOMAIN_7001 | TODO_API_BASE_URL_7001 | TODO_DOMAIN_7001 | starplatform7001 | kr.co.sensiblenews.platform7001 | kr.co.sensiblenews.platform7001 | TODO_APPLE_TEAM_ID_7001.kr.co.sensiblenews.platform7001 | test |
| 7002 | TODO_DOMAIN_7002 | TODO_API_BASE_URL_7002 | TODO_DOMAIN_7002 | starplatform7002 | kr.co.sensiblenews.platform7002 | kr.co.sensiblenews.platform7002 | TODO_APPLE_TEAM_ID_7002.kr.co.sensiblenews.platform7002 | test |
| 7003 | TODO_DOMAIN_7003 | TODO_API_BASE_URL_7003 | TODO_DOMAIN_7003 | starplatform7003 | kr.co.sensiblenews.platform7003 | kr.co.sensiblenews.platform7003 | TODO_APPLE_TEAM_ID_7003.kr.co.sensiblenews.platform7003 | test |
| 7004 | TODO_DOMAIN_7004 | TODO_API_BASE_URL_7004 | TODO_DOMAIN_7004 | starplatform7004 | kr.co.sensiblenews.platform7004 | kr.co.sensiblenews.platform7004 | TODO_APPLE_TEAM_ID_7004.kr.co.sensiblenews.platform7004 | test |
| 7005 | TODO_DOMAIN_7005 | TODO_API_BASE_URL_7005 | TODO_DOMAIN_7005 | starplatform7005 | kr.co.sensiblenews.platform7005 | kr.co.sensiblenews.platform7005 | TODO_APPLE_TEAM_ID_7005.kr.co.sensiblenews.platform7005 | test |
| 7006 | TODO_DOMAIN_7006 | TODO_API_BASE_URL_7006 | TODO_DOMAIN_7006 | starplatform7006 | kr.co.sensiblenews.platform7006 | kr.co.sensiblenews.platform7006 | TODO_APPLE_TEAM_ID_7006.kr.co.sensiblenews.platform7006 | test |
| 7007 | TODO_DOMAIN_7007 | TODO_API_BASE_URL_7007 | TODO_DOMAIN_7007 | starplatform7007 | kr.co.sensiblenews.platform7007 | kr.co.sensiblenews.platform7007 | TODO_APPLE_TEAM_ID_7007.kr.co.sensiblenews.platform7007 | test |
| 7008 | TODO_DOMAIN_7008 | TODO_API_BASE_URL_7008 | TODO_DOMAIN_7008 | starplatform7008 | kr.co.sensiblenews.platform7008 | kr.co.sensiblenews.platform7008 | TODO_APPLE_TEAM_ID_7008.kr.co.sensiblenews.platform7008 | test |
| 7009 | TODO_DOMAIN_7009 | TODO_API_BASE_URL_7009 | TODO_DOMAIN_7009 | starplatform7009 | kr.co.sensiblenews.platform7009 | kr.co.sensiblenews.platform7009 | TODO_APPLE_TEAM_ID_7009.kr.co.sensiblenews.platform7009 | test |
| 7010 | TODO_DOMAIN_7010 | TODO_API_BASE_URL_7010 | TODO_DOMAIN_7010 | starplatform7010 | kr.co.sensiblenews.platform7010 | kr.co.sensiblenews.platform7010 | TODO_APPLE_TEAM_ID_7010.kr.co.sensiblenews.platform7010 | test |

검증 규칙(validate.js): apiBaseUrl 호스트 = domain, deeplinkHost = domain(경고), 10개 간 패키지·번들·도메인·DB 중복 금지, production 모드에서 TODO_ 광고 ID 금지.
