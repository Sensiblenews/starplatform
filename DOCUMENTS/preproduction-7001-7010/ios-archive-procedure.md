# iOS Archive 절차 (38단계, NOT VERIFIED)

전제: 호기 Apple Developer 팀, 배포 인증서·프로비저닝 프로파일, `GoogleService-Info.plist`(호기 Firebase).

1. 주입: `node factory/inject.js --config factory/platforms/platform-700x.json --apply <checkout> --secrets <.env>`
2. 웹 번들: `cd core-frontend && ionic build && npx cap copy ios`
3. 네이티브 설정 반영: `npx trapeze run trapeze-ios.yml -y` — **한 번만**(멱등 아님, 반복 시 plist 항목 중복)
4. 의존성: `cd ios/App && pod install` (로컬 zip podspec 우회 메모 참조)
5. Xcode: `ionic capacitor open ios` → Signing & Capabilities 에서 팀·번들 ID 확인 → Product > Archive
6. 증빙: Organizer 의 Archive 정보(버전·빌드·번들 ID·팀) 스크린샷, `xcodebuild -exportArchive` 로그, IPA SHA-256.

서명 없는 아카이브(`CODE_SIGNING_ALLOWED=NO xcodebuild archive`) 는 빌드 파이프라인 확인용으로만 쓸 수 있고 스토어 제출은 불가하다.
