# Validation 테스트 픽스처 (24·44단계 증빙)

`invalid-*.json` 은 반드시 FAIL 이 나야 하는 설정, `valid-strict.json` 은 `--strict` 에서도 PASS 가 나야 하는 완전한 설정이다.
`node factory/validate.js factory/test-fixtures/invalid-*.json` 의 종료코드는 1, `node factory/validate.js --strict factory/test-fixtures/valid-strict.json` 은 0 이어야 한다.
`factory.sh selftest` 가 이 두 가지를 자동으로 확인한다.
