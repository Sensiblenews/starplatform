#!/usr/bin/env node
/**
 * Build Injector — platform.config.json 값을 Master 체크아웃의 설정 파일에 주입한다 (의존성 없음, Node 18+).
 *
 *   node factory/inject.js --config factory/platforms/platform-7001.json --out /tmp/out-7001 [--secrets /path/platform-7001/.env]
 *   node factory/inject.js --config factory/platforms/platform-7001.json --apply <checkout> --secrets ...
 *
 * 동작 원리
 *   원본 체크아웃은 Master(platforms/master.json 값이 박힌 상태)다. 각 대상 파일에서 "Master 값 → 대상 값" 치환을 수행하고
 *   치환이 0건이면 오류로 멈춘다(코드가 바뀌어 주입 지점이 사라진 경우를 드리프트로 잡기 위함).
 *   master.json 을 넣으면 출력이 원본과 바이트 단위로 같아야 한다 — 이것이 Injector 자가 검증(factory.sh selftest)이다.
 *
 * --out  : 결과 파일을 <out>/<저장소 상대경로> 로 쓴다. 체크아웃은 건드리지 않는다.
 * --apply: 체크아웃에 덮어쓴다. 단 globals.properties 는 절대 덮어쓰지 않고 <checkout>/factory/.generated/ 에만 쓴다
 *          (운영 설정 파일은 승인 없이 수정 금지 — CLAUDE.md). 서명 키·google-services.json·GoogleService-Info.plist 는 대상이 아니다.
 * --secrets: KEY=VALUE 형식 .env. secretsRef.keys 가 전부 있어야 한다. 없으면 --allow-missing-secrets 로 ${KEY} 자리표시자를 남긴다.
 *
 * 다루지 않는 것(README "한계" 참조): Java 패키지 디렉터리 이름, assetlinks.json 의 서명 지문, 아이콘·스플래시·로고 이미지, 색상 테마.
 */
'use strict';
const fs = require('fs');
const path = require('path');

function arg(name, def) { const i = process.argv.indexOf(name); return i >= 0 ? process.argv[i + 1] : def; }
const has = name => process.argv.includes(name);

const configPath = arg('--config');
const outDir = arg('--out');
const applyDir = arg('--apply');
const secretsPath = arg('--secrets');
const root = path.resolve(arg('--root', applyDir || path.join(__dirname, '..')));
if (!configPath || (!outDir && !applyDir) || (outDir && applyDir)) {
  console.error('usage: inject.js --config <platform.json> (--out <dir> | --apply <checkout>) [--secrets <.env>] [--root <checkout>] [--allow-missing-secrets]');
  process.exit(2);
}

const cfg = JSON.parse(fs.readFileSync(configPath, 'utf8'));
const master = JSON.parse(fs.readFileSync(path.join(__dirname, 'platforms', 'master.json'), 'utf8'));
const GOOGLE_TEST = { // 구글 공개 테스트 단위 (admob.mode=test 일 때 사용)
  bannerAndroid: 'ca-app-pub-3940256099942544/6300978111', bannerIos: 'ca-app-pub-3940256099942544/2934735716',
  interstitialAndroid: 'ca-app-pub-3940256099942544/1033173712', interstitialIos: 'ca-app-pub-3940256099942544/4411468910',
  nativeAndroid: 'ca-app-pub-3940256099942544/2247696110', nativeIos: 'ca-app-pub-3940256099942544/3986624511'
};
const units = cfg.admob.mode === 'test' ? GOOGLE_TEST : cfg.admob.units;

// ---------- 비밀값 ----------
let secrets = {};
if (secretsPath) {
  for (const line of fs.readFileSync(secretsPath, 'utf8').split('\n')) {
    const m = line.match(/^\s*([A-Z][A-Z0-9_]*)\s*=\s*(.*)\s*$/);
    if (m) secrets[m[1]] = m[2].replace(/^"(.*)"$/, '$1');
  }
}
const missing = cfg.secretsRef.keys.filter(k => !(k in secrets));
if (missing.length && !has('--allow-missing-secrets')) {
  console.error(`비밀값 누락 (${secretsPath || '--secrets 미지정'}): ${missing.join(', ')}\n  → --secrets <.env> 를 주거나 --allow-missing-secrets 로 자리표시자를 남긴다`);
  process.exit(1);
}
const sec = k => (k in secrets ? secrets[k] : `\${${k}}`);

// ---------- 치환 규칙 ----------
// 각 항목: { file, rules: [[정규식 또는 문자열, 대체값, 최소 매치 수]] }
const esc = s => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
const q = (s, flags = 'g') => new RegExp(esc(s), flags);
const FE = 'core-frontend', BE = 'core-backend';
const nativeAndroidDirs = [`${FE}/platformConfigurement/android`, `${FE}/android`]; // android/ 는 미추적 작업본(있을 때만)
const nativeIosDirs = [`${FE}/platformConfigurement/ios`, `${FE}/ios`];

const plan = [];
plan.push({ file: `${FE}/trapeze-android.yml`, rules: [
  [/(PACKAGE_NAME:\n\s+default: )\S+/, `$1${cfg.android.packageName}`, 1],
  [/(KAKAO_NATIVE_KEY:\n\s+default: )\S+/, `$1${cfg.sns.kakaoNativeKey}`, 1],
  [/(FACEBOOK_APP_ID:\n\s+default: )\S+/, `$1${cfg.sns.facebookAppId}`, 1],
  [/(FACEBOOK_NATIVE_TOKEN:\n\s+default: )\S+/, `$1${cfg.sns.facebookClientToken}`, 1],
  [/(ADMOB_APP_ID:\n\s+default: )\S+/, `$1${cfg.admob.androidAppId}`, 1],
  [/(DEEPLINK_HOST:\n\s+default: )\S+/, `$1${cfg.deeplinkHost}`, 1],
  [/(\n\s+versionName: )\S+/, `$1${cfg.android.versionName}`, 1],
  [/(\n\s+versionCode: )\S+/, `$1${cfg.android.versionCode}`, 1]
]});
plan.push({ file: `${FE}/trapeze-ios.yml`, rules: [
  [/(BUNDLE_ID:\n\s+default: )\S+/, `$1${cfg.ios.bundleId}`, 1],
  [/(KAKAO_NATIVE_KEY:\n\s+default: )\S+/, `$1${cfg.sns.kakaoNativeKey}`, 1],
  [/(FACEBOOK_APP_ID:\n\s+default: )\S+/, `$1${cfg.sns.facebookAppId}`, 1],
  [/(FACEBOOK_NATIVE_TOKEN:\n\s+default: )\S+/, `$1${cfg.sns.facebookClientToken}`, 1],
  [/(ADMOB_APP_ID:\n\s+default: )\S+/, `$1${cfg.admob.iosAppId}`, 1],
  [/(\n\s+version: )\S+/, `$1${cfg.ios.version}`, 1],
  [/(\n\s+buildNumber: )\S+/, `$1${cfg.ios.buildNumber}`, 1],
  [/(\n\s+displayName: ).+/, `$1${cfg.displayNameKo || cfg.displayName}`, 1],
  [/(FacebookDisplayName: ).+/, `$1${cfg.displayNameKo || cfg.displayName}`, 1],
  [/(\['applinks:)[^']+(')/, `$1${cfg.deeplinkHost}$2`, 1],
  [/(- fb\$FACEBOOK_APP_ID\n(?:[^\n]*#[^\n]*\n)*\s+- )\S+/, `$1${cfg.customScheme || master.customScheme}`, 1]
]});
plan.push({ file: `${FE}/capacitor.config.ts`, rules: [
  [/(let appId = ')[^']+(')/, `$1${cfg.ios.bundleId}$2`, 1],
  [/(appName: ')[^']+(')/, `$1${cfg.displayName}$2`, 1],
  [/(serverClientId: ")[^"]+(")/, `$1${cfg.googleAuth.serverClientId}$2`, 1],
  [/(iosClientId: ")[^"]+(")/, `$1${cfg.googleAuth.iosClientId}$2`, 1]
]});
for (const env of ['environment.prod.ts', 'environment.ts']) {
  plan.push({ file: `${FE}/src/environments/${env}`, rules: [[/(\n\s*apiBaseURL: ')[^']+(')/, `$1${cfg.apiBaseUrl}$2`, 1]] });
}
// AdMob.ts 는 .gitignore(**/AdMob.ts) 대상이라 새 clone 에는 없다 — 없으면 경고만 하고 건너뛴다(README 한계·하드코딩 조사 참조)
plan.push({ file: `${FE}/src/app/constants/Keys/AdMob.ts`, optional: true, rules: [
  [/(export const BANNER_AD_ID = ')[^']+(')/, `$1${units.bannerAndroid}$2`, 1],
  [/(export const BANNER_AD_ID_IOS = ')[^']+(')/, `$1${units.bannerIos}$2`, 1],
  [/(export const INTERSTITIAL_AD_ID = ')[^']+(')/, `$1${units.interstitialAndroid}$2`, 1],
  [/(export const INTERSTITIAL_AD_ID_IOS = ')[^']+(')/, `$1${units.interstitialIos}$2`, 1]
]});
plan.push({ file: `${FE}/src/app/app.component.ts`, rules: [[/(adId: ')[^']+(')/, `$1${units.interstitialAndroid}$2`, 1]] });
plan.push({ file: `${FE}/src/app/constants/Keys/KakaoAPI.ts`, rules: [
  [/(const APP_KEY = ')[^']+(')/, `$1${cfg.sns.kakaoNativeKey}$2`, 1],
  [/(const WEB_KEY = ')[^']+(')/, `$1${cfg.sns.kakaoJsKey}$2`, 1]
]});
plan.push({ file: `${FE}/src/app/constants/Keys/FacebookAPI.ts`, rules: [[/(const APP_ID = ')[^']+(')/, `$1${cfg.sns.facebookAppId}$2`, 1]] });
plan.push({ file: `${FE}/src/index.html`, rules: [[/(<title>)[^<]+(<\/title>)/, `$1${cfg.displayNameKo || cfg.displayName}$2`, 1]] });
for (const d of nativeAndroidDirs) {
  plan.push({ file: `${d}/app/src/main/res/values/strings.xml`, optional: d.endsWith('/android'), rules: [
    [/(name="app_name">)[^<]+/, `$1${cfg.displayName}`, 1],
    [/(name="title_activity_main">)[^<]+/, `$1${cfg.displayName}`, 1],
    [/(name="package_name">)[^<]+/, `$1${cfg.android.packageName}`, 1],
    [/(name="custom_url_scheme">)[^<]+/, `$1${cfg.android.packageName}`, 1],
    [/(name="server_client_id">)[^<]+/, `$1${cfg.googleAuth.serverClientId}`, 1],
    [/(name="default_web_client_id">)[^<]+/, `$1${cfg.googleAuth.serverClientId}`, 1]
  ]});
  plan.push({ file: `${d}/app/src/main/java/kr/co/sensiblenews/witchHuntingVU2D7F2P7E/MainActivity.java`, optional: d.endsWith('/android'), rules: [
    [/(\n\s*AdLoader adLoader = new AdLoader\.Builder\(this, ")[^"]+(")/, `$1${units.nativeAndroid}$2`, 1]
  ]});
}
for (const d of nativeIosDirs) {
  plan.push({ file: `${d}/App/App/controllers/MainViewController.swift`, optional: d.endsWith('/ios'), rules: [
    [/(AdLoader\(adUnitID: ")[^"]+(")/, `$1${units.nativeIos}$2`, 1]
  ]});
}
// 백엔드·웹: Master 도메인 → 대상 도메인 (하드코딩 조사 결과 파일 전부), AdSense 게시자 ID
const domainFiles = [
  `${BE}/src/main/java/com/sensible/common/Constants.java`,
  `${BE}/src/main/java/com/sensible/admin/controller/SuperAdminController.java`,
  `${BE}/src/main/java/com/sensible/api/controller/AppleController.java`,
  `${BE}/src/main/java/com/sensible/api/controller/DeepLinkController.java`,
  `${BE}/src/main/java/com/sensible/api/service/SuperAppService.java`,
  `${BE}/src/main/java/com/sensible/common/util/FileUtil.java`,
  `${BE}/src/main/java/com/sensible/common/util/ImageModerationUtil.java`,
  `${BE}/src/main/java/com/sensible/common/util/WebCardUtil.java`,
  `${BE}/src/main/webapp/WEB-INF/jsp/common/about.jsp`, `${BE}/src/main/webapp/WEB-INF/jsp/common/contact.jsp`,
  `${BE}/src/main/webapp/WEB-INF/jsp/common/content_landing.jsp`, `${BE}/src/main/webapp/WEB-INF/jsp/common/deeplink_redirect.jsp`,
  `${BE}/src/main/webapp/WEB-INF/jsp/common/faq.jsp`, `${BE}/src/main/webapp/WEB-INF/jsp/common/home.jsp`,
  `${BE}/src/main/webapp/WEB-INF/jsp/common/posts.jsp`, `${BE}/src/main/webapp/WEB-INF/jsp/info/info.jsp`,
  `${BE}/src/main/webapp/WEB-INF/jsp/super/sidebar.jsp`, `${BE}/src/main/webapp/WEB-INF/jsp/super/web_contact.jsp`,
  `${FE}/src/app/app.component.ts`, `${FE}/src/app/modals/login-modal/login-modal.page.ts`, `${FE}/src/app/pages/detail-page/detail-page.page.ts`,
  `${FE}/src/app/pages/feed-detail/feed-detail.page.ts`, `${FE}/src/app/pages/lobby/market-menu-popover.component.ts`,
  `${FE}/src/app/pages/star-page/star-page.page.ts`, `${FE}/src/app/services/deep-link.service.ts`, `${FE}/src/app/services/helper.service.ts`
];
for (const f of domainFiles) plan.push({ file: f, merge: true, rules: [[q(master.domain), cfg.domain, 1]] });
const adsenseFiles = ['about', 'contact', 'content_landing', 'faq', 'home', 'posts'].map(j => `${BE}/src/main/webapp/WEB-INF/jsp/common/${j}.jsp`);
for (const f of adsenseFiles) plan.push({ file: f, merge: true, rules: [[q(master.adsense.publisherId), (cfg.adsense || master.adsense).publisherId, 1]] });
// 딥링크 검증 파일: AASA appID = TEAMID.bundleId, assetlinks package_name (서명 지문은 키스토어 생성 뒤 수동)
plan.push({ file: `${BE}/src/main/webapp/.well-known/apple-app-site-association`, rules: [[/("appID": ")[^"]+(")/, `$1${cfg.ios.teamId}.${cfg.ios.bundleId}$2`, 1]] });
plan.push({ file: `${BE}/src/main/webapp/.well-known/assetlinks.json`, rules: [[/("package_name": ")[^"]+(")/, `$1${cfg.android.packageName}$2`, 1]] });
// 앱 표기명(사용자 노출 문자열) — Master 표기 → 대상 표기
const nameFiles = [`${FE}/src/app/pages/app-inst/app-inst.page.ts`, `${FE}/src/app/pages/faq/faq.page.ts`, `${FE}/src/app/services/helper.service.ts`,
  `${FE}/src/app/components/header/header.component.html`, `${FE}/src/app/pages/lobby/lobby.page.html`, `${FE}/src/app/pages/my-page/my-page.page.html`,
  `${FE}/src/app/pages/star-page/star-page.page.html`, `${FE}/src/app/pages/feed-detail/feed-detail.page.html`, `${FE}/src/app/pages/detail-page/detail-page.page.ts`,
  `${FE}/src/app/pages/star-page/star-page.page.ts`, `${FE}/src/app/app.component.ts`];
for (const f of nameFiles) plan.push({ file: f, merge: true, soft: true, rules: [
  [q(master.displayName), cfg.displayName, 0], [q(master.displayNameKo), cfg.displayNameKo || cfg.displayName, 0]
]});

// ---------- 실행 ----------
// 같은 파일에 규칙이 여러 번 등록되면(merge) 한 번 읽어 순서대로 적용한다
const byFile = new Map();
for (const p of plan) {
  if (!byFile.has(p.file)) byFile.set(p.file, { file: p.file, optional: !!p.optional, rules: [] });
  const e = byFile.get(p.file); e.rules.push(...p.rules.map(r => ({ re: r[0], to: r[1], min: r[2], soft: !!p.soft })));
}
const report = [];
let failed = false;
for (const e of byFile.values()) {
  const abs = path.join(root, e.file);
  if (!fs.existsSync(abs)) { if (e.optional) { report.push([e.file, e.file.endsWith('AdMob.ts') ? '경고: 없음(.gitignore 대상 — 운영본에서 복사해 넣어야 빌드됨)' : '건너뜀(없음)']); continue; } console.error(`대상 파일 없음: ${e.file}`); failed = true; continue; }
  const before = fs.readFileSync(abs, 'utf8');
  let after = before; let total = 0;
  for (const r of e.rules) {
    const re = r.re instanceof RegExp ? r.re : q(r.re);
    const n = (after.match(re.global ? re : new RegExp(re.source, re.flags + 'g')) || []).length;
    if (n < r.min) { console.error(`${e.file}: 주입 지점 ${n}건 (최소 ${r.min}) — 코드가 바뀌었는지 확인: ${re}`); failed = true; }
    after = after.replace(re, r.to); total += n;
  }
  const changed = after !== before;
  report.push([e.file, changed ? `${total}건 치환` : (total ? `${total}건 일치(변경 없음)` : '매치 없음')]);
  const dest = applyDir ? abs : path.join(outDir, e.file);
  if (outDir || changed) { fs.mkdirSync(path.dirname(dest), { recursive: true }); fs.writeFileSync(dest, after); }
}

// globals.properties — 템플릿 + config + 비밀. --apply 라도 체크아웃의 운영 파일은 덮지 않는다
const tpl = fs.readFileSync(path.join(__dirname, 'globals.properties.template'), 'utf8');
const vars = {
  PLATFORM_ID: cfg.platformId === 'master' ? 'master' : `platform-${cfg.platformId}`, GENERATED_AT: new Date().toISOString().slice(0, 10),
  DB_HOST: cfg.backend.dbHost, DB_PORT: cfg.backend.dbPort, DB_NAME: cfg.backend.dbName, DB_USER: cfg.backend.dbUser, DB_PASSWORD: sec('DB_PASSWORD'),
  APPLE_KEY_DIR: cfg.backend.appleKeyDir, APPLE_KEY_ID: sec('APPLE_KEY_ID'), APPLE_TEAM_ID: cfg.ios.teamId, APPLE_SERVICE_ID: sec('APPLE_SERVICE_ID'), API_BASE_URL: cfg.apiBaseUrl,
  REDIS_HOST: cfg.backend.redisHost, REDIS_PORT: cfg.backend.redisPort, REDIS_PASSWORD: sec('REDIS_PASSWORD'), UPLOAD_ROOT: cfg.backend.uploadRoot,
  MEDIA_TOKEN_SECRET: sec('MEDIA_TOKEN_SECRET'), LANDING_TOKEN_SECRET: sec('LANDING_TOKEN_SECRET'),
  SMTP_USERNAME: sec('SMTP_USERNAME'), SMTP_PASSWORD: sec('SMTP_PASSWORD')
};
const globals = tpl.replace(/\$\{([A-Z_]+)\}/g, (m, k) => (k in vars ? String(vars[k]) : m));
const gDest = applyDir ? path.join(applyDir, 'factory', '.generated', `${vars.PLATFORM_ID}.globals.properties`) : path.join(outDir, BE, 'src/main/resources/META-INF/props/globals.properties');
fs.mkdirSync(path.dirname(gDest), { recursive: true }); fs.writeFileSync(gDest, globals, { mode: 0o600 });
report.push([path.relative(applyDir || outDir, gDest), applyDir ? '생성(운영 파일은 덮지 않음 — 수동 배치)' : '템플릿에서 생성']);

console.log(`Build Injector — ${cfg.platformId} (${cfg.displayName})  admob.mode=${cfg.admob.mode}  ${applyDir ? 'APPLY → ' + applyDir : 'OUT → ' + outDir}`);
for (const [f, s] of report) console.log(`  ${s.padEnd(28)} ${f}`);
if (failed) { console.error('\n주입 실패 — 위 오류를 확인한다'); process.exit(1); }
console.log(`\n완료: ${report.length}개 파일`);
