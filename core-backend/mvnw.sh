#!/usr/bin/env bash
#
# mvnw.sh — JAVA_HOME을 자동 설정하는 Maven 래퍼 스크립트
#
# 사용법:
#   ./mvnw.sh <command>
#
# 예시:
#   ./mvnw.sh compile          # 컴파일
#   ./mvnw.sh build            # WAR 빌드 (테스트 스킵)
#   ./mvnw.sh build:test       # WAR 빌드 (테스트 포함)
#   ./mvnw.sh run              # 로컬 Tomcat 실행
#   ./mvnw.sh clean            # 빌드 산출물 삭제
#   ./mvnw.sh test             # 테스트 실행
#   ./mvnw.sh deps             # 의존성 다운로드
#   ./mvnw.sh deps:tree        # 의존성 트리 출력
#   ./mvnw.sh <maven args>     # 임의의 Maven 명령어 실행
#

set -e

# ── JDK 8 경로 탐지 (JAVA8_HOME > macOS java_home > JVM 디렉터리 스캔) ──
# 주의: macOS의 `java_home -v 1.8`은 JDK 8이 없으면 상위 버전을 반환하므로
#       반드시 javac 실제 버전으로 재검증해야 한다.
is_jdk8() {
  [ -n "$1" ] && [ -x "$1/bin/javac" ] && \
    "$1/bin/javac" -version 2>&1 | grep -q '^javac 1\.8\.'
}

JAVA_HOME=""
for candidate in \
  "${JAVA8_HOME:-}" \
  "$([ -x /usr/libexec/java_home ] && /usr/libexec/java_home -v 1.8 2>/dev/null || true)" \
  /Library/Java/JavaVirtualMachines/*8*/Contents/Home \
  /usr/lib/jvm/java-8-*
do
  if is_jdk8 "$candidate"; then
    JAVA_HOME="$candidate"
    break
  fi
done

if [ -z "$JAVA_HOME" ]; then
  echo "❌ JDK 8을 찾을 수 없습니다."
  echo "   macOS : brew install --cask zulu@8"
  echo "   Ubuntu: sudo apt install openjdk-8-jdk"
  echo "   또는 직접 지정: JAVA8_HOME=/path/to/jdk8 ./mvnw.sh $*"
  exit 1
fi

if ! command -v mvn >/dev/null; then
  echo "❌ Maven을 찾을 수 없습니다."
  echo "   macOS : brew install maven"
  echo "   Ubuntu: sudo apt install maven"
  exit 1
fi

export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

# ── 프로젝트 루트로 이동 ──
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ── 명령어 매핑 ──
COMMAND="${1:-help}"

case "$COMMAND" in
  compile)
    echo "🔨 컴파일 중..."
    mvn compile
    ;;
  build)
    echo "📦 WAR 빌드 중 (테스트 스킵)..."
    mvn clean package -DskipTests
    ;;
  build:test)
    echo "📦 WAR 빌드 중 (테스트 포함)..."
    mvn clean package
    ;;
  run)
    echo "🚀 Tomcat 서버 시작 중..."
    mvn tomcat7:run
    ;;
  clean)
    echo "🧹 빌드 산출물 삭제 중..."
    mvn clean
    ;;
  test)
    echo "🧪 테스트 실행 중..."
    mvn test
    ;;
  deps)
    echo "📥 의존성 다운로드 중..."
    mvn dependency:resolve
    ;;
  deps:tree)
    echo "🌳 의존성 트리 출력 중..."
    mvn dependency:tree
    ;;
  help|--help|-h)
    echo ""
    echo "사용법: ./mvnw.sh <command>"
    echo ""
    echo "Commands:"
    echo "  compile      소스 코드 컴파일"
    echo "  build        WAR 빌드 (테스트 스킵)"
    echo "  build:test   WAR 빌드 (테스트 포함)"
    echo "  run          로컬 Tomcat 서버 실행"
    echo "  clean        빌드 산출물 삭제"
    echo "  test         테스트 실행"
    echo "  deps         의존성 다운로드"
    echo "  deps:tree    의존성 트리 출력"
    echo ""
    echo "또는 임의의 Maven 인자를 직접 전달할 수 있습니다:"
    echo "  ./mvnw.sh clean install -Pprod"
    echo ""
    ;;
  *)
    # 등록되지 않은 명령어는 그대로 Maven에 전달
    echo "⚙️  mvn $* 실행 중..."
    mvn "$@"
    ;;
esac
