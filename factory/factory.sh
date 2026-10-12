#!/bin/zsh
# Config Factory 실행 래퍼. 저장소 어디서든 실행 가능.
#   factory/factory.sh validate [--strict]            # platforms/*.json 전부 검증
#   factory/factory.sh selftest                       # 검증기 정상·오류 픽스처 + Injector 자가 검증(master → 변경 0)
#   factory/factory.sh inject <id> <out-dir> [.env]   # 예: inject 7001 /tmp/out-7001 ~/secrets/platform-7001/.env
#   factory/factory.sh build-web                      # ionic/ng 웹 번들 (www/)
#   factory/factory.sh build-war                      # 백엔드 WAR (target/witch.war)
#   factory/factory.sh package <id> <out-dir> [.env]  # inject 된 사본으로 web+war 빌드 (worktree 사용, 원 체크아웃 불변)
#
# 금지: npm run build-android / build-ios / clean-build-* (android/·ios/ 를 지움). 앱 빌드는 ionic capacitor build 만.
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/.." && pwd)
cmd=${1:-help}; shift || true

case "$cmd" in
  validate)
    node "$HERE/validate.js" "$@" "$HERE"/platforms/*.json ;;
  selftest)
    echo "== 1. 정상 설정 (PASS 기대)"; node "$HERE/validate.js" "$HERE"/platforms/*.json > /dev/null && echo PASS
    echo "== 2. 오류 픽스처 5종 (FAIL 기대)"; if node "$HERE/validate.js" "$HERE"/platforms/platform-7001.json "$HERE"/test-fixtures/invalid-*.json > /dev/null; then echo "FAIL: 오류 설정이 통과함"; exit 1; else echo PASS; fi
    echo "== 3. strict 완전 설정 (PASS 기대)"; node "$HERE/validate.js" --strict "$HERE/test-fixtures/valid-strict.json" > /dev/null && echo PASS
    echo "== 4. Injector 자가 검증 (master → 변경 0건)"
    T=$(mktemp -d); node "$HERE/inject.js" --config "$HERE/platforms/master.json" --out "$T" --root "$ROOT" --allow-missing-secrets > /dev/null
    n=0; while IFS= read -r f; do case "$f" in *globals.properties) continue;; esac; cmp -s "$T/$f" "$ROOT/$f" || { echo "DIFF $f"; n=$((n+1)); }; done < <(cd "$T" && find . -type f | sed 's#^\./##')
    rm -rf "$T"; [ "$n" -eq 0 ] && echo PASS || { echo "FAIL: $n개 파일 차이"; exit 1; } ;;
  inject)
    id=$1; out=$2; env=$3; [ -n "$id" ] && [ -n "$out" ] || { echo "usage: factory.sh inject <id|master> <out-dir> [.env]"; exit 2; }
    cfg="$HERE/platforms/platform-$id.json"; [ "$id" = master ] && cfg="$HERE/platforms/master.json"
    node "$HERE/validate.js" "$cfg" > /dev/null
    if [ -n "$env" ]; then node "$HERE/inject.js" --config "$cfg" --out "$out" --root "$ROOT" --secrets "$env"; else node "$HERE/inject.js" --config "$cfg" --out "$out" --root "$ROOT" --allow-missing-secrets; fi ;;
  build-web)
    cd "$ROOT/core-frontend" && npx ng build ;;
  build-war)
    cd "$ROOT/core-backend" && ./mvnw.sh build ;;
  package)
    id=$1; out=$2; env=$3; [ -n "$id" ] && [ -n "$out" ] || { echo "usage: factory.sh package <id> <out-dir> [.env]"; exit 2; }
    cfg="$HERE/platforms/platform-$id.json"
    node "$HERE/validate.js" "$cfg" > /dev/null
    WT="$out/worktree"; rm -rf "$WT"; mkdir -p "$out"
    git -C "$ROOT" worktree add --detach "$WT" HEAD > /dev/null
    ln -s "$ROOT/core-frontend/node_modules" "$WT/core-frontend/node_modules"
    if [ -n "$env" ]; then node "$HERE/inject.js" --config "$cfg" --apply "$WT" --secrets "$env"; else node "$HERE/inject.js" --config "$cfg" --apply "$WT" --allow-missing-secrets; fi
    (cd "$WT/core-frontend" && npx ng build) 2>&1 | tee "$out/build-web.log"
    (cd "$WT/core-backend" && ./mvnw.sh build) 2>&1 | tee "$out/build-war.log"
    cp -R "$WT/core-frontend/www" "$out/www"; cp "$WT/core-backend/target/witch.war" "$out/platform-$id.war"
    cp "$WT/factory/.generated/platform-$id.globals.properties" "$out/" 2>/dev/null || true
    git -C "$ROOT" worktree remove --force "$WT"
    (cd "$out" && shasum -a 256 platform-$id.war > SHA256SUMS && find www -type f -exec shasum -a 256 {} + >> SHA256SUMS)
    echo "산출물: $out (web=www/, war=platform-$id.war, SHA256SUMS). Android/iOS 는 inject 된 체크아웃에서 ionic capacitor build 로 별도 진행" ;;
  *)
    sed -n '2,12p' "$0" ;;
esac
