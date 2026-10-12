#!/usr/bin/env node
/**
 * platform.config.json 검증기 (의존성 없음, Node 18+).
 *
 *   node factory/validate.js factory/platforms/*.json          # 경고 허용(TODO_ 자리표시자)
 *   node factory/validate.js --strict factory/platforms/*.json # TODO_ 도 오류
 *
 * 검사 내용
 *   1. platform.config.schema.json 의 required / type / pattern / enum / const / min·max / additionalProperties
 *   2. 파일 간 중복: platformId, domain, apiBaseUrl, android.packageName, ios.bundleId, backend.dbName, backend.dbUser
 *   3. 교차 일관성: apiBaseUrl 호스트 == domain, deeplinkHost == domain, admob.mode=production 이면 TODO_ 단위 금지,
 *      파일명 platform-<id>.json 과 platformId 일치
 *   4. TODO_ 자리표시자: 기본 경고, --strict 오류
 * 종료코드 0 = PASS, 1 = FAIL(오류 1건 이상), 2 = 사용법 오류
 */
'use strict';
const fs = require('fs');
const path = require('path');

const args = process.argv.slice(2);
const strict = args.includes('--strict');
const files = args.filter(a => !a.startsWith('--'));
if (files.length === 0) {
  console.error('usage: validate.js [--strict] <config.json> [...]');
  process.exit(2);
}

const schema = JSON.parse(fs.readFileSync(path.join(__dirname, 'platform.config.schema.json'), 'utf8'));

function resolveRef(ref) {
  // "#/definitions/adUnit" 형태만 지원
  const parts = ref.replace(/^#\//, '').split('/');
  return parts.reduce((o, k) => o[k], schema);
}

function checkNode(node, value, where, out) {
  if (node.$ref) node = Object.assign({}, resolveRef(node.$ref), node, { $ref: undefined });
  if (node.const !== undefined && value !== node.const) { out.errors.push(`${where}: 값이 ${JSON.stringify(node.const)} 이어야 함`); return; }
  if (node.type) {
    const t = Array.isArray(value) ? 'array' : (Number.isInteger(value) && node.type === 'integer') ? 'integer' : typeof value;
    const ok = node.type === 'integer' ? Number.isInteger(value) : (node.type === 'object' ? (value !== null && typeof value === 'object' && !Array.isArray(value)) : t === node.type);
    if (!ok) { out.errors.push(`${where}: 자료형 ${node.type} 필요 (받은 값: ${JSON.stringify(value)})`); return; }
  }
  if (node.enum && !node.enum.includes(value)) out.errors.push(`${where}: 허용값 ${node.enum.join('|')} 중 하나여야 함 (받은 값: ${JSON.stringify(value)})`);
  if (typeof value === 'string') {
    if (node.minLength !== undefined && value.length < node.minLength) out.errors.push(`${where}: 최소 길이 ${node.minLength}`);
    if (node.maxLength !== undefined && value.length > node.maxLength) out.errors.push(`${where}: 최대 길이 ${node.maxLength}`);
    if (node.pattern && !new RegExp(node.pattern).test(value)) out.errors.push(`${where}: 형식 불일치 /${node.pattern}/ (받은 값: ${JSON.stringify(value)})`);
    if (/^TODO_/.test(value)) out.todos.push(`${where} = ${value}`);
  }
  if (typeof value === 'number') {
    if (node.minimum !== undefined && value < node.minimum) out.errors.push(`${where}: 최소 ${node.minimum}`);
    if (node.maximum !== undefined && value > node.maximum) out.errors.push(`${where}: 최대 ${node.maximum}`);
  }
  if (Array.isArray(value)) {
    if (node.minItems !== undefined && value.length < node.minItems) out.errors.push(`${where}: 최소 ${node.minItems}개`);
    if (node.uniqueItems && new Set(value).size !== value.length) out.errors.push(`${where}: 중복 항목`);
    if (node.items) value.forEach((v, i) => checkNode(node.items, v, `${where}[${i}]`, out));
  }
  if (node.type === 'object' && value && typeof value === 'object') {
    for (const k of node.required || []) if (!(k in value)) out.errors.push(`${where}.${k}: 필수 항목 누락`);
    for (const k of Object.keys(value)) {
      if (node.properties && node.properties[k]) checkNode(node.properties[k], value[k], `${where}.${k}`, out);
      else if (node.additionalProperties === false) out.errors.push(`${where}.${k}: 스키마에 없는 항목`);
    }
  }
}

function hostOf(url) { try { return new URL(url).host; } catch (e) { return null; } }

const results = [];
const seen = {};
function dup(key, val, file) {
  if (val === undefined || /^TODO_/.test(String(val))) return null;
  seen[key] = seen[key] || {};
  if (seen[key][val]) return `${key} 중복: ${JSON.stringify(val)} (이미 ${seen[key][val]} 에 있음)`;
  seen[key][val] = file;
  return null;
}

for (const file of files) {
  const out = { file, errors: [], warnings: [], todos: [] };
  results.push(out);
  let cfg;
  try { cfg = JSON.parse(fs.readFileSync(file, 'utf8')); }
  catch (e) { out.errors.push(`JSON 파싱 실패: ${e.message}`); continue; }

  checkNode(schema, cfg, '$', out);
  if (out.errors.length) continue;

  // 파일명 ↔ platformId
  // 파일명 ↔ platformId (factory/platforms/ 안의 파일만. 픽스처·임시 파일은 제외)
  const base = path.basename(file, '.json');
  const expected = cfg.platformId === 'master' ? 'master' : `platform-${cfg.platformId}`;
  if (path.basename(path.dirname(path.resolve(file))) === 'platforms' && base !== expected) {
    out.errors.push(`파일명 ${base}.json 과 platformId ${cfg.platformId} 불일치 (기대: ${expected}.json)`);
  }
  if (!/^TODO_/.test(cfg.apiBaseUrl) && /^TODO_/.test(cfg.domain)) out.warnings.push('apiBaseUrl 은 채웠는데 domain 이 TODO_ 다');

  // 교차 일관성
  if (!/^TODO_/.test(cfg.domain)) {
    if (hostOf(cfg.apiBaseUrl) !== cfg.domain) out.errors.push(`apiBaseUrl 호스트(${hostOf(cfg.apiBaseUrl)}) 가 domain(${cfg.domain}) 과 다름`);
    if (cfg.deeplinkHost !== cfg.domain) out.warnings.push(`deeplinkHost(${cfg.deeplinkHost}) 가 domain 과 다름 — 의도한 경우만 허용`);
  }
  if (cfg.admob.mode === 'production') {
    const todoUnits = Object.entries(cfg.admob.units).filter(([, v]) => /^TODO_/.test(v)).map(([k]) => k);
    if (todoUnits.length) out.errors.push(`admob.mode=production 인데 단위 ID 미입력: ${todoUnits.join(', ')}`);
    if (/^TODO_/.test(cfg.admob.androidAppId) || /^TODO_/.test(cfg.admob.iosAppId)) out.errors.push('admob.mode=production 인데 App ID 미입력');
  }
  if (cfg.android.versionName !== cfg.ios.version) out.warnings.push(`android.versionName(${cfg.android.versionName}) 과 ios.version(${cfg.ios.version}) 이 다름`);

  // 파일 간 중복
  for (const [k, v] of [
    ['platformId', cfg.platformId], ['domain', cfg.domain], ['apiBaseUrl', cfg.apiBaseUrl],
    ['android.packageName', cfg.android.packageName], ['ios.bundleId', cfg.ios.bundleId],
    ['backend.dbName', cfg.backend.dbName], ['backend.dbUser', cfg.backend.dbUser],
    ['admob.androidAppId', cfg.admob.androidAppId], ['admob.iosAppId', cfg.admob.iosAppId]
  ]) { const d = dup(k, v, file); if (d) out.errors.push(d); }

  if (strict && out.todos.length) out.errors.push(`--strict: TODO_ 자리표시자 ${out.todos.length}건 남음`);
}

let failed = 0;
for (const r of results) {
  const status = r.errors.length ? 'FAIL' : (r.todos.length || r.warnings.length ? 'PASS (경고)' : 'PASS');
  if (r.errors.length) failed++;
  console.log(`${status.padEnd(12)} ${r.file}`);
  r.errors.forEach(e => console.log(`    오류  ${e}`));
  r.warnings.forEach(w => console.log(`    경고  ${w}`));
  if (r.todos.length) console.log(`    TODO  자리표시자 ${r.todos.length}건: ${r.todos.map(t => t.split(' = ')[0].replace(/^\$\./, '')).join(', ')}`);
}
console.log(`\n합계: ${results.length}개 중 PASS ${results.length - failed}, FAIL ${failed}${strict ? ' (strict)' : ''}`);
process.exit(failed ? 1 : 0);
