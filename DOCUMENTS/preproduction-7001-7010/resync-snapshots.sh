#!/bin/zsh
# platform-7001~7010 사본을 master-baseline-v1.0(cb7c05e) 으로 갱신한다. 각 사본의 README 커밋은 유지하고
# 그 위에 새 기준을 머지한다(사본 main = baseline + README 였으므로 fast-forward 가 안 되는 구조).
# 실행 전제: gh 활성 계정이 Sensiblenews (gh auth switch --user Sensiblenews), 사용자가 터미널에서 직접 실행.
set -e
R=/Users/hagangmin/Documents/works/kmong/starplatform
W=$(mktemp -d)/snapshot-wt
git -C $R worktree add --detach "$W" master-baseline-v1.0 > /dev/null
cd "$W"
for n in $(seq 7001 7010); do
  URL="https://github.com/Sensiblenews/platform-$n.git"
  git fetch -q "$URL" main
  git checkout -q --detach FETCH_HEAD
  git merge -q --no-edit -m "chore: platform-$n 기준을 master-baseline-v1.0 (cb7c05e) 로 갱신 — PR #31·#32 반영

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>" master-baseline-v1.0
  git push -q "$URL" HEAD:refs/heads/main master-baseline-v1.0
  echo "platform-$n main=$(git rev-parse --short HEAD) tag=master-baseline-v1.0"
done
cd /; git -C $R worktree remove --force "$W"
gh auth switch --user gkrkdals
echo done
