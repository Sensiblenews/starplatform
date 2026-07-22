# WITCH (core-backend)

Spring MVC 기반 백엔드 API 서버입니다.

## 기술 스택

| 항목 | 버전 |
|---|---|
| Java | 1.8 (OpenJDK 8) |
| Spring Framework | 4.1.1.RELEASE |
| 전자정부프레임워크 | 3.5.0 |
| MyBatis | 3.2.2 |
| MySQL Connector | 8.0.11 |
| Tiles | 3.0.3 |
| Tomcat (내장) | 7.x (Maven Plugin) |
| 빌드 도구 | Maven |
| 패키징 | WAR |

## 사전 요구 사항

- **JDK 8**
- **Maven 3.8+** (`mvn --version`으로 확인)

설치되어 있지 않다면:

```bash
# macOS (Apple Silicon / Intel 공통) — Zulu 8은 arm64 네이티브 빌드 제공
brew install --cask zulu@8
brew install maven

# Ubuntu
sudo apt install openjdk-8-jdk maven
```

시스템 기본 JDK를 8로 바꿀 필요는 없습니다. `mvnw.sh`가 이 프로젝트를 빌드할 때만 `JAVA_HOME`을 JDK 8로 지정하므로, 전역 `java -version`은 그대로 두면 됩니다.

## JDK 경로

`mvnw.sh`가 아래 순서로 JDK 8을 자동 탐지하므로 별도 설정이 필요 없습니다.

1. `JAVA8_HOME` 환경변수
2. macOS: `/usr/libexec/java_home -v 1.8`
3. Linux: `/usr/lib/jvm/java-8-*`

비표준 경로에 설치했다면 환경변수로 직접 지정하세요:

```bash
JAVA8_HOME=/path/to/jdk8 ./mvnw.sh build
```

## 빌드 & 실행

`mvnw.sh` 래퍼 스크립트를 사용하면 `JAVA_HOME` 설정 없이 간편하게 실행할 수 있습니다:

```bash
./mvnw.sh <command>
```

### 명령어 요약

| 작업 | 명령어 |
|---|---|
| 컴파일 | `./mvnw.sh compile` |
| WAR 빌드 (테스트 스킵) | `./mvnw.sh build` |
| WAR 빌드 (테스트 포함) | `./mvnw.sh build:test` |
| 로컬 Tomcat 실행 | `./mvnw.sh run` |
| Clean | `./mvnw.sh clean` |
| 테스트 | `./mvnw.sh test` |
| 의존성 다운로드 | `./mvnw.sh deps` |
| 의존성 트리 | `./mvnw.sh deps:tree` |
| 도움말 | `./mvnw.sh help` |

등록되지 않은 명령어는 그대로 Maven에 전달됩니다:

```bash
./mvnw.sh clean install -Pprod
```

> **참고**: 로컬 실행(`run`) 시 포트 80을 사용합니다. 관리자 권한이 필요할 수 있으며, `pom.xml`의 `<port>80</port>`를 `8080` 등으로 변경할 수 있습니다.

### WAR 빌드 결과물

빌드 후 WAR 파일은 아래 경로에 생성됩니다:

```
target/witch.war
```

<details>
<summary>mvnw.sh 없이 직접 Maven 실행하기</summary>

모든 Maven 명령어 앞에 `JAVA_HOME`을 지정해야 합니다:

```bash
# macOS
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn clean package -DskipTests

# Ubuntu
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn clean package -DskipTests
```

또는 환경변수를 미리 설정하세요:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)   # macOS
mvn clean package -DskipTests
```

</details>

## 프로젝트 구조

```
core-backend/
├── mvnw.sh                          # Maven 래퍼 스크립트
├── pom.xml                          # Maven 빌드 설정
├── lib/                             # file 기반 Maven 저장소 (project-local-repo)
│   └── jbit/core/jbit-core/1.0/     # jbit-core 커스텀 라이브러리
├── src/
│   ├── main/
│   │   ├── java/com/sensible/       # Java 소스 코드
│   │   ├── resources/
│   │   │   ├── META-INF/spring/     # Spring XML 설정 파일
│   │   │   ├── META-INF/sqlmap/     # MyBatis SQL 매퍼
│   │   │   ├── META-INF/props/      # 프로퍼티 파일
│   │   │   └── META-INF/tiles/      # Tiles 레이아웃 설정
│   │   └── webapp/
│   │       └── WEB-INF/
│   │           ├── web.xml          # 서블릿 배포 디스크립터
│   │           ├── config/spring/   # 서블릿 컨텍스트 설정
│   │           └── jsp/             # JSP 뷰 파일
│   └── test/                        # 테스트 코드
└── target/                          # 빌드 산출물 (gitignore)
```

### 로컬 JAR 추가하기

`lib/`은 `pom.xml`의 `project-local-repo`로 등록된 file 기반 Maven 저장소입니다. **Maven 저장소 레이아웃을 그대로 지켜야** 해석됩니다:

```
lib/<groupId를 /로 분해>/<artifactId>/<version>/<artifactId>-<version>.jar
```

예) `jbit.core:jbit-core:1.0` → `lib/jbit/core/jbit-core/1.0/jbit-core-1.0.jar`

artifactId 디렉터리를 빠뜨리면 `was not found in file:///.../lib` 오류가 납니다. 같은 위치에 최소 `.pom` 파일도 함께 두면 "POM is missing" 경고가 사라집니다.

> 해석 실패는 `~/.m2`에 캐시되므로, 경로를 고친 뒤에는 `-U` 옵션으로 재시도하세요: `./mvnw.sh compile -U`